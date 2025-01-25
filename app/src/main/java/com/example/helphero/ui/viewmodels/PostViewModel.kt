package com.example.helphero.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helphero.models.Post
import com.example.helphero.repositories.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PostViewModel(private val repository: PostRepository) : ViewModel() {

    val postsLiveData: LiveData<List<Post>> = repository.postsLiveData

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

    // TO DO: Check the authenticated user and save the post
    fun savePost(title: String, desc: String, imageUri: Uri) {
        val postId: String = UUID.randomUUID().toString()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

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
                repository.insertPost(post, imageUri)
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