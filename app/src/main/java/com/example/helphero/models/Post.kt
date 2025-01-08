package com.example.herohelp.models

data class Post(
    val postId: String,
    val userId: String, // Reference to the user who made the post
    val imageUrl: String,
    val date: String,
    val comments: List<Comment>
)