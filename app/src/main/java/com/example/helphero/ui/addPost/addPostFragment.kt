package com.example.helphero.ui.addPost

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.helphero.R
import com.example.helphero.databinding.FragmentAddPostBinding
import com.example.helphero.utils.ImageUtil
import com.example.helphero.repositories.PostRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.example.helphero.databases.posts.PostDatabase
import com.example.helphero.ui.viewmodels.PostViewModel

class AddPostFragment : Fragment(R.layout.fragment_add_post) {
    private lateinit var binding: FragmentAddPostBinding
    private lateinit var postViewModel: PostViewModel
    private var imageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageUri = uri
            postViewModel.setImageUri(uri)
            val imageBtn: ImageButton = binding.btnAddImage
            ImageUtil.loadImage(uri, imageBtn)
        } else {
            Toast.makeText(requireContext(), getString(R.string.image_error), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddPostBinding.inflate(inflater, container, false)
        val firestoreDb = FirebaseFirestore.getInstance()
        val database = PostDatabase.getDatabase(requireContext())
        val postDao = database.postDao()
        val repository = PostRepository(firestoreDb, postDao)
        val factory = PostViewModel.PostModelFactory(repository)
        postViewModel = ViewModelProvider(requireActivity(), factory)[PostViewModel::class.java]

        binding.btnAddImage.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnPost.setOnClickListener {
            val title = binding.etPostName.text.toString().trim()
            val desc = binding.etPostDescription.text.toString().trim()

            if (validation(title, desc, imageUri)) {
                postViewModel.savePost(title, desc, imageUri!!)
            }
        }

        postViewModel.postSuccessful.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                postViewModel.resetForm()
                imageUri = null
                binding.btnAddImage.setImageResource(R.drawable.ic_add_image)
                Navigation.findNavController(requireView()).navigate(R.id.homeFragment)
            }
        })

        postViewModel.loading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.uploadProgress.isVisible = isLoading
        })

        return binding.root
    }

    private fun validation(title: String, desc: String, uri: Uri?): Boolean {
        if (uri == null) {
            Toast.makeText(requireContext(), getString(R.string.select_img), Toast.LENGTH_SHORT).show()
            return false
        }

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_title), Toast.LENGTH_SHORT).show()
            return false
        }

        if (desc.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_desc), Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}