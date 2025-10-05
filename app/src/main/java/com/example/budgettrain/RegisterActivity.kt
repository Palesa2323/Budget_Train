package com.example.budgettrain

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettrain.data.db.DatabaseProvider
import com.example.budgettrain.data.entity.User
import java.security.MessageDigest
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameField = findViewById<TextInputEditText>(R.id.registerUsername)
        val emailField = findViewById<TextInputEditText>(R.id.registerEmail)
        val passwordField = findViewById<TextInputEditText>(R.id.registerPassword)
        val confirmPasswordField = findViewById<TextInputEditText>(R.id.registerConfirmPassword)
        val registerButton = findViewById<MaterialButton>(R.id.registerButton)
        val toLoginButton = findViewById<MaterialButton>(R.id.toLoginButton)

        registerButton.setOnClickListener {
            val username = usernameField.text.toString()
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val confirm = confirmPasswordField.text.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    try {
                        val db = DatabaseProvider.get(this@RegisterActivity)
                        val existingUser = db.userDao().getByUsername(username)
                        if (existingUser != null) {
                            Toast.makeText(this@RegisterActivity, "Username already exists", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val existingEmail = db.userDao().getByEmail(email)
                        if (existingEmail != null) {
                            Toast.makeText(this@RegisterActivity, "Email already registered", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val passwordHash = sha256(password)
                        val userId = db.userDao().insert(
                            User(
                                username = username,
                                email = email,
                                passwordHash = passwordHash
                            )
                        )

                        if (userId > 0) {
                            Toast.makeText(this@RegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@RegisterActivity, LogoutActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        toLoginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
