package com.example.helphero.repositories

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

    @WorkerThread
    suspend fun getCommentsForPost(postId: String): List<Comment> {
        return commentDao.getPostComments(postId)
    }

    init {
        listenForCommentUpdates()
    }

    private fun listenForCommentUpdates() {
        Log.d(TAG, "Listening for comment updates...")

        commentsListenerRegistration?.remove()
        commentsListenerRegistration = firestoreDb.collection(COLLECTION)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for comment updates", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.documentChanges.isEmpty()) {
                    Log.d(TAG, "No changes in Firestore snapshot")
                    return@addSnapshotListener
                }

                Log.d(TAG, "Received ${snapshot.documentChanges.size} changes from Firestore")

                CoroutineScope(Dispatchers.IO).launch {
                    val updatedComments = mutableListOf<Comment>()
                    val removedComments = mutableListOf<Comment>()

                    snapshot.documentChanges.forEach { change ->
                        try {
                            val firestoreComment = change.document.toObject(FirestoreComment::class.java)
                            val comment = firestoreComment.toRoomComment(change.document.id)

                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    Log.d(TAG, "Adding/Updating comment: $comment")  // <-- New log
                                    insert(comment)
                                    updatedComments.add(comment)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    Log.d(TAG, "Removing comment: $comment")
                                    delete(comment)
                                    removedComments.add(comment)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing document change: ${change.document.id}", e)
                        }
                    }

                    if (updatedComments.isNotEmpty() || removedComments.isNotEmpty()) {
                        val allComments = commentDao.getAll()
                        Log.d(TAG, "Updated Comments from Room: $allComments")
                        _commentsLiveData.postValue(allComments)
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