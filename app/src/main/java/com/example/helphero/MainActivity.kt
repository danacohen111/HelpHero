package com.example.helphero

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.helphero.R
import com.google.android.material.appbar.MaterialToolbar
import android.view.Menu
import android.view.MenuItem

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        NavigationUI.setupWithNavController(topAppBar, navController)

        topAppBar.setNavigationOnClickListener {
            navController.navigateUp()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_top_navigation, menu)

        for (i in 0 until menu?.size()!!) {
            val item = menu.getItem(i)
            item.isChecked = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.homeFragment -> {
                resetMenuItems(findViewById<MaterialToolbar>(R.id.topAppBar).menu)
                item.isChecked = true
                navController.navigate(R.id.homeFragment)
                return true
            }
            R.id.profileFragment -> {
                resetMenuItems(findViewById<MaterialToolbar>(R.id.topAppBar).menu)
                item.isChecked = true
                navController.navigate(R.id.profileFragment)
                return true
            }
            R.id.addPostFragment -> {
                resetMenuItems(findViewById<MaterialToolbar>(R.id.topAppBar).menu)
                item.isChecked = true
                navController.navigate(R.id.addPostFragment)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun resetMenuItems(menu: Menu?) {
        menu?.let {
            for (i in 0 until it.size()) {
                val item = it.getItem(i)
                item.isChecked = false
            }
        }
    }
}
