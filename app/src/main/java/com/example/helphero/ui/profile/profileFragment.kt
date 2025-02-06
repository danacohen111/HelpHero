package com.example.helphero.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.helphero.R
import com.example.helphero.databinding.FragmentProfileBinding
import com.example.helphero.models.Post
import com.example.helphero.ui.adapters.PostAdapter
import com.example.helphero.ui.viewmodels.PostViewModel
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // ViewModel reference
    private val postViewModel: PostViewModel by activityViewModels()

    // Uri for new selected image
    private var selectedImageUri: Uri? = null

    // Register image picker activity result launcher
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
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

        binding.btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "Signed Out Successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.signInFragment)
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            postViewModel.fetchUserPosts(currentUserId) // Load the current user's posts
        }

        // Observe user posts and display them
        postViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            // Only set the adapter if it hasn't been set already
            if (binding.recyclerViewUserPosts.adapter == null) {
                val adapter = PostAdapter(
                    lifecycleOwner = viewLifecycleOwner,
                    context = requireContext(),
                    onEditClick = { post -> updatePost(post) }, // Changed to update
                    onDeleteClick = { post -> deletePost(post) }
                )
                binding.recyclerViewUserPosts.adapter = adapter
                binding.recyclerViewUserPosts.layoutManager = LinearLayoutManager(requireContext())
            }

            // Update the adapter with the user's posts
            (binding.recyclerViewUserPosts.adapter as PostAdapter).submitList(posts)
        }
    }

    private fun updatePost(post: Post) {
        // Create the AlertDialog to enter new description
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.setText(post.desc) // Pre-fill the description with existing value
        builder.setView(input)
            .setTitle("Update Post Description")
            .setPositiveButton("OK") { dialog, _ ->
                val newDescription = input.text.toString()

                // Show image picker for the new image
                imagePickerLauncher.launch("image/*")

                // Observe the result of the image picker first
                imagePickerLauncher.launch("image/*") // Launch image picker before updating the post
                postViewModel.postSuccessful.observe(viewLifecycleOwner) { isSuccess ->
                    if (isSuccess) {
                        // Once image is picked, update the post with the new description and imageUri
                        postViewModel.updatePost(post.postId, newDescription, selectedImageUri)
                        Toast.makeText(requireContext(), "Post Updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Not a good package", Toast.LENGTH_SHORT).show()
                    }
                }

                // Dismiss the dialog after the image picker interaction
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deletePost(post: Post) {
        // Call ViewModel to delete the post
        postViewModel.deletePost(post.postId)

        // Observe the result of the delete
        postViewModel.postSuccessful.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(requireContext(), "Post Deleted: ${post.postId}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Not a good package", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
