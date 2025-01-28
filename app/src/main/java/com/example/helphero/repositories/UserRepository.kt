package com.example.helphero.repositories

import android.content.ContentResolver
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.example.helphero.databases.users.UserDao
import com.example.helphero.models.FirestoreUser
import com.example.helphero.models.User
import com.example.helphero.models.toRoomUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserRepository(
    private val firestoreDb: FirebaseFirestore,
    private val firestoreAuth: FirebaseAuth,
    private val contentResolver: ContentResolver,
    private val userDao: UserDao
) {
    val TAG = "userRepository"
    val loginSuccessfull = MutableLiveData<Boolean>()

    @WorkerThread
    suspend fun get(id: String): User {
        Log.d(TAG, "Fetching user with id: $id")
        return userDao.get(id)
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
                    val firestoreUser = documentSnapshot.toObject(FirestoreUser::class.java)
                    firestoreUser?.let {
                        val roomUser = it.toRoomUser(userId)
                        saveUserLocally(roomUser)
                        loginSuccessfull.postValue(true)
                    } ?: onError("Error parsing user data.")
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
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestoreAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = firestoreAuth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone
                    )
                    firestoreDb.collection("users").document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onError(e.message ?: "Unknown error")
                        }
                } else {
                    onError(task.exception?.message ?: "Unknown error")
                }
            }
    }

}
