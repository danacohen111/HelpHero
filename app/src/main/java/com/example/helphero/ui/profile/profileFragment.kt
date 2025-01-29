package com.example.helphero.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
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
    private lateinit var binding: FragmentProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        val userDao = UserDatabase.getDatabase(requireContext()).userDao()
        val userRepository = UserRepository(
            firestoreDb = FirebaseFirestore.getInstance(),
            firestoreAuth = FirebaseAuth.getInstance(),
            contentResolver = requireContext().contentResolver,
            userDao = userDao
        )

        // Create an instance of ProfileViewModel using the factory
        val factory = ProfileViewModelFactory(userRepository)
        profileViewModel = ViewModelProvider(this, factory).get(ProfileViewModel::class.java)

        // Observe the LiveData for user profile
        profileViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            // Bind the user data to the views
            binding.tvUsername.text = user.name
            binding.tvEmail.text = user.email
            binding.tvPhone.text = user.phone

            // Load profile image (use Picasso)
            if (!user.photoUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(user.photoUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .transform(CropCircleTransformation())
                    .into(binding.profileImageView)
            } else {
                // Set a placeholder image if photoUrl is empty
                binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        })

        // Observe the loading state
        profileViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        })

        // Observe the error message
        profileViewModel.errorMessage.observe(viewLifecycleOwner, Observer { error ->
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        })

        // Fetch the user details (pass the user ID here)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        currentUserId?.let { profileViewModel.fetchUserDetails(it) }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "Signed Out Successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.signInFragment)
        }
    }
}