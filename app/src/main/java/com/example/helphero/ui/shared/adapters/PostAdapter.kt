package com.example.helphero.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.helphero.R
import com.example.helphero.databinding.ItemPostBinding
import com.example.herohelp.models.Post

class PostAdapter : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    // Sample data for now (replace with dynamic data later from Firebase or your database)
    private val posts = listOf(
        Post(
            user = User("John Doe", "123-456-7890", "john_doe.png"),
            postImage = "post_image_1.png",
            commentCount = 5
        ),
        Post(
            user = User("Jane Smith", "987-654-3210", "jane_smith.png"),
            postImage = "post_image_2.png",
            commentCount = 3
        )
    )

    // ViewHolder class for each item (Post)
    inner class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            // Set user details (user's photo, name, phone)
            // Replace with dynamic image loading (Picasso, Glide, etc. in the future)
            binding.userName.text = post.user.name
            binding.userPhone.text = post.user.phone
            binding.userPhoto.setImageResource(R.drawable.default_user)  // Placeholder image

            // Set post image (replace with dynamic image loading later)
            binding.postImage.setImageResource(R.drawable.default_post_image)  // Placeholder image

            // Display comment count or any other data relevant to the post
            binding.commentsHeader.text = "I want to help heroes..."  // Example post text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int {
        return posts.size
    }
}
