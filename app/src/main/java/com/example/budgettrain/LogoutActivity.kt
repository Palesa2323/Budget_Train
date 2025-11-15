package com.example.budgettrain

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettrain.data.repository.FirebaseAuthRepository
import com.google.android.material.button.MaterialButton

class LogoutActivity : AppCompatActivity() {
    private val authRepository = FirebaseAuthRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logout)

        val logoutBtn = findViewById<MaterialButton>(R.id.logoutButton)
        logoutBtn.setOnClickListener {
            authRepository.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
