package com.tribute.app.pelajarmemantau

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.os.Handler


class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val handler = Handler()
        handler.postDelayed(Runnable {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }, 3000L) //3000 L = 3 detik
    }
}
