package com.example.helphero.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.helphero.databinding.FragmentHomeBinding
import com.example.helphero.databases.posts.PostDatabase
import com.example.helphero.models.Post
import com.example.helphero.repositories.PostRepository
import com.example.helphero.ui.adapters.PostAdapter
import com.example.helphero.ui.viewmodels.PostViewModel
import com.example.helphero.ui.viewmodels.PostViewModel.PostModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var postViewModel: PostViewModel
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firestoreDb = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        val database = PostDatabase.getDatabase(requireContext())
        val postDao = database.postDao()
        val contentResolver = requireContext().contentResolver
        val repository = PostRepository(firestoreDb, firebaseAuth, postDao, contentResolver)
        val factory = PostViewModel.PostModelFactory(repository)
        postViewModel = ViewModelProvider(requireActivity(), factory)[PostViewModel::class.java]

        setupRecyclerView()
        observePosts()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(viewLifecycleOwner, requireContext(), requireContext().contentResolver)
        binding.recyclerViewPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun observePosts() {
        postViewModel.postsLiveData.observe(viewLifecycleOwner, Observer { posts: List<Post> ->
            if (posts.isNotEmpty()) {
                binding.textViewEmptyState.visibility = View.GONE
                postAdapter.submitList(posts)
            } else {
                binding.textViewEmptyState.visibility = View.VISIBLE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}