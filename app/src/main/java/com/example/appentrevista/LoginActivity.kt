package com.example.appentrevista

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {
    private lateinit var usuario: EditText
    private lateinit var senha: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        usuario = findViewById(R.id.etuser)
        senha = findViewById(R.id.etpassword)
        findViewById<MaterialButton>(R.id.btenter).setOnClickListener { entrar() }
        senha.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_DONE) { entrar(); true } else false
        }
    }

    private fun entrar() {
        usuario.error = null
        senha.error = null
        val login = usuario.text.toString().trim()
        val password = senha.text.toString()
        when {
            login.isEmpty() -> { usuario.error = "Informe o usuário"; usuario.requestFocus() }
            password.isEmpty() -> { senha.error = "Informe a senha"; senha.requestFocus() }
            else -> {
                // Verifica as credenciais e entrega o perfil para o menu correspondente.
                val perfil = RegrasPesquisa.autenticar(login, password)
                if (perfil == null) {
                    Toast.makeText(this, "Usuário ou senha inválidos", Toast.LENGTH_LONG).show()
                } else {
                    startActivity(Intent(this, MainActivity::class.java).putExtra(RegrasPesquisa.EXTRA_PERFIL, perfil))
                    finish()
                }
            }
        }
    }
}
