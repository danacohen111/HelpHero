package com.yourpackage.ui.signup

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
//import androidx.navigation.fragment.findNavController
//import com.google.firebase.auth.FirebaseAuth
import com.example.helphero.R
import com.example.helphero.R.layout.fragment_signup

class SignUpFragment : Fragment(fragment_signup) {

    private lateinit var viewModel: SignUpViewModel
//    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize FirebaseAuth
//        auth = FirebaseAuth.getInstance()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(SignUpViewModel::class.java)

        val emailField = view.findViewById<EditText>(R.id.emailField)
        val passwordField = view.findViewById<EditText>(R.id.passwordField)
        val signUpButton = view.findViewById<View>(R.id.signupButton)

        // Sign Up Button Click Listener
        signUpButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call ViewModel to create account
//            viewModel.signUp(auth, email, password) { isSuccessful, errorMessage ->
//                if (isSuccessful) {
//                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
//                    findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
//                } else {
//                    Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
//                }
//            }
        }

    }
}
