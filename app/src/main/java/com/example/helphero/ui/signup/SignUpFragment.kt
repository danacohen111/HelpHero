package com.yourpackage.ui.signup

//import androidx.navigation.fragment.findNavController
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.helphero.R
import com.example.helphero.R.layout.fragment_signup
import com.example.helphero.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth

class SignUpFragment : Fragment(fragment_signup) {

    private lateinit var viewModel: SignUpViewModel
    private lateinit var binding: FragmentSignupBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignupBinding.bind(view)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(SignUpViewModel::class.java)

        val nameField = view.findViewById<EditText>(R.id.nameField)
        val emailField = view.findViewById<EditText>(R.id.emailField)
        val passwordField = view.findViewById<EditText>(R.id.passwordField)
        val phoneNumberField = view.findViewById<EditText>(R.id.phoneNumberField)
        val signUpButton = view.findViewById<View>(R.id.signupButton)

        // Sign Up Button Click Listener
        signUpButton.setOnClickListener {
            val name = nameField.text.toString()
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val phoneNumber = phoneNumberField.text.toString()

            if (name.isBlank() || email.isBlank() || password.isBlank() || phoneNumber.isBlank()) {
                Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call ViewModel to create account
            viewModel.signUp(
                auth,
                name,
                email,
                password,
                phoneNumber
            ) { isSuccessful, errorMessage ->
                if (isSuccessful) {
                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT)
                        .show()
//                    findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
                } else {
                    Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    fun validation(): Boolean {
        val name = binding.nameField.text
        val email = binding.emailField.text
        val password = binding.passwordField.text
        val phoneNumber = binding.phoneNumberField.text

        if (name.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_name), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_email), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (password.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_password), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_password), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        return true
    }
}
