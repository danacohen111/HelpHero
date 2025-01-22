package com.example.helphero

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.helphero.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var isLoggedIn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = binding.bottomNavigationView
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        supportActionBar?.hide()

        NavigationUI.setupWithNavController(bottomNavigationView, navController)
        bottomNavigationView.inflateMenu(R.menu.menu_bottom_navigation)

        isLoggedin()

        if (isLoggedIn) {
            Log.d(TAG, "user is logged")
            navController.navigate(R.id.homeFragment)
            bottomNavigationView.visibility = BottomNavigationView.VISIBLE
        } else {
            navController.navigate(R.id.signInFragment)
            bottomNavigationView.visibility = BottomNavigationView.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.homeFragment -> {
            navController.navigate(R.id.homeFragment)
            true
        }
        R.id.profileFragment -> {
            navController.navigate(R.id.profileFragment)
            true
        }
        R.id.addPostFragment -> {
            navController.navigate(R.id.addPostFragment)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    fun isLoggedin() {
        isLoggedIn = auth.currentUser != null
    }
}