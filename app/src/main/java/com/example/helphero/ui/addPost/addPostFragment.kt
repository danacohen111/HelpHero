package com.example.helphero.ui.addPost

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.example.helphero.databases.posts.PostDatabase
import com.example.helphero.databinding.FragmentAddPostBinding
import com.example.helphero.models.NominatimResponse
import com.example.helphero.network.RetrofitInstance
import com.example.helphero.repositories.PostRepository
import com.example.helphero.ui.viewmodels.PostViewModelFactory
import com.example.helphero.ui.viewmodels.PostViewModel
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddPostFragment : Fragment(R.layout.fragment_add_post) {
    private lateinit var binding: FragmentAddPostBinding
    private lateinit var postViewModel: PostViewModel
    private var imageUri: Uri? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                imageUri = uri
                postViewModel.setImageUri(uri)
                val imageBtn: ImageButton = binding.btnAddImage
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
        binding = FragmentAddPostBinding.inflate(inflater, container, false)
        val firestoreDb = FirebaseFirestore.getInstance()
        val database = PostDatabase.getDatabase(requireContext())
        val postDao = database.postDao()
        val repository = PostRepository(firestoreDb, postDao)
        postViewModel = ViewModelProvider(
            requireActivity(),
            PostViewModelFactory(repository)
        )[PostViewModel::class.java]

        binding.btnAddImage.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.buttonSelectLocation.setOnClickListener {
            val input = EditText(requireContext())
            input.hint = "Enter location"

            AlertDialog.Builder(requireContext())
                .setTitle("Select Location")
                .setView(input)
                .setPositiveButton("Search") { _, _ ->
                    val query = input.text.toString()
                    searchLocation(query)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnPost.setOnClickListener {
            val title = binding.etPostName.text.toString().trim()
            val desc = binding.etPostDescription.text.toString().trim()
            val location = binding.tvSelectedLocation.text.toString().trim()

            if (validation(title, desc, imageUri, location)) {
                postViewModel.savePost(title, desc, imageUri!!, location) // Pass location here
            }
        }

        postViewModel.postSuccessful.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                postViewModel.resetForm()
                imageUri = null
                binding.etPostName.text.clear()
                binding.etPostDescription.text.clear()
                binding.btnAddImage.setImageResource(R.drawable.ic_add_image)
                Navigation.findNavController(requireView()).navigate(R.id.action_addPostFragment_to_homeFragment)
            }
        })

        postViewModel.loading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.uploadProgress.isVisible = isLoading
        })

        return binding.root
    }

    private fun searchLocation(query: String) {
        RetrofitInstance.api.searchLocation(query)
            .enqueue(object : Callback<List<NominatimResponse>> {
                override fun onResponse(
                    call: Call<List<NominatimResponse>>,
                    response: Response<List<NominatimResponse>>
                ) {
                    val locations = response.body()
                    if (!locations.isNullOrEmpty()) {
                        val locationNames = locations.map { it.displayName }.toTypedArray()

                        AlertDialog.Builder(requireContext())
                            .setTitle("Select Location")
                            .setItems(locationNames) { _, which ->
                                binding.tvSelectedLocation.text = locationNames[which]
                            }
                            .show()
                    } else {
                        Toast.makeText(requireContext(), "No results found", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onFailure(call: Call<List<NominatimResponse>>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error fetching locations", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun validation(title: String, desc: String, uri: Uri?, location: String): Boolean {
        if (uri == null) {
            Toast.makeText(requireContext(), getString(R.string.select_img), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_title), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (desc.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_desc), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (location.equals("Location")) {
            Toast.makeText(requireContext(), getString(R.string.enter_location), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        return true
    }
}