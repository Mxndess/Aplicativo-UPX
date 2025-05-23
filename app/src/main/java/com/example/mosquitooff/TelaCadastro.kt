package com.example.mosquitooff

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class TelaCadastro : AppCompatActivity() {

    private lateinit var dbHelper: BancoSQLiteHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_cadastro)

        dbHelper = BancoSQLiteHelper(this)

        val campoCpf = findViewById<EditText>(R.id.campoCpf)
        val campoSenha = findViewById<EditText>(R.id.campoSenhaCadastro)
        val campoEmail = findViewById<EditText>(R.id.campoEmailCadastro)
        val botaoCriarConta = findViewById<Button>(R.id.botaoCriarConta)

        botaoCriarConta.setOnClickListener {
            val cpf = campoCpf.text.toString().trim()
            val senha = campoSenha.text.toString().trim()
            val email = campoEmail.text.toString().trim()

            if (cpf.length != 11 || senha.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos corretamente.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sucesso = dbHelper.inserirUsuario(cpf, senha, email)

            if (sucesso) {
                Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, TelaLogin::class.java))
                finish()
            } else {
                Toast.makeText(this, "Erro: CPF já cadastrado.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
