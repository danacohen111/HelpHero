package com.example.helphero.repositories

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.helphero.databases.posts.PostDao
import com.example.helphero.models.FirestorePost
import com.example.helphero.models.Post
import com.example.helphero.models.toFirestorePost
import com.example.helphero.models.toRoomPost
import com.example.helphero.utils.ImageUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostRepository(
    private val firestoreDb: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val postDao: PostDao,
    private val contentResolver: ContentResolver
) {
    private val TAG = "PostRepository"
    private val COLLECTION = "posts"
    private val storageRef: StorageReference = Firebase.storage.reference.child("posts")

    private var postsListenerRegistration: ListenerRegistration? = null

    private val _postsLiveData = MutableLiveData<List<Post>>()
    val postsLiveData: LiveData<List<Post>> get() = _postsLiveData

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _postSuccessful = MutableLiveData<Boolean>()
    val postSuccessful: LiveData<Boolean> get() = _postSuccessful

    @WorkerThread
    fun get(id: String): Post? {
        Log.d(TAG, "Fetching post with id: $id")
        return postDao.get(id)
    }

    @WorkerThread
    fun getUserPosts(userId: String): List<Post> {
        Log.d(TAG, "Fetching posts for user with id: $userId")
        return postDao.getUserPosts(userId)
    }

    @WorkerThread
    suspend fun insert(post: Post) {
        Log.d(TAG, "Inserting post into Room database")
        postDao.insert(post)
    }

    @WorkerThread
    suspend fun delete(post: Post) {
        Log.d(TAG, "Deleting post from Room database")
        postDao.delete(post)
    }

    init {
        listenForPostUpdates()
    }

    private fun listenForPostUpdates() {
        Log.d(TAG, "Starting to listen for post updates")
        postsListenerRegistration?.remove()
        postsListenerRegistration = firestoreDb.collection(COLLECTION)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for post updates", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.documentChanges.isEmpty()) {
                    Log.d(TAG, "No changes in Firestore snapshot")
                    return@addSnapshotListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val updatedPosts = mutableListOf<Post>()
                    val removedPosts = mutableListOf<Post>()

                    snapshot.documentChanges.forEach { change ->
                        try {
                            val firestorePost = change.document.toObject(FirestorePost::class.java)
                            val post = firestorePost.toRoomPost(change.document.id)

                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        insert(post) // Only insert if not present
                                        updatedPosts.add(post)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    delete(post)
                                    removedPosts.add(post)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing document change: ${change.document.id}", e)
                        }
                    }

                    if (updatedPosts.isNotEmpty() || removedPosts.isNotEmpty()) {
                        _postsLiveData.postValue(postDao.getAll()) // Refresh Room data
                        Log.d(
                            TAG,
                            "Processed Firestore changes: ${updatedPosts.size} added/modified, ${removedPosts.size} removed"
                        )
                    }
                }
            }
    }

    fun insertPost(post: Post, imageUri: Uri) {
        _loading.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Uploading image for post with id: ${post.postId}")
                val imageUrl = ImageUtil.uploadImage(post.postId, imageUri, storageRef, contentResolver).toString()
                post.imageUrl = imageUrl

                val fsPost = post.toFirestorePost()
                firestoreDb.collection(COLLECTION).document(post.postId).set(fsPost)
                    .addOnSuccessListener {
                        Log.d(TAG, "Post successfully inserted with id: ${post.postId}")
                        _postSuccessful.postValue(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error inserting post with id: ${post.postId}", e)
                        _postSuccessful.postValue(false)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during post insertion", e)
                _postSuccessful.postValue(false)
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun deletePost(id: String) {
        Log.d(TAG, "Deleting post with id: $id")
        ImageUtil.deleteStorageImage(id, storageRef)
            .addOnSuccessListener {
                firestoreDb.collection(COLLECTION).document(id).delete()
                    .addOnFailureListener {
                        Log.e(TAG, "Failed to delete post document with id: $id", it)
                    }
                Log.d(TAG, "Post deleted successfully with id: $id")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to delete image from storage for post with id: $id", it)
            }
    }
}
