package com.example.helphero

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.helphero.R
import com.example.helphero.data.repositories.PostRepository
import com.example.helphero.data.repositories.UserRepository
import com.example.helphero.databases.users.UserDatabase
import com.example.helphero.databases.posts.PostDatabase
import com.example.helphero.models.FirestoreUser
import com.example.helphero.models.Post
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var userRepository: UserRepository
    private lateinit var postRepository: PostRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        NavigationUI.setupWithNavController(topAppBar, navController)

        // Initialize Firebase
        val firestoreDb = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        val storageRef = FirebaseStorage.getInstance().reference

        // Initialize Repositories
        val userDao = UserDatabase.getDatabase(application).userDao()
        userRepository = UserRepository(firestoreDb, firebaseAuth, userDao)
        val postDao = PostDatabase.getDatabase(application).postDao()
        postRepository = PostRepository(firestoreDb, firebaseAuth, postDao)

        // TODO: Delete when done testing
        // Create a new user
        val newUser = FirestoreUser("testuser@example.com", "password123", "Test User")
        userRepository.createUser(newUser, storageRef.child("profile_images/testuser.jpg")) { error ->
            Log.e("MainActivity", "Error creating user: $error")
        }

        // Observe user creation success
        userRepository.signUpSuccessfull.observe(this, Observer { success ->
            if (success) {
                Log.d("MainActivity", "User created successfully")

                // Insert a new post
                val newPost = Post(
                    postId = "1",
                    userId = firebaseAuth.currentUser?.uid ?: "",
                    imageUrl = "",
                    date = "2023-10-10",
                    comments = mutableListOf()
                )
                postRepository.insertPost(newPost)

                // Observe post insertion success
                postRepository.postSuccessful.observe(this, Observer { postSuccess ->
                    if (postSuccess) {
                        Log.d("MainActivity", "Post inserted successfully")
                    } else {
                        Log.e("MainActivity", "Failed to insert post")
                    }
                })
            } else {
                Log.e("MainActivity", "Failed to create user")
            }
        })

        // Observe post updates
        postRepository.postsLiveData.observe(this, Observer { posts ->
            Log.d("MainActivity", "Post updates received: ${posts.size} posts")
        })
    }
}