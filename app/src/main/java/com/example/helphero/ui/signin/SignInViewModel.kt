package com.example.helphero.ui.signin

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helphero.databases.users.UserDao
import com.example.helphero.models.User
import com.example.helphero.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignInViewModel(private val userRepository: UserRepository, private val userDao: UserDao) :
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
                // First, check the local database for the user
                val localUser = userDao.get(email)
                if (localUser != null && localUser.password == password) {
                    // Local sign-in success
                    _signInSuccess.postValue(true)
                    _loading.postValue(false)
                    return@launch
                }

                // If not found in local DB, fallback to Firebase authentication
                userRepository.login(email, password) { error ->
                    _errorMessage.postValue(error)
                    _signInSuccess.postValue(false)
                }

                // If Firebase authentication is successful, save user locally
                if (userRepository.loginSuccessfull.value == true) {
                    val firebaseUser = userRepository.currUser.value
                    firebaseUser?.let {
                        val newUser = User(
                            userId = it.uid,
                            name = it.displayName ?: "",
                            phone = "",
                            photoUrl = it.photoUrl.toString(),
                            email = it.email ?: "",
                            password = password
                        )
                        userDao.update(newUser)
                    }
                    _signInSuccess.postValue(true)
                }
            } catch (e: Exception) {
                Log.e("SignInViewModel", "Error during sign-in: ${e.message}")
                _errorMessage.postValue("Error during sign-in: ${e.message}")
                _signInSuccess.postValue(false)
            } finally {
                _loading.postValue(false)
            }
        }
    }
}