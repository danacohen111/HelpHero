package com.example.helphero.ui.profile

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
import com.example.helphero.databinding.FragmentProfileBinding
import com.example.helphero.repositories.UserRepository
import com.example.helphero.ui.viewmodels.ProfileViewModel
import com.example.helphero.ui.viewmodels.ProfileViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var profileViewModel: ProfileViewModel
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel with repository
        val userDao = UserDatabase.getDatabase(requireContext()).userDao()
        val userRepository = UserRepository(
            firestoreDb = FirebaseFirestore.getInstance(),
            firestoreAuth = FirebaseAuth.getInstance(),
            userDao = userDao
        )
        val factory = ProfileViewModelFactory(userRepository)
        profileViewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]

        // Observe user data and update UI
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            binding.tvUsername.text = user.name
            binding.tvEmail.text = user.email
            binding.tvPhone.text = user.phone

            if (!user.photoUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(user.photoUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .transform(CropCircleTransformation())
                    .into(binding.profileImageView)
            } else {
                binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }

        // Observe error messages
        profileViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }

        // Fetch user details when fragment is displayed
        FirebaseAuth.getInstance().currentUser?.uid?.let { profileViewModel.fetchUserDetails(it) }

        // Sign Out button
        binding.btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "Signed Out Successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.signInFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
