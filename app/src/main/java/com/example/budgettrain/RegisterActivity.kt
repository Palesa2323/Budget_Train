package com.example.budgettrain

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LogoutActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        toLoginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
