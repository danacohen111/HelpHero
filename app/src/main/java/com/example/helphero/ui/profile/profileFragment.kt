package com.example.helphero.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.helphero.R
import com.example.helphero.databases.users.UserDatabase
import com.example.helphero.databinding.FragmentProfileBinding
import com.example.helphero.models.User
import com.example.helphero.repositories.UserRepository
import com.example.helphero.ui.viewmodels.ProfileViewModel
import com.example.helphero.ui.viewmodels.ProfileViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var profileViewModel: ProfileViewModel
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var isEditMode = false
    private var currentUser: User? = null
    private var imageUri: Uri? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                imageUri = uri
                val imageBtn: ImageButton = binding.btnProfileImage
                imageUri?.let { profileViewModel.setImageUri(it) }
                imageBtn.setImageURI(uri)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.image_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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
            currentUser = user
            binding.tvUsername.text = user.name
            binding.tvEmail.text = user.email
            binding.tvPhone.text = user.phone
            binding.etPhone.setText(user.phone)

            if (!user.photoUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(user.photoUrl)
                    .fit()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(binding.btnProfileImage)
            } else {
                binding.btnProfileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }

        binding.btnProfileImage.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.ivEditProfile.setOnClickListener {
            isEditMode = !isEditMode
            toggleEditMode(isEditMode)
        }

        binding.btnSaveProfile.setOnClickListener {
            currentUser?.let { user ->
                val updatedPhone = binding.etPhone.text.toString()
                val updatedUser = user.copy(phone = updatedPhone)
                profileViewModel.updateUserProfile(updatedUser)
            } ?: Toast.makeText(requireContext(), "User data not available", Toast.LENGTH_SHORT)
                .show()
        }

        // Observe error messages
        profileViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }

        profileViewModel.isProfileUpdated.observe(viewLifecycleOwner) { success ->
            if (success) {
                currentUser?.let { user ->
                    binding.tvPhone.text = user.phone
                    binding.etPhone.setText(user.phone)
                    FirebaseAuth.getInstance().currentUser?.uid?.let {
                        profileViewModel.fetchUserDetails(
                            user.userId
                        )
                    }
                    isEditMode = false
                    toggleEditMode(isEditMode)
                }
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT)
                    .show()
            }
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

    private fun toggleEditMode(isEdit: Boolean) {
        if (isEdit) {
            binding.tvPhone.visibility = View.GONE
            binding.etPhone.visibility = View.VISIBLE
            binding.etPhone.isEnabled = true
            binding.btnSaveProfile.visibility = View.VISIBLE
            binding.btnProfileImage.isEnabled = true

        } else {
            binding.tvPhone.visibility = View.VISIBLE
            binding.etPhone.visibility = View.GONE
            binding.etPhone.isEnabled = false
            binding.btnSaveProfile.visibility = View.GONE
            binding.btnProfileImage.isEnabled = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}