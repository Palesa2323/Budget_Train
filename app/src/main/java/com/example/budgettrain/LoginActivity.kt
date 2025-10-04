package com.example.budgettrain

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameField = findViewById<TextInputEditText>(R.id.loginUsername)
        val passwordField = findViewById<TextInputEditText>(R.id.loginPassword)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val toRegisterButton = findViewById<MaterialButton>(R.id.toRegisterButton)

        // Dummy login (for now)
        loginButton.setOnClickListener {
            val username = usernameField.text.toString()
            val password = passwordField.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LogoutActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
            }
        }

        toRegisterButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
