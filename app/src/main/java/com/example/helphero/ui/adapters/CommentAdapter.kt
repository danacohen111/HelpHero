package com.example.helphero.ui.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.helphero.R
import com.example.helphero.models.Comment
import com.example.helphero.models.User
import com.example.helphero.ui.viewmodels.UserViewModel
import com.example.helphero.ui.viewmodels.UserViewModelFactory
import com.example.helphero.utils.ImageUtil

class CommentAdapter(
    private var comments: List<Comment>,
    private val lifecycleOwner: LifecycleOwner,
    private val userViewModelFactory: UserViewModelFactory
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.commentTextViewUsername)
        val commentText: TextView = itemView.findViewById(R.id.textViewCommentText)
        val commentTime: TextView = itemView.findViewById(R.id.textViewCommentTime)
        val profileImage: ImageView = itemView.findViewById(R.id.commentImageViewProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        val userViewModel = ViewModelProvider(
            lifecycleOwner as ViewModelStoreOwner,
            userViewModelFactory
        ).get(UserViewModel::class.java)

        userViewModel.getUserById(comment.userId)
        userViewModel.user.observe(lifecycleOwner) { user: User ->
            holder.userName.text = user.name
            if (user.photoUrl.isNotEmpty()) {
                ImageUtil.loadImage(
                    Uri.parse(user.photoUrl),
                    holder.profileImage,
                    R.drawable.ic_profile_placeholder
                )
            } else {
                holder.profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }

        holder.commentText.text = comment.text
        holder.commentTime.text = comment.date
    }

    override fun getItemCount(): Int {
        return comments.size
    }

}