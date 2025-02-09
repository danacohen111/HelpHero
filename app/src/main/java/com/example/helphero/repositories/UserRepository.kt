package com.example.helphero.repositories

import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.example.helphero.databases.users.UserDao
import com.example.helphero.models.FirestoreUser
import com.example.helphero.models.User
import com.example.helphero.models.toFirestoreUser
import com.example.helphero.models.toRoomUser
import com.example.helphero.utils.ImageUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserRepository(
    private val firestoreDb: FirebaseFirestore,
    private val firestoreAuth: FirebaseAuth,
    private val userDao: UserDao
) {
    val TAG = "userRepository"
    val loginSuccessfull = MutableLiveData<Boolean>()

    @WorkerThread
    fun get(id: String, onSuccess: (User) -> Unit, onError: (String) -> Unit) {
        Log.d(TAG, "Fetching user with id: $id")

        // Try fetching user from Room database first
        CoroutineScope(Dispatchers.IO).launch {
            val localUser = userDao.get(id)
            if (localUser != null) {
                Log.d(TAG, "User found in Room database")
                onSuccess(localUser)
                return@launch
            }

            // If not found in Room, fetch from Firestore
            firestoreDb.collection("users").document(id).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firestoreUser = document.toObject(FirestoreUser::class.java)
                        firestoreUser?.let {
                            val roomUser = it.toRoomUser(id)
                            saveUserLocally(roomUser) // Cache in Room
                            Log.d(TAG, "User fetched from Firestore and cached locally")
                            onSuccess(roomUser)
                        } ?: onError("Error parsing Firestore user data")
                    } else {
                        Log.w(TAG, "User not found in Firestore")
                        onError("User not found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user from Firestore: ${e.message}")
                    onError("Error fetching user: ${e.message}")
                }
        }
    }

    /**
     * Login using Firebase Authentication and fetch user details from Firestore.
     */
    fun login(email: String, password: String, onError: (String) -> Unit) {
        firestoreAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser: FirebaseUser? = firestoreAuth.currentUser
                    firebaseUser?.let { user ->
                        fetchUserFromFirestore(user.uid, onError)
                    } ?: onError("User not found.")
                } else {
                    val errorMessage = task.exception?.message ?: "Sign-in failed"
                    onError(errorMessage)
                }
            }
    }

    /**
     * Fetch the user details from Firestore and save to Room database.
     */
    private fun fetchUserFromFirestore(userId: String, onError: (String) -> Unit) {
        firestoreDb.collection("users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    try {
                        // Safely parse Firestore data into FirestoreUser
                        val firestoreUser = documentSnapshot.toObject(FirestoreUser::class.java)
                        if (firestoreUser != null) {
                            val roomUser = firestoreUser.toRoomUser(userId)
                            saveUserLocally(roomUser)
                            loginSuccessfull.postValue(true)
                        } else {
                            onError("User data is empty or malformed in Firestore.")
                        }
                    } catch (e: Exception) {
                        onError("Error parsing user data: ${e.message}")
                    }
                } else {
                    onError("User data not found in Firestore.")
                }
            }
            .addOnFailureListener { exception ->
                onError("Error fetching user: ${exception.message}")
            }
    }


    /**
     * Save user details to Room database.
     */
    private fun saveUserLocally(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                userDao.update(user)
            } catch (e: Exception) {
                Log.e("UserRepository", "Error saving user to local database: ${e.message}")
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        name: String,
        phone: String,
        profileImageUri: Uri,  // Removed nullability from profile image URI
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "Starting sign up process for email: $email")
        firestoreAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = firestoreAuth.currentUser?.uid ?: return@addOnCompleteListener
                    Log.d(TAG, "User created with userId: $userId")
                    val user = User(
                        userId = userId,
                        name = name,
                        phone = phone,
                        photoUrl = "",  // Initially, set photoUrl to an empty string
                        email = email,
                        password = password
                    )

                    val imageId = "profile_${userId}"
                    Log.d(TAG, "Uploading profile image with imageId: $imageId")

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val imageUrl = ImageUtil.uploadImage(imageId, profileImageUri)
                            if (imageUrl != null) {
                                user.photoUrl = imageUrl.toString()
                                Log.d(TAG, "Profile image uploaded successfully: $imageUrl")

                                val firestoreUser = user.toFirestoreUser()

                                firestoreDb.collection("users").document(userId)
                                    .set(firestoreUser)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "User data saved to Firestore")
                                        saveUserLocally(user)  // Save user locally in Room
                                        onSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(
                                            TAG,
                                            "Error saving user data to Firestore: ${e.message}"
                                        )
                                        onError(e.message ?: "Unknown error")
                                    }
                            } else {
                                Log.e(TAG, "Failed to upload image")
                                onError("Failed to upload image")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error uploading image: ${e.message}")
                            onError("Error uploading image: ${e.message}")
                        }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Log.e(TAG, "Error creating user: $errorMessage")
                    onError(errorMessage)
                }
            }
    }

    fun updateUser(
        user: User,
        profileImageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = user.userId
        val firestoreUser = user.toFirestoreUser()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (profileImageUri != null) {
                    val imageId = "profile_$userId"
                    Log.d(TAG, "Uploading profile image with imageId: $imageId")
                     user.photoUrl.takeIf { it.isNotEmpty() }?.let { oldImageUrl ->
                            ImageUtil.deleteImage(oldImageUrl)
                        }

                    val imageUrl = ImageUtil.uploadImage(imageId, profileImageUri)

                    if (imageUrl != null) {
                        user.photoUrl = imageUrl.toString()
                        Log.d(TAG, "Profile image uploaded successfully: $imageUrl")
                    } else {
                        Log.e(TAG, "Failed to upload image")
                        onError("Failed to upload image")
                        return@launch
                    }
                }

                // Update Firestore
                firestoreDb.collection("users").document(userId)
                    .set(firestoreUser)
                    .addOnSuccessListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                userDao.update(user) // Update Room DB
                                withContext(Dispatchers.Main) { onSuccess() }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { onError("Error updating local DB: ${e.message}") }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        onError("Firestore update failed: ${exception.message}")
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Error updating user: ${e.message}")
                onError("Error updating user: ${e.message}")
            }
        }
    }
}