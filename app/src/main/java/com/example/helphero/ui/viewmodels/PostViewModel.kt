package com.example.helphero.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.helphero.models.Post
import com.example.helphero.repositories.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PostViewModel(private val repository: PostRepository) : ViewModel() {

    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    val postsLiveData: LiveData<List<Post>> = repository.postsLiveData

    private val _postSuccessful = MutableLiveData<Boolean>()
    val postSuccessful: LiveData<Boolean> get() = _postSuccessful

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> get() = _imageUri

    // Filtered posts for the current user
    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> get() = _userPosts

    init {
        fetchUserPosts(userId)
    }

    fun resetForm() {
        _postSuccessful.value = false
        _imageUri.value = null
    }

    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
    }

    fun fetchUserPosts(userId: String) {
        postsLiveData.observeForever { posts ->
            _userPosts.postValue(posts.filter { it.userId == userId })
        }
    }

    fun savePost(title: String, desc: String, imageUri: Uri, location: String) {
        val postId: String = UUID.randomUUID().toString()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val post = Post(
                    postId = postId,
                    userId = userId,
                    title = title,
                    desc = desc,
                    imageUrl = "",
                    date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(Date()),
                    location = location,
                    comments = emptyList()
                )
                repository.insertPost(post, imageUri)
                withContext(Dispatchers.Main) {
                    _postSuccessful.value = true
                    fetchUserPosts(userId)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _postSuccessful.value = false
                    _loading.value = false
                }
            }
        }
    }

    fun updatePost(postId: String, desc: String?, imageUri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updatePost(postId, desc, imageUri)
                withContext(Dispatchers.Main) {
                    _postSuccessful.value = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _postSuccessful.value = false
                }
                Log.e("PostViewModel", "Error updating post: ${e.message}")
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deletePost(postId)
                withContext(Dispatchers.Main) {
                    _postSuccessful.value = true
                    fetchUserPosts(userId)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _postSuccessful.value = false
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