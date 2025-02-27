package com.example.helphero.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.helphero.databases.posts.PostDatabase
import com.example.helphero.databinding.FragmentHomeBinding
import com.example.helphero.models.Post
import com.example.helphero.repositories.PostRepository
import com.example.helphero.ui.adapters.PostAdapter
import com.example.helphero.ui.viewmodels.PostViewModel
import com.example.helphero.ui.viewmodels.PostViewModelFactory
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
        val database = PostDatabase.getDatabase(requireContext())
        val postDao = database.postDao()
        val repository = PostRepository(firestoreDb, postDao)
        postViewModel = ViewModelProvider(
            requireActivity(),
            PostViewModelFactory(repository)
        )[PostViewModel::class.java]

        setupRecyclerView()
        setupSwipeRefreshLayout()
        observePosts()
        handleWindowInsets()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(viewLifecycleOwner, requireContext(),
            onEditClick = { post -> updatePost(post) },
            onDeleteClick = { post -> deletePost(post) })
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

    private fun updatePost(post: Post) {
        Toast.makeText(requireContext(), "Go to profile to update post", Toast.LENGTH_SHORT).show()
    }

    private fun deletePost(post: Post) {
        Toast.makeText(requireContext(), "Go to profile to delete post", Toast.LENGTH_SHORT).show()
    }

    private fun handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.swipeRefreshLayout) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBarInsets.bottom
            )
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
