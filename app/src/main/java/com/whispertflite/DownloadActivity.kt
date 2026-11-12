package com.whispertflite

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DownloadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Immediately bypass download and redirect to MainActivity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
