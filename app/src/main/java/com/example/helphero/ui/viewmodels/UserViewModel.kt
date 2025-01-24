package com.example.helphero.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helphero.models.User
import com.example.helphero.repositories.UserRepository
import kotlinx.coroutines.launch

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user

    // Make sure the database call runs off the main thread using a coroutine
    fun getUserById(userId: String) {
        viewModelScope.launch {
            if (isAnonymousUser(userId)) {
                _user.value = getDefaultUser()
            } else {
                try {
                    // Call the suspend function to get the user off the main thread
                    val fetchedUser = userRepository.get(userId)
                    _user.value = fetchedUser
                } catch (e: Exception) {
                    // Handle any potential errors (e.g., user not found)
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
