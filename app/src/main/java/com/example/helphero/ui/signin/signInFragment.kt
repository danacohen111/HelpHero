package com.example.helphero.ui.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.helphero.R
import com.example.helphero.databases.users.UserDatabase
import com.example.helphero.databinding.FragmentSignInBinding
import com.example.helphero.repositories.UserRepository
import com.example.helphero.ui.viewmodels.SignInViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.helphero.MainActivity
import com.example.helphero.ui.viewmodels.SignInViewModel

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SignInViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)

        // Initialize ViewModel with required dependencies
        val userDao = UserDatabase.getDatabase(requireContext()).userDao()
        val userRepository = UserRepository(
            firestoreDb = FirebaseFirestore.getInstance(),
            firestoreAuth = FirebaseAuth.getInstance(),
            contentResolver = requireContext().contentResolver,
            userDao = userDao
        )
        viewModel = ViewModelProvider(
            this,
            SignInViewModelFactory(userRepository)
        )[SignInViewModel::class.java]

        (activity as MainActivity).hideNavBar()

        setupListeners()
        setupObservers()

        return binding.root
    }

    private fun setupListeners() {
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.signIn(email, password)
        }

        binding.btnSignUp.setOnClickListener {
            Toast.makeText(requireContext(), "Navigate to Sign-Up Page", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.signUpFragment)
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide a loading indicator
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.signInSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(requireContext(), "Sign-In Successful!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.homeFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        (activity as MainActivity).showNavBar()
    }
}
