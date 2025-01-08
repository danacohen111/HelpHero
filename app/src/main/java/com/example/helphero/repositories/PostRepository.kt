package com.example.helphero.data.repositories

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.helphero.data.database.posts.PostDao
import com.example.helphero.models.FirestorePost
import com.example.helphero.models.Post
import com.example.helphero.models.toRoomPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PostRepository (private val firestoreDb: FirebaseFirestore, private val firebaseAuth: FirebaseAuth, private val postDao: PostDao) {

    private val COLLECTION = "posts"
    private var postsListenerRegistration: ListenerRegistration? = null

    private val _postsLiveData = MutableLiveData<List<Post>>()
    val postsLiveData: LiveData<List<Post>> get() = _postsLiveData

    @WorkerThread
    fun get (id: Int): Post = postDao.get(id)

    @WorkerThread
    fun getUserPosts(userId: String): Flow<List<Post>> = postDao.getUserPosts(userId)

    @WorkerThread
    suspend fun insert(post: Post) { postDao.insert(post) }

    @WorkerThread
    suspend fun delete (post: Post) = postDao.delete(post)

    init {
        listenForPostUpdates()
    }

    private fun listenForPostUpdates() {
        postsListenerRegistration = firestoreDb.collection(COLLECTION).orderBy("createdString", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val posts = mutableListOf<Post>()

                snapshot?.documents?.forEach {document ->
                    val firestorePost = document.toObject(FirestorePost::class.java)
                    firestorePost?.let {fsPost ->
                        val post = fsPost.toRoomPost(document.id)
                        insert(post)
                        posts.add(post)
                    }
                }
                _postsLiveData.postValue(posts)
            }
        }

        firestoreDb.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
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
                    }
                }
            }
    }
}