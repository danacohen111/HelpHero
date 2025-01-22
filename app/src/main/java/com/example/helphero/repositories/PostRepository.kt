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
    fun get(id: Int): Post {
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
        Log.d(TAG, "Inserting post")
        postDao.insert(post)
    }

    @WorkerThread
    suspend fun delete(post: Post) {
        Log.d(TAG, "Deleting post")
        postDao.delete(post)
    }

    init {
        listenForPostUpdates()
    }

    private fun listenForPostUpdates() {
        Log.d(TAG, "Listening for post updates")
        postsListenerRegistration =
            firestoreDb.collection(COLLECTION).orderBy("createdString", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening for post updates", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        Log.d(TAG, "Snapshot received: $snapshot")
                        snapshot.documentChanges.forEach { change ->
                            Log.d(TAG, "Document Change Type: ${change.type}, Document ID: ${change.document.id}, Data: ${change.document.data}")
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            val posts = mutableListOf<Post>()
                            val deletedPosts = mutableListOf<Post>()

                            snapshot.documentChanges.forEach { change ->
                                val firestorePost = change.document.toObject(FirestorePost::class.java)
                                firestorePost?.let { fsPost ->
                                    val post = fsPost.toRoomPost(change.document.id)
                                    when (change.type) {
                                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                            insert(post)
                                            posts.add(post)
                                        }
                                        DocumentChange.Type.REMOVED -> {
                                            delete(post)
                                            deletedPosts.add(post)
                                        }
                                    }
                                }
                            }
                            _postsLiveData.postValue(posts)
                            Log.d(TAG, "Post updates processed: ${posts.size} posts, ${deletedPosts.size} deleted posts")
                        }
                    } else {
                        Log.d(TAG, "Snapshot is null")
                    }
                }

        _postsLiveData.observeForever { posts ->
            Log.d(TAG, "Current posts in LiveData: ${posts.size}")
            posts.forEach { post ->
                Log.d(TAG, "Post ID: ${post.postId}, Title: ${post.title}, Description: ${post.desc}")
            }
        }
    }

    fun insertPost(post: Post, imageUri: Uri) {
        _loading.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Inserting post with id: ${post.postId}")
                if (post.imageUrl.isEmpty()) {
                    //TO DO: upload the image once the storage is initiallized
                    //val imageUrl = ImageUtil.uploadImage(post.postId, imageUri, storageRef, contentResolver).toString()
                    post.imageUrl = "hi"
                    Log.d(TAG, "Image uploaded for post with id: ${post.postId}")
                }
                val fsPost = post.toFirestorePost()
                firestoreDb.collection(COLLECTION).document(post.postId).set(fsPost)
                    .addOnSuccessListener {
                        Log.d(TAG, "Post inserted with id: ${post.postId}")
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
                Log.d(TAG, "Post insertion completed for id: ${post.postId}")
            }
        }
    }

    fun deletePost(id: String) {
        Log.d(TAG, "Deleting post with id: $id")
        ImageUtil.deleteStorageImage(id, storageRef)
            .addOnSuccessListener {
                firestoreDb.collection(COLLECTION).document(id).delete()
                    .addOnFailureListener {
                        Log.e(TAG, "Could not delete post with id: $id", it)
                        throw (Exception("Could not delete post"))
                    }
                Log.d(TAG, "Post deleted with id: $id")
            }
            .addOnFailureListener {
                Log.e(TAG, "Could not delete image from storage for post with id: $id")
                throw (Exception("Could not delete image from storage"))
            }
    }
}