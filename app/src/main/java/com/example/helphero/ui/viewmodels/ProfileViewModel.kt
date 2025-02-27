package com.example.helphero.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.helphero.models.User
import com.example.helphero.repositories.UserRepository

class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {
    val user = MutableLiveData<User>()
    val errorMessage = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    val isProfileUpdated = MutableLiveData<Boolean>()
    val successMessage = MutableLiveData<String>()
    private val _imageUri = MutableLiveData<Uri?>()

    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
    }

    fun fetchUserDetails(userId: String) {
        isLoading.value = true
        userRepository.get(userId, { fetchedUser ->
            user.value = fetchedUser
            isLoading.value = false
        }, { error ->
            errorMessage.value = error
            isLoading.value = false
        })
    }

    fun updateUserProfile(updatedUser: User, imageUri: Uri?) {
        isLoading.value = true
        userRepository.updateUser(updatedUser, imageUri,
            onSuccess = {
                successMessage.value = "Profile updated successfully"
                isLoading.value = false
                isProfileUpdated.value = true
            },
            onError = { error ->
                errorMessage.value = error
                isLoading.value = false
                isProfileUpdated.value = false
            })
    }

}
