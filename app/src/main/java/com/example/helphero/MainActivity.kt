package com.example.helphero

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.helphero.R
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Reference the MaterialToolbar from app_bar.xml (via include in activity_main.xml)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Optional: Handle navigation on the toolbar if required
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }

        // Navigate to homeFragment by default
        if (savedInstanceState == null) {
            navController.navigate(R.id.homeFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu into the toolbar
        menuInflater.inflate(R.menu.menu_top_navigation, menu)

        // Set the homeFragment menu item as checked by default
        menu?.findItem(R.id.homeFragment)?.isChecked = true

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle menu item clicks
        when (item.itemId) {
            R.id.homeFragment -> {
                handleMenuItemState(item)
                navController.navigate(R.id.homeFragment)
                return true
            }
            R.id.profileFragment -> {
                handleMenuItemState(item)
                navController.navigate(R.id.profileFragment)
                return true
            }
            R.id.addPostFragment -> {
                handleMenuItemState(item)
                navController.navigate(R.id.addPostFragment)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Reset all menu items' state to unselected.
     */
    private fun resetMenuItems(menu: Menu?) {
        menu?.let {
            for (i in 0 until it.size()) {
                val item = it.getItem(i)
                item.isChecked = false
            }
        }
    }

    /**
     * Handle the state of the selected menu item.
     */
    private fun handleMenuItemState(selectedItem: MenuItem) {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val menu = toolbar.menu

        // Reset all menu items
        resetMenuItems(menu)

        // Update the selected item's state
        selectedItem.isChecked = true
    }
}