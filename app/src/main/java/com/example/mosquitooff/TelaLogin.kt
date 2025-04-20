package com.example.mosquitooff

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TelaLogin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_login)

        val campoUsuario = findViewById<EditText>(R.id.usuario)
        val campoSenha = findViewById<EditText>(R.id.senha)
        val botaoEntrar = findViewById<Button>(R.id.botaoEntrar)

        botaoEntrar.setOnClickListener {
            val usuario = campoUsuario.text.toString().trim()
            val senha = campoSenha.text.toString().trim()

            if (usuario == "admin" && senha == "1234") {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Usuário ou senha inválidos!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
