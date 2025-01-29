package com.example.helphero.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helphero.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _signUpSuccess = MutableLiveData<Boolean>()
    val signUpSuccess: LiveData<Boolean> = _signUpSuccess

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun signUp(name: String, email: String, password: String, phone: String) {
        _errorMessage.postValue("")
        _loading.postValue(true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.signUp(
                    email = email,
                    password = password,
                    name = name,
                    phone = phone,
                    onSuccess = {
                        _signUpSuccess.postValue(true)
                    },
                    onError = { error ->
                        _errorMessage.postValue(error)
                        _signUpSuccess.postValue(false)
                    }
                )
            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Error during sign-up: ${e.message}")
                _errorMessage.postValue("Error during sign-up: ${e.message}")
                _signUpSuccess.postValue(false)
            } finally {
                _loading.postValue(false)
            }
        }
    }
}
