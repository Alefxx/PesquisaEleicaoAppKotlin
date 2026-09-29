package com.example.appentrevista

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        btenter = findViewById(R.id.btenter)
        etusuario = findViewById(R.id.etuser)
        etsenha = findViewById(R.id.etpassword)




        btenter.setOnClickListener {

            val etusuariosXX = etusuario.text.toString()

            val etsenhaXX = etsenha.text.toString()

            if (etusuariosXX == "admin" && etsenhaXX == "1234" || etusuariosXX == "ent" && etsenhaXX == "123") {
                var telaMain: Intent
                telaMain = Intent(this, MainActivity::class.java)
                startActivity(telaMain)

            } else {
                Toast.makeText(this,"LOGIN OU SENHA INVALIDO", Toast.LENGTH_LONG).show()
            }
        }
    }
}