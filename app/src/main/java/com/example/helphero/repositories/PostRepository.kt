package com.example.helphero.repositories

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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val firestoreDb: FirebaseFirestore,
    private val postDao: PostDao,
) {
    private val TAG = "PostRepository"
    private val COLLECTION = "posts"
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
    suspend fun deleteById(postId: String) {
        Log.d(TAG, "Deleting post by id from Room database")
        postDao.deleteById(postId)
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
        if (postsListenerRegistration != null) return

        Log.d(TAG, "Starting to listen for post updates")
        postsListenerRegistration = firestoreDb.collection(COLLECTION)
            .orderBy(
                "date",
                Query.Direction.ASCENDING
            )
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for post updates", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.documentChanges.isEmpty()) {
                    Log.d(TAG, "No changes in Firestore snapshot")
                    _postsLiveData.postValue(emptyList())
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
                                    insert(post)
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

                    val sortedPosts = postDao.getAll().sortedBy { it.date }
                    if (sortedPosts.isNotEmpty()) {
                        _postsLiveData.postValue(sortedPosts)
                    } else {
                        _postsLiveData.postValue(emptyList())
                    }

                    Log.d(
                        TAG,
                        "Processed Firestore changes: ${updatedPosts.size} added/modified, ${removedPosts.size} removed"
                    )
                }
            }
    }

    fun insertPost(post: Post, imageUri: Uri) {
        _loading.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Uploading image for post with id: ${post.postId}")
                val imageUrl = ImageUtil.uploadImage(post.postId, imageUri).toString()
                post.imageUrl = imageUrl

                val fsPost = post.toFirestorePost()
                firestoreDb.collection(COLLECTION).document(post.postId).set(fsPost)
                    .addOnSuccessListener {
                        Log.d(TAG, "Post successfully inserted with id: ${post.postId}")
                        _postSuccessful.postValue(true)
                        listenForPostUpdates()
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

    fun updatePost(
        postId: String,
        desc: String?,
        imageUri: Uri?
    ) {
        _loading.postValue(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val postRef = firestoreDb.collection(COLLECTION).document(postId)

                val snapshot = postRef.get().await()
                if (!snapshot.exists()) {
                    Log.e(TAG, "Post not found: $postId")
                    return@launch
                }

                val existingPost = snapshot.toObject(FirestorePost::class.java) ?: return@launch
                val updates = mutableMapOf<String, Any>()

                desc?.let { updates["desc"] = it }

                if (imageUri != null) {
                    existingPost.imageUrl.takeIf { it.isNotEmpty() }?.let { oldImageUrl ->
                        ImageUtil.deleteImage(oldImageUrl)
                    }
                    val newImageUrl = ImageUtil.uploadImage(postId, imageUri).toString()
                    updates["imageUrl"] = newImageUrl
                }

                postRef.update(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Post updated successfully: $postId")
                        _postSuccessful.postValue(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating post: $postId", e)
                        _postSuccessful.postValue(false)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during post update", e)
                _postSuccessful.postValue(false)
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun deletePost(postId: String) {
        _loading.postValue(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val postRef = firestoreDb.collection(COLLECTION).document(postId)

                val snapshot = postRef.get().await()
                if (snapshot.exists()) {
                    val existingPost = snapshot.toObject(FirestorePost::class.java)
                    existingPost?.imageUrl?.takeIf { it.isNotEmpty() }?.let { oldImageUrl ->
                        try {
                            Log.d(TAG, "Attempting to delete image at URL: $oldImageUrl")
                            ImageUtil.deleteImage(oldImageUrl)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deleting image: $oldImageUrl", e)
                        }
                    }
                }

                postRef.delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "Post deleted successfully: $postId")
                        CoroutineScope(Dispatchers.IO).launch {
                            postDao.deleteById(postId)
                        }
                        _postSuccessful.postValue(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error deleting post: $postId", e)
                        _postSuccessful.postValue(false)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during post deletion", e)
                _postSuccessful.postValue(false)
            } finally {
                _loading.postValue(false)
            }
        }
    }
}
