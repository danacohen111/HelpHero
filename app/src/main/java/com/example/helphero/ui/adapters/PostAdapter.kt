package com.example.helphero.ui.adapters

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.helphero.R
import com.example.helphero.databinding.ItemPostBinding
import com.example.helphero.databases.users.UserDatabase
import com.example.helphero.models.Post
import com.example.helphero.models.User
import com.example.helphero.repositories.UserRepository
import com.example.helphero.ui.viewmodels.UserViewModel
import com.example.helphero.ui.viewmodels.UserViewModelFactory
import com.example.helphero.utils.ImageUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        fun bind(post: Post) {
            userViewModel.getUserById(post.userId)
            userViewModel.user.observe(lifecycleOwner) { user: User ->
                binding.textViewUsername.text = user.name
                binding.textViewPhoneNumber.text = user.phone
                if (user.photoUrl.isNotEmpty()) {
                    ImageUtil.loadImage(Uri.parse(user.photoUrl), binding.imageViewProfile,R.drawable.ic_profile_placeholder)
                } else {
                    binding.imageViewProfile.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }
            binding.textViewPostTime.text = post.date
            binding.textViewPostCaption.text = post.desc
            if (post.imageUrl.isNotEmpty()) {
                ImageUtil.loadImage(Uri.parse(post.imageUrl), binding.imageViewPostImage,R.drawable.ic_post_placeholder)
            } else {
                binding.imageViewPostImage.setImageResource(R.drawable.ic_post_placeholder)
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