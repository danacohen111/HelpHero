package com.example.helphero

import android.os.Bundle
import android.view.Menu
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

        bottomNavigationView = binding.toolbar
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        supportActionBar?.hide()

        NavigationUI.setupWithNavController(bottomNavigationView, navController)
        isLoggedin()

        if (isLoggedIn) {
            navController.navigate(R.id.homeFragment)
        } else {
            navController.navigate(R.id.signInFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bottom_navigation, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu?.findItem(R.id.homeFragment)?.isVisible = isLoggedIn
        menu?.findItem(R.id.profileFragment)?.isVisible = isLoggedIn
        menu?.findItem(R.id.addPostFragment)?.isVisible = isLoggedIn
        return true
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
        invalidateOptionsMenu()
    }
}