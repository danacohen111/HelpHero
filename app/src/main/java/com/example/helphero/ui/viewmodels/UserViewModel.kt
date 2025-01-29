package com.example.helphero.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helphero.models.User
import com.example.helphero.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: MutableLiveData<User?> get() = _user
    var TAG = "UserViewModel"

    fun getUserById(userId: String) {
        viewModelScope.launch {
            if (isAnonymousUser(userId)) {
                _user.value = getDefaultUser()
            } else {
                try {
                    val fetchedUser = withContext(Dispatchers.IO) {
                        userRepository.get(userId)
                    }
                    _user.value = fetchedUser
                } catch (e: Exception) {
                    Log.d(TAG, "Error fetching user: ${e.message}")
                }
            }
        }
    }

    private fun isAnonymousUser(userId: String): Boolean {
        return userId == "anonymous"
    }

    private fun getDefaultUser(): User {
        return User(
            userId = "anonymous",
            name = "Anonymous",
            phone = "11111111",
            photoUrl = "ic_profile_placeholder",
            email = "anonymous@example.com",
            password = "default_password"
        )
    }
}

class UserViewModelFactory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}