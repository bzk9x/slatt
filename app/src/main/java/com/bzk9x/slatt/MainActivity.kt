package com.bzk9x.slatt

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        FirebaseApp.initializeApp(this)
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val prefs = getSharedPreferences("Slatt", MODE_PRIVATE)
        val hasCreatedUsername = prefs.getBoolean("hasCreatedUsername", false)

        Handler(Looper.getMainLooper()).postDelayed({
            if (currentUser != null) {
                if (hasCreatedUsername) {
                    finish()
                } else {
                    val createAccountActivity = Intent(this, CreateUsernameActivity::class.java)
                    startActivity(createAccountActivity)
                }
            } else {
                val authGateActivity = Intent(this, AuthGateActivity::class.java)
                startActivity(authGateActivity)
            }
            finish()
        }, 1000)
    }
}