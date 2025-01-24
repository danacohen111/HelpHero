package com.example.helphero.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.helphero.databinding.FragmentHomeBinding
import com.example.helphero.databases.posts.PostDatabase
import com.example.helphero.models.Post
import com.example.helphero.repositories.PostRepository
import com.example.helphero.ui.adapters.PostAdapter
import com.example.helphero.ui.viewmodels.PostViewModel
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
        setupSwipeRefreshLayout()
        observePosts()
        handleWindowInsets() // Add WindowInsets handling here
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(viewLifecycleOwner, requireContext(), requireContext().contentResolver)
        binding.recyclerViewPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setupSwipeRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            observePosts()
        }
    }

    private fun observePosts() {
        postViewModel.postsLiveData.observe(viewLifecycleOwner) { posts: List<Post> ->
            binding.swipeRefreshLayout.isRefreshing = false
            if (posts.isNotEmpty()) {
                binding.textViewEmptyState.visibility = View.GONE
                postAdapter.submitList(posts)
                binding.recyclerViewPosts.layoutManager?.scrollToPosition(0)
            } else {
                binding.textViewEmptyState.visibility = View.VISIBLE
            }
        }
    }

    private fun handleWindowInsets() {
        // Adjust SwipeRefreshLayout padding dynamically using WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(binding.swipeRefreshLayout) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBarInsets.bottom // Add bottom padding dynamically
            )
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
