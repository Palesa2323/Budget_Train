package com.example.budgettrain

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettrain.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {
    private val authRepository = FirebaseAuthRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Note: Firebase Auth uses email, but UI might have username field
        // If UI has username, we'll treat it as email for now
        // You may want to update the UI to use email field instead
        val emailField = findViewById<TextInputEditText>(R.id.loginUsername) // Using existing field
        val passwordField = findViewById<TextInputEditText>(R.id.loginPassword)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val toRegisterButton = findViewById<MaterialButton>(R.id.toRegisterButton)

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = authRepository.login(email, password)
                result.onSuccess { user ->
                    Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }.onFailure { exception ->
                    val errorMessage = when {
                        exception.message?.contains("password") == true -> "Invalid password"
                        exception.message?.contains("user") == true -> "User not found"
                        else -> exception.message ?: "Login failed"
                    }
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

        toRegisterButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
