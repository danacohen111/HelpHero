package com.example.helphero.ui.profile

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.helphero.R
import com.example.helphero.databases.comments.CommentDatabase
import com.example.helphero.databases.users.UserDatabase
import com.example.helphero.databinding.FragmentProfileBinding
import com.example.helphero.models.Post
import com.example.helphero.models.User
import com.example.helphero.repositories.CommentRepository
import com.example.helphero.repositories.UserRepository
import com.example.helphero.ui.adapters.PostAdapter
import com.example.helphero.ui.viewmodels.CommentViewModel
import com.example.helphero.ui.viewmodels.CommentViewModelFactory
import com.example.helphero.ui.viewmodels.PostViewModel
import com.example.helphero.ui.viewmodels.ProfileViewModel
import com.example.helphero.ui.viewmodels.ProfileViewModelFactory
import com.example.helphero.utils.ImageUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val TAG = "ProfileFragment"
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val postViewModel: PostViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null
    private var pendingPostUpdate: Pair<String, String>? = null

    private var isEditMode = false
    private var currentUser: User? = null
    private var imageUri: Uri? = null

    private lateinit var commentRepository: CommentRepository
    private lateinit var commentViewModel: CommentViewModel
    private lateinit var userRepository: UserRepository
    private lateinit var profileViewModel: ProfileViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val firestoreDb = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        val commentDao = CommentDatabase.getDatabase(context).commentDao()
        commentRepository = CommentRepository(firestoreDb, firebaseAuth, commentDao)

        val userDao = UserDatabase.getDatabase(context).userDao()
        userRepository = UserRepository(
            firestoreDb = firestoreDb,
            firestoreAuth = firebaseAuth,
            userDao = userDao
        )

        commentViewModel = ViewModelProvider(
            requireActivity(),
            CommentViewModelFactory(commentRepository)
        )[CommentViewModel::class.java]

        profileViewModel = ViewModelProvider(
            requireActivity(),
            ProfileViewModelFactory(userRepository)
        )[ProfileViewModel::class.java]
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

        context?.let { context ->
            profileViewModel.user.observe(viewLifecycleOwner) { user ->
                currentUser = user
                binding.tvUsername.text = user.name
                binding.tvEmail.text = user.email
                binding.tvPhone.text = user.phone
                binding.etPhone.setText(user.phone)
                binding.etUsername.setText(user.name)

                if (!user.photoUrl.isNullOrEmpty()) {
                    ImageUtil.loadImage(
                        Uri.parse(user.photoUrl),
                        binding.btnProfileImage,
                        R.drawable.ic_profile_placeholder
                    )
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
                    val updatedUser = user.copy(
                        name = binding.etUsername.text.toString(),
                        phone = binding.etPhone.text.toString(),
                        photoUrl = imageUri?.toString() ?: user.photoUrl
                    )
                    profileViewModel.updateUserProfile(updatedUser, imageUri)
                } ?: Toast.makeText(context, "User data not available", Toast.LENGTH_SHORT).show()
            }

            profileViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            }

            profileViewModel.isProfileUpdated.observe(viewLifecycleOwner) { success ->
                if (success) {
                    currentUser?.let { user ->
                        binding.tvPhone.text = user.phone
                        binding.etPhone.setText(user.phone)
                        FirebaseAuth.getInstance().currentUser?.uid?.let {
                            profileViewModel.fetchUserDetails(user.userId)
                        }
                        isEditMode = false
                        toggleEditMode(isEditMode)
                    }
                } else {
                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }

            FirebaseAuth.getInstance().currentUser?.uid?.let { profileViewModel.fetchUserDetails(it) }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            postViewModel.fetchUserPosts(currentUserId)

            val adapter = PostAdapter(
                lifecycleOwner = viewLifecycleOwner,
                context = context,
                onEditClick = { post -> updatePost(post) },
                onDeleteClick = { post -> deletePost(post) }
            )
            binding.recyclerViewUserPosts.adapter = adapter
            binding.recyclerViewUserPosts.layoutManager = LinearLayoutManager(context)

            postViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
                adapter.submitList(posts)
            }

            binding.btnSignOut.setOnClickListener {
                FirebaseAuth.getInstance().signOut()

                FirebaseAuth.getInstance().addAuthStateListener { auth ->
                    if (auth.currentUser == null) {
                        Toast.makeText(context, "Signed Out Successfully", Toast.LENGTH_SHORT)
                            .show()
                        findNavController().navigate(R.id.signInFragment)
                    }
                }
            }
        }
    }

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

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImageUri = uri
            Log.d(TAG, "Image picked: $selectedImageUri")

            pendingPostUpdate?.let { (postId, newDescription) ->
                postViewModel.updatePost(postId, newDescription, selectedImageUri)
                pendingPostUpdate = null
            }
        }

    private fun updatePost(post: Post) {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.setText(post.desc)

        builder.setView(input)
            .setTitle("Update Post Description")
            .setPositiveButton("OK") { _, _ ->
                val newDescription = input.text.toString()

                AlertDialog.Builder(requireContext())
                    .setTitle("Choose New Picture")
                    .setMessage("Would you like to select a new image for this post?")
                    .setPositiveButton("Select Image") { _, _ ->
                        pendingPostUpdate = post.postId to newDescription
                        imagePickerLauncher.launch("image/*")
                    }
                    .setNegativeButton("No, keep existing") { _, _ ->
                        postViewModel.updatePost(post.postId, newDescription, null)
                    }
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: Post) {
        lifecycleScope.launch {
            val comments = commentViewModel.getCommentsForPost(post.postId)
            comments.forEach { comment ->
                commentViewModel.deleteComment(comment.commentId)
            }
            postViewModel.deletePost(post.postId)
        }
    }

    private fun toggleEditMode(isEdit: Boolean) {
        binding.tvUsername.visibility = if (isEdit) View.GONE else View.VISIBLE
        binding.etUsername.visibility = if (isEdit) View.VISIBLE else View.GONE
        binding.etUsername.isEnabled = isEdit

        binding.tvPhone.visibility = if (isEdit) View.GONE else View.VISIBLE
        binding.etPhone.visibility = if (isEdit) View.VISIBLE else View.GONE
        binding.etPhone.isEnabled = isEdit

        binding.btnSaveProfile.visibility = if (isEdit) View.VISIBLE else View.GONE
        binding.btnProfileImage.isEnabled = isEdit
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
