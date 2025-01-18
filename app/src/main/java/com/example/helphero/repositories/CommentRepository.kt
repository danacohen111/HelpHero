package com.example.helphero.data.repositories

import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.helphero.databases.comments.CommentDao
import com.example.helphero.models.Comment
import com.example.helphero.models.FirestoreComment
import com.example.helphero.models.toFirestoreComment
import com.example.helphero.models.toRoomComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommentRepository(private val firestoreDb: FirebaseFirestore, private val firebaseAuth: FirebaseAuth, private val commentDao: CommentDao) {

    private val TAG = "CommentsRepository"
    private val firebaseDb: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val COLLECTION = "comments"
    private var commentsListenerRegistration: ListenerRegistration? = null

    private val _commentsLiveData = MutableLiveData<List<Comment>>()
    val commentsLiveData: LiveData<List<Comment>> get() = _commentsLiveData

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _commentSuccessful = MutableLiveData<Boolean>()
    val commentSuccessful: LiveData<Boolean> get() = _commentSuccessful

    @WorkerThread
    fun get(id: String): Comment {
        Log.d(TAG, "Fetching comment with id: $id")
        return commentDao.get(id)
    }

    @WorkerThread
    fun getPostComments(postId: String): List<Comment> {
        Log.d(TAG, "Fetching comments for post with id: $postId")
        return commentDao.getPostComments(postId)
    }

    @WorkerThread
    suspend fun insert(comment: Comment) {
        Log.d(TAG, "Inserting comment with id: ${comment.commentId}")
        commentDao.insert(comment)
    }

    @WorkerThread
    suspend fun delete(comment: Comment) {
        Log.d(TAG, "Deleting comment with id: ${comment.commentId}")
        commentDao.delete(comment)
    }

    init {
        listenForCommentUpdates()
    }

    private fun listenForCommentUpdates() {
        Log.d(TAG, "Listening for comment updates")
        commentsListenerRegistration = firestoreDb.collection(COLLECTION).orderBy("createdString", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for comment updates", error)
                return@addSnapshotListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val comments = mutableListOf<Comment>()

                snapshot?.documents?.forEach { document ->
                    val firestoreComment = document.toObject(FirestoreComment::class.java)
                    firestoreComment?.let { fsComment ->
                        val comment = fsComment.toRoomComment(document.id)
                        insert(comment)
                        comments.add(comment)
                    }
                }
                _commentsLiveData.postValue(comments)
                Log.d(TAG, "Comment updates received: ${comments.size} comments")
            }
        }

        firestoreDb.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for deleted comments", error)
                    return@addSnapshotListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val deletedItems = snapshot?.documentChanges
                        ?.filter { it.type == DocumentChange.Type.REMOVED }
                        ?.mapNotNull { change ->
                            change.document.toObject(FirestoreComment::class.java)
                                .toRoomComment(change.document.id)
                        }

                    deletedItems?.let { items ->
                        items.forEach { delete(it) }
                        Log.d(TAG, "Deleted comments received: ${items.size} comments")
                    }
                }
            }
    }

    fun insertComment(comment: Comment) {
        _loading.postValue(true)
        try {
            val fsComment = comment.toFirestoreComment()
            firebaseDb.collection(COLLECTION).document(comment.commentId).set(fsComment)
            Log.d(TAG, "Comment inserted to Firebase with id: ${comment.commentId}")
            _commentSuccessful.postValue(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting comment to Firebase", e)
        } finally {
            _loading.postValue(false)
        }
    }

    fun deleteComment(commentId: String) {
        _loading.postValue(true)
        try {
            firebaseDb.collection(COLLECTION).document(commentId).delete()
            Log.d(TAG, "Comment deleted from Firebase with id: $commentId")
            _commentSuccessful.postValue(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting comment from Firebase", e)
        } finally {
            _loading.postValue(false)
        }
    }
}