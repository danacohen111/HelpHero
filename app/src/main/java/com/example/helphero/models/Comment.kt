package com.example.helphero.models

data class Comment(
    val commentId: String,
    val userId: String, // Reference to the user who made the comment
    val text: String
)