package com.example.helphero.ui.adapters

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.helphero.R
import com.example.helphero.databinding.ItemPostBinding
import com.example.helphero.databases.comments.CommentDatabase
import com.example.helphero.databases.users.UserDatabase
import com.example.helphero.models.Comment
import com.example.helphero.models.Post
import com.example.helphero.models.User
import com.example.helphero.repositories.CommentRepository
import com.example.helphero.repositories.UserRepository
import com.example.helphero.ui.viewmodels.CommentViewModel
import com.example.helphero.ui.viewmodels.CommentViewModelFactory
import com.example.helphero.ui.viewmodels.UserViewModel
import com.example.helphero.ui.viewmodels.UserViewModelFactory
import com.example.helphero.utils.ImageUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PostAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val contentResolver: ContentResolver
) : ListAdapter<Post, PostAdapter.PostViewHolder>(DiffCallback()) {

    private val userRepository: UserRepository by lazy {
        val firestoreDb = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        val database = UserDatabase.getDatabase(context)
        val userDao = database.userDao()
        UserRepository(firestoreDb, firebaseAuth, contentResolver, userDao)
    }

    private val userViewModel: UserViewModel by lazy {
        ViewModelProvider(
            lifecycleOwner as ViewModelStoreOwner,
            UserViewModelFactory(userRepository)
        ).get(UserViewModel::class.java)
    }

    private val commentRepository: CommentRepository by lazy {
        val firestoreDb = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        val commentDao = CommentDatabase.getDatabase(context).commentDao()
        CommentRepository(firestoreDb, firebaseAuth, commentDao)
    }

    private val commentViewModel: CommentViewModel by lazy {
        ViewModelProvider(
            lifecycleOwner as ViewModelStoreOwner,
            CommentViewModelFactory(commentRepository)
        ).get(CommentViewModel::class.java)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        private var isCommentsVisible = false // Track visibility of comments section locally

        fun bind(post: Post) {
            // Load user info
            userViewModel.getUserById(post.userId)
            userViewModel.user.observe(lifecycleOwner) { user: User ->
                binding.textViewUsername.text = user.name
                binding.textViewPhoneNumber.text = user.phone
                if (user.photoUrl.isNotEmpty()) {
                    ImageUtil.loadImage(
                        Uri.parse(user.photoUrl),
                        binding.imageViewProfile,
                        R.drawable.ic_profile_placeholder
                    )
                } else {
                    binding.imageViewProfile.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }

            // Load post data
            binding.textViewPostTime.text = post.date
            binding.textViewPostCaption.text = post.desc
            if (post.imageUrl.isNotEmpty()) {
                ImageUtil.loadImage(
                    Uri.parse(post.imageUrl),
                    binding.imageViewPostImage,
                    R.drawable.ic_post_placeholder
                )
            } else {
                binding.imageViewPostImage.setImageResource(R.drawable.ic_post_placeholder)
            }

            // Toggle comments section
            binding.buttonComments.setOnClickListener {
                isCommentsVisible = !isCommentsVisible
                binding.commentsContainer.visibility =
                    if (isCommentsVisible) View.VISIBLE else View.GONE
            }

            // Observe comments LiveData and update RecyclerView
            commentViewModel.commentsLiveData.observe(lifecycleOwner) { comments ->
                val postComments = comments.filter { it.postId == post.postId }.sortedByDescending { it.date }
                val commentsAdapter = CommentAdapter(postComments, lifecycleOwner, UserViewModelFactory(userRepository))
                binding.recyclerViewComments.adapter = commentsAdapter
                binding.recyclerViewComments.layoutManager = LinearLayoutManager(context)
            }

            // Add new comment
            binding.buttonAddComment.setOnClickListener {
                val commentText = binding.editTextAddComment.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    val newComment = Comment(
                        commentId = UUID.randomUUID().toString(),
                        postId = post.postId,
                        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknownUserId",
                        text = commentText,
                        date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(Date())
                    )
                    commentViewModel.addComment(newComment)
                    binding.editTextAddComment.text.clear()
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}