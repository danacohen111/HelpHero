package com.example.helphero.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.helphero.R
import com.example.helphero.databases.users.UserDatabase
import com.example.helphero.databinding.FragmentSignUpBinding
import com.example.helphero.repositories.UserRepository
import com.example.helphero.viewmodels.SignUpViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class SignUpFragment : Fragment(R.layout.fragment_sign_up) {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SignUpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignUpBinding.inflate(inflater, container, false)

        val userDao = UserDatabase.getDatabase(requireContext()).userDao()
        val userRepository = UserRepository(
            firestoreDb = FirebaseFirestore.getInstance(),
            firestoreAuth = FirebaseAuth.getInstance(),
            contentResolver = requireContext().contentResolver,
            userDao = userDao
        )

        viewModel = ViewModelProvider(
            this,
            SignUpViewModelFactory(userRepository)
        )[SignUpViewModel::class.java]

        setupListeners(binding)
        setupObservers(binding)

        return binding.root
    }

    private fun setupListeners() {
        binding.btnSignUp.setOnClickListener {
            val name = binding.etUserName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val phone = binding.etPhoneNumber.text.toString().trim()
            val imageUrl = binding.etImageUrl.text.toString().trim()  // Get the image URL

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || imageUrl.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.signUp(name, email, password, phone, imageUrl)
        }
    }

    private fun setupObservers() {
        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        // Observe sign-up success
        viewModel.signUpSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(requireContext(), "Sign-Up Successful!", Toast.LENGTH_SHORT).show()
                navigateToHomeScreen()
            }
        }
    }

    private fun navigateToHomeScreen() {
        // Navigate to home screen or other screen after successful sign-up
        findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signUpButton.setOnClickListener {
            val name = binding.usernameInput.text.toString()
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val phone = binding.phoneInput.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                viewModel.signUp(name, email, password, phone,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Sign-up successful!", Toast.LENGTH_SHORT)
                            .show()
                        // Navigate to the next screen
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    })
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}