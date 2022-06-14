package com.example.veggystock.splashscreen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.veggystock.MainActivity.MainActivity
import com.example.veggystock.R

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {

    companion object {
        const val TIME_SPLASH_SCREEN = 1200L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        supportActionBar?.hide()
        exitSplashScreen()
    }

    private fun exitSplashScreen() {
        val handler = Handler()
        handler.postDelayed({
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
            this.finish()
        }, TIME_SPLASH_SCREEN)
    }
}