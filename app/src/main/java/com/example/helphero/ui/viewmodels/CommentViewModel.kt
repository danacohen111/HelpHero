package com.example.helphero.ui.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.helphero.models.Comment
import com.example.helphero.repositories.CommentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentViewModel(private val repository: CommentRepository) : ViewModel() {

    val TAG = "CommentViewModel"

    val commentsLiveData: LiveData<List<Comment>> = repository.commentsLiveData

    private val _commentSuccessful = MutableLiveData<Boolean>()
    val commentSuccessful: LiveData<Boolean> get() = _commentSuccessful

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun addComment(comment: Comment) {
        _loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertComment(comment)
                withContext(Dispatchers.Main) {
                    _commentSuccessful.value = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error adding comment", e)
                    _commentSuccessful.value = false
                }
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun deleteComment(commentId: String) {
        _loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteComment(commentId)
                withContext(Dispatchers.Main) {
                    _commentSuccessful.value = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error deleting comment", e)
                    _commentSuccessful.value = false
                }
            } finally {
                _loading.postValue(false)
            }
        }
    }

    suspend fun getCommentsForPost(postId: String): List<Comment> {
        return withContext(Dispatchers.IO) {
            repository.getCommentsForPost(postId)
        }
    }

}