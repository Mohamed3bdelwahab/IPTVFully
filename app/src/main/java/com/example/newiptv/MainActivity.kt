package com.example.newiptv

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.newiptv.ui.home.HomeScreen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Launch HomeScreen directly
        val intent = Intent(this, HomeScreen::class.java)
        startActivity(intent)
        finish() // Close MainActivity
    }
}