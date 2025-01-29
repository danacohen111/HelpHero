package com.example.helphero.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.helphero.MainActivity
import com.example.helphero.R
import com.example.helphero.databases.users.UserDatabase
import com.example.helphero.databinding.FragmentSignUpBinding
import com.example.helphero.repositories.UserRepository
import com.example.helphero.ui.viewmodels.SignUpViewModel
import com.example.helphero.ui.viewmodels.SignUpViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SignUpViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as MainActivity).hideNavBar()

        binding.btnSignup.setOnClickListener {
            val name = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            val email = binding.etEmail.text.toString()
            val phone = binding.etPhone.text.toString()

            if (isInputValid(name, password, email, phone)) {
                viewModel.signUp(name, email, password, phone)
                Toast.makeText(requireContext(), "Sign-Up Successful", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.homeFragment)
            }
        }

        observeViewModel()
    }

    private fun isInputValid(
        name: String,
        password: String,
        email: String,
        phone: String
    ): Boolean {
        return when {
            name.isEmpty() -> {
                Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
                false
            }

            password.isEmpty() -> {
                Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show()
                false
            }

            email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT)
                    .show()
                false
            }

            phone.isEmpty() || !phone.matches(Regex("^[0-9]{10}$")) -> {
                Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT)
                    .show()
                false
            }

            else -> true
        }
    }


    private fun observeViewModel() {
        viewModel.signUpSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(context, "Sign-up successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Sign-up failed", Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (errorMessage.isNotBlank()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        (activity as MainActivity).showNavBar()
    }
}
