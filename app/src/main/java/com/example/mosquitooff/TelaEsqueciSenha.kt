package com.example.mosquitooff

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TelaEsqueciSenha : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_esqueci_senha)

        val campoCpf = findViewById<EditText>(R.id.campoCpfRecuperar)
        val botaoRecuperar = findViewById<Button>(R.id.botaoRecuperarSenha)
        val botaoVoltarLogin = findViewById<Button>(R.id.botaoVoltarLogin)

        botaoRecuperar.setOnClickListener {
            val cpf = campoCpf.text.toString().trim()

            if (cpf.length == 11 && cpf.all { it.isDigit() }) {
                Toast.makeText(
                    this,
                    "Se o CPF estiver cadastrado, enviaremos instruções por e-mail.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } else {
                Toast.makeText(this, "Insira um CPF válido com 11 dígitos.", Toast.LENGTH_SHORT).show()
            }
        }

        botaoVoltarLogin.setOnClickListener {
            val intent = Intent(this, TelaLogin::class.java)
            startActivity(intent)
            finish()
        }
    }
}
