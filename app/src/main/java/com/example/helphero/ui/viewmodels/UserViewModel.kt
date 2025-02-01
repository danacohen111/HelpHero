package com.example.helphero.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helphero.models.User
import com.example.helphero.repositories.UserRepository
import kotlinx.coroutines.launch

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val userCache = mutableMapOf<String, MutableLiveData<User?>>()
    var TAG = "UserViewModel"

    fun getUserById(userId: String): LiveData<User?> {
        if (userCache.containsKey(userId)) {
            return userCache[userId]!!
        }

        val liveData = MutableLiveData<User?>()
        userCache[userId] = liveData

        if (isAnonymousUser(userId)) {
            liveData.value = getDefaultUser()
        } else {
            userRepository.get(
                userId,
                onSuccess = { fetchedUser ->
                    liveData.postValue(fetchedUser)
                    Log.d(TAG, "Fetched user: ${fetchedUser.name}")
                },
                onError = { error -> Log.d(TAG, "Error fetching user: $error") }
            )
        }
        return liveData
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

    fun updateUser(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            userRepository.updateUser(user, onSuccess, onError)
        }
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
