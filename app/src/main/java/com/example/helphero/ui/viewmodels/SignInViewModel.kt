package com.example.helphero.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helphero.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignInViewModel(private val userRepository: UserRepository) :
    ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _signInSuccess = MutableLiveData<Boolean>()
    val signInSuccess: LiveData<Boolean> = _signInSuccess

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun signIn(email: String, password: String) {
        _errorMessage.postValue("")
        _loading.postValue(true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.login(email, password) { error ->
                    _errorMessage.postValue(error)
                    _signInSuccess.postValue(false)
                }

                if (userRepository.loginSuccessfull.value == true) {
                    _signInSuccess.postValue(true)
                    _loading.postValue(false)
                }
            } catch (e: Exception) {
                Log.e("SignInViewModel", "Error during sign-in: ${e.message}")
                _errorMessage.postValue("Error during sign-in: ${e.message}")
                _signInSuccess.postValue(false)
            } finally {
                _signInSuccess.postValue(true)
                _loading.postValue(false)
            }
        }
    }
}