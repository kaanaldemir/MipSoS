package com.mipo.mipsos

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply the saved theme mode
        val sharedPrefHelper = SharedPrefHelper(this)
        AppCompatDelegate.setDefaultNightMode(sharedPrefHelper.getThemeMode())

        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // Start MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            // Finish SplashActivity
            finish()
        }, 250) // delay
    }
}
