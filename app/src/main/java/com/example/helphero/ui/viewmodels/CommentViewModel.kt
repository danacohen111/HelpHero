package com.example.helphero.ui.viewmodels

import androidx.lifecycle.*
import com.example.helphero.models.Comment
import com.example.helphero.repositories.CommentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommentViewModel(private val repository: CommentRepository) : ViewModel() {

    val commentsLiveData: LiveData<List<Comment>> = repository.commentsLiveData

    private val _commentSuccessful = MutableLiveData<Boolean>()
    val commentSuccessful: LiveData<Boolean> get() = _commentSuccessful

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun addComment(comment: Comment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertComment(comment)
            _commentSuccessful.postValue(true)
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComment(commentId)
            _commentSuccessful.postValue(true)
        }
    }
}
    class CommentViewModelFactory(private val repository: CommentRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CommentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CommentViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }