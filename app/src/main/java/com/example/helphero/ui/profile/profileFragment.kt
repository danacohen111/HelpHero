package com.example.helphero.ui.profile

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
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
import androidx.room.InvalidationTracker
import com.example.helphero.R
import com.example.helphero.databases.comments.CommentDatabase
import com.example.helphero.databinding.FragmentProfileBinding
import com.example.helphero.models.Post
import com.example.helphero.repositories.CommentRepository
import com.example.helphero.ui.adapters.PostAdapter
import com.example.helphero.ui.viewmodels.CommentViewModel
import com.example.helphero.ui.viewmodels.PostViewModel
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

    private val commentRepository: CommentRepository by lazy {
        val firestoreDb = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        val commentDao = CommentDatabase.getDatabase(requireContext()).commentDao()
        CommentRepository(firestoreDb, firebaseAuth, commentDao)
    }

    private val commentViewModel: CommentViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            CommentViewModel.CommentViewModelFactory(commentRepository)
        )[CommentViewModel::class.java]
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

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        postViewModel.fetchUserPosts(currentUserId)

        val adapter = PostAdapter(
            lifecycleOwner = viewLifecycleOwner,
            context = requireContext(),
            onEditClick = { post -> updatePost(post) },
            onDeleteClick = { post -> deletePost(post) }
        )
        binding.recyclerViewUserPosts.adapter = adapter
        binding.recyclerViewUserPosts.layoutManager = LinearLayoutManager(requireContext())

        postViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}