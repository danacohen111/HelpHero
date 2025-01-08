package com.example.app.shared.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.helphero.R

class CommentAdapter(private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    data class Comment(val userPhoto: Int, val text: String)

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val commentUserPhoto: ImageView = itemView.findViewById(R.id.commentUserPhoto)
        val commentText: TextView = itemView.findViewById(R.id.commentText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.commentUserPhoto.setImageResource(comment.userPhoto)
        holder.commentText.text = comment.text
    }

    override fun getItemCount(): Int = comments.size
}
