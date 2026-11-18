package com.example.control_academico

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<View>(R.id.logoSplash)
        val texto = findViewById<View>(R.id.tvSplash)
        logo.alpha = 0f; logo.scaleX = 0.88f; logo.scaleY = 0.88f
        texto.alpha = 0f

        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(900).start()
        texto.animate().alpha(1f).setStartDelay(350).setDuration(800).start()

        logo.postDelayed({
            if (!isFinishing) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }, 1700)
    }
}
