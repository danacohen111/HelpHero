package com.example.helphero.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.helphero.databases.users.UserDao
import com.example.helphero.repositories.UserRepository
import com.example.helphero.ui.signin.SignInViewModel

class SignInViewModelFactory(
    private val userRepository: UserRepository,
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignInViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignInViewModel(userRepository, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
