package com.example.helphero

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var continueButton: Button
    private lateinit var signupButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        emailField = findViewById(R.id.emailField)
        passwordField = findViewById(R.id.passwordField)
        continueButton = findViewById(R.id.continueButton)
        signupButton = findViewById(R.id.signupButton)

        continueButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            // Basic validation (check if fields are empty)
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out both fields", Toast.LENGTH_SHORT).show()
            }
        }

        signupButton.setOnClickListener {
            finish()
        }
    }
}

