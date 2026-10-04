package com.example.appentrevista

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.os.Looper

class EntradaActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val abrirLogin = Runnable {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_entrada)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


    }

    override fun onStart() {
        super.onStart()
        handler.postDelayed(abrirLogin, 1500)
    }

    override fun onStop() {
        // Evita abrir o login se a abertura já saiu de primeiro plano.
        handler.removeCallbacks(abrirLogin)
        super.onStop()
    }
}
