package com.yourpackage.ui.signup

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class SignUpViewModel : ViewModel() {

    fun signUp(
        auth: FirebaseAuth,
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        onComplete: (isSuccessful: Boolean, errorMessage: String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }
}
