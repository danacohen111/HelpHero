package com.example.helphero.ui.addPost

import android.net.Uri
import androidx.lifecycle.*
import com.example.helphero.models.Post
import com.example.helphero.repositories.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PostViewModel(private val repository: PostRepository) : ViewModel() {

    private val _postSuccessful = MutableLiveData<Boolean>()
    val postSuccessful: LiveData<Boolean> get() = _postSuccessful

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> get() = _imageUri

    fun resetForm() {
        _postSuccessful.value = false
        _imageUri.value = null
    }

    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
    }

    fun savePost(title: String, desc: String, uri: Uri) {
        val postId: String = UUID.randomUUID().toString()
        val userId = "anonymous" // Use a default or anonymous user ID

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val post = Post(
                    postId = postId,
                    userId = userId,
                    title = title,
                    desc = desc,
                    imageUrl = "",
                    date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(Date()),
                    comments = emptyList()
                )
                repository.insertPost(post)
                withContext(Dispatchers.Main) {
                    _postSuccessful.value = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loading.value = false
                }
            }
        }
    }

    class PostModelFactory(private val repository: PostRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PostViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}