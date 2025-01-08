package com.example.helphero.models

data class User(
    val userId: String,
    val name: String,
    val phone: String,
    val photoUrl: String,
    val email: String,
    val password: String
)