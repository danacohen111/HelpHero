package com.example.helphero.data.repositories

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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PostRepository(private val firestoreDb: FirebaseFirestore, private val firebaseAuth: FirebaseAuth, private val postDao: PostDao) {

    private val TAG = "PostRepository"
    private val COLLECTION = "posts"
    private val storageRef: StorageReference = Firebase.storage.reference.child("posts")

    private var postsListenerRegistration: ListenerRegistration? = null

    private val _postsLiveData = MutableLiveData<List<Post>>()
    val postsLiveData: LiveData<List<Post>> get() = _postsLiveData

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _postImage = MutableLiveData<Uri>()
    val postImage: LiveData<Uri> get() = _postImage

    private val _postSuccessful = MutableLiveData<Boolean>()
    val postSuccessful: LiveData<Boolean> get() = _postSuccessful

    @WorkerThread
    fun get(id: Int): Post {
        Log.d(TAG, "Fetching post with id: $id")
        return postDao.get(id)
    }

    @WorkerThread
    fun getUserPosts(userId: String): Flow<List<Post>> {
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
        postsListenerRegistration = firestoreDb.collection(COLLECTION).orderBy("createdString", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for post updates", error)
                return@addSnapshotListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val posts = mutableListOf<Post>()

                snapshot?.documents?.forEach { document ->
                    val firestorePost = document.toObject(FirestorePost::class.java)
                    firestorePost?.let { fsPost ->
                        val post = fsPost.toRoomPost(document.id)
                        insert(post)
                        posts.add(post)
                    }
                }
                _postsLiveData.postValue(posts)
                Log.d(TAG, "Post updates received: ${posts.size} posts")
            }
        }

        firestoreDb.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for deleted posts", error)
                    return@addSnapshotListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val deletedItems = snapshot?.documentChanges
                        ?.filter { it.type == DocumentChange.Type.REMOVED }
                        ?.mapNotNull { change ->
                            change.document.toObject(FirestorePost::class.java)
                                .toRoomPost(change.document.id)
                        }

                    deletedItems?.let { items ->
                        items.forEach { delete(it) }
                        Log.d(TAG, "Deleted posts received: ${items.size} posts")
                    }
                }
            }
    }

    fun insertPost(post: Post) {
        _loading.postValue(true)
        try {
            _postImage.value?.let { uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    Log.d(TAG, "Inserting post with id: ${post.postId}")
                    if (post.imageUrl.isEmpty()) {
                        post.imageUrl = ImageUtil.UploadImage(post.postId, uri, storageRef).toString()
                        Log.d(TAG, "Image uploaded for post with id: ${post.postId}")
                    }
                    val fsPost = post.toFirestorePost()
                    firestoreDb.collection(COLLECTION).document(post.postId).set(fsPost)
                    Log.d(TAG, "Post inserted with id: ${post.postId}")
                }
            }
        } finally {
            _postSuccessful.postValue(true)
            _loading.postValue(false)
            Log.d(TAG, "Post insertion completed for id: ${post.postId}")
        }
    }

    fun deletePost(id: String) {
        Log.d(TAG, "Deleting post with id: $id")
        ImageUtil.deleteStorageImage(id, storageRef)
            .addOnSuccessListener {
                firestoreDb.collection(COLLECTION).document(id).delete()
                    .addOnFailureListener {
                        Log.e(TAG, "Could not delete post with id: $id", it)
                        throw(Exception("Could not delete post"))
                    }
                Log.d(TAG, "Post deleted with id: $id")
            }
            .addOnFailureListener {
                Log.e(TAG, "Could not delete image from storage for post with id: $id")
                throw(Exception("Could not delete image from storage"))
            }
    }
}