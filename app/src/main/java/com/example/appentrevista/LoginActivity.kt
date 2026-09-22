package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.jvm.java

class LoginActivity : AppCompatActivity() {

    private lateinit var btenter: Button
    private lateinit var etusuario: EditText
    private lateinit var etsenha: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        btenter = findViewById(R.id.btenter)
        etusuario = findViewById(R.id.etusuario)
        etsenha = findViewById(R.id.etsenha)




        btenter.setOnClickListener {

            val etusuariosXX = etusuario.text.toString()

            val etsenhaXX = etsenha.text.toString()

            if (etusuariosXX == "Alef" && etsenhaXX == "1234") {
                var telaLogin: Intent
                telaLogin = Intent(this, LoginActivity::class.java)
                startActivity(telaLogin)

            } else {
                Toast.makeText(this,"LOGIN OU SENHA INVALIDO", Toast.LENGTH_LONG).show()
            }
        }
    }
}