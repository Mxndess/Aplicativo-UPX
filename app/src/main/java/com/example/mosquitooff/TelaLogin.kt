package com.example.mosquitooff

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class TelaLogin : AppCompatActivity() {

    private lateinit var dbHelper: BancoSQLiteHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_login)

        dbHelper = BancoSQLiteHelper(this)

        val campoUsuario = findViewById<EditText>(R.id.usuario)
        val campoSenha = findViewById<EditText>(R.id.senha)
        val botaoEntrar = findViewById<Button>(R.id.botaoEntrar)
        val linkEsqueciSenha = findViewById<TextView>(R.id.linkEsqueciSenha)
        val linkRegistrar = findViewById<TextView>(R.id.linkRegistrar)

        botaoEntrar.setOnClickListener {
            val cpf = campoUsuario.text.toString().trim()
            val senha = campoSenha.text.toString().trim()

            if (cpf.length != 11 || senha.isEmpty()) {
                Toast.makeText(this, "Preencha CPF e senha corretamente.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val valido = dbHelper.validarLogin(cpf, senha)

            if (valido) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "CPF ou senha inválidos!", Toast.LENGTH_SHORT).show()
            }
        }

        linkEsqueciSenha.setOnClickListener {
            startActivity(Intent(this, TelaEsqueciSenha::class.java))
        }

        linkRegistrar.setOnClickListener {
            startActivity(Intent(this, TelaCadastro::class.java))
        }
    }
}
