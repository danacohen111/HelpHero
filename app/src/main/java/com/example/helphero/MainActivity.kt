package com.example.helphero

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.helphero.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Reference the MaterialToolbar from app_bar.xml (via include in activity_main.xml)
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        toolbar.setTitle("");
        getSupportActionBar()?.setDisplayShowTitleEnabled(false);

        // Set up the toolbar with NavController
        NavigationUI.setupWithNavController(toolbar, navController)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_top_navigation, menu)
        Log.d("MainActivity", "inflated !!")
        return true
    }

        override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
            R.id.homeFragment -> {
                navController.navigate(R.id.homeFragment)
                Log.d("MainActivity", "moving to home :)")
                true
            }

            R.id.profileFragment -> {
                navController.navigate(R.id.profileFragment)
                Log.d("MainActivity", "moving to profile :0")
                true
            }

            R.id.addPostFragment -> {
                navController.navigate(R.id.addPostFragment)
                Log.d("MainActivity", "moving to add post ->")
                true
            }

            else -> {
                // The user's action isn't recognized.
                // Invoke the superclass to handle it.
                super.onOptionsItemSelected(item)
            }
        }
}