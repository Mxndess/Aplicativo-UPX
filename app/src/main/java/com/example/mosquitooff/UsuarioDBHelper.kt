package com.example.mosquitooff

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UsuarioDBHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "usuarios.db"
        private const val DATABASE_VERSION = 1

        private const val TABELA_USUARIOS = "usuarios"
        private const val COLUNA_ID = "id"
        private const val COLUNA_CPF = "cpf"
        private const val COLUNA_SENHA = "senha"
        private const val COLUNA_EMAIL = "email"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val criarTabela = """
            CREATE TABLE $TABELA_USUARIOS (
                $COLUNA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUNA_CPF TEXT UNIQUE,
                $COLUNA_SENHA TEXT,
                $COLUNA_EMAIL TEXT
            )
        """.trimIndent()
        db.execSQL(criarTabela)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABELA_USUARIOS")
        onCreate(db)
    }

    fun inserirUsuario(cpf: String, senha: String, email: String): Boolean {
        val db = writableDatabase
        val valores = ContentValues().apply {
            put(COLUNA_CPF, cpf)
            put(COLUNA_SENHA, senha)
            put(COLUNA_EMAIL, email)
        }
        return try {
            db.insertOrThrow(TABELA_USUARIOS, null, valores)
            true
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    fun validarLogin(cpf: String, senha: String): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABELA_USUARIOS WHERE $COLUNA_CPF = ? AND $COLUNA_SENHA = ?"
        val cursor = db.rawQuery(query, arrayOf(cpf, senha))
        val valido = cursor.count > 0
        cursor.close()
        db.close()
        return valido
    }

    fun recuperarEmailPorCpf(cpf: String): String? {
        val db = readableDatabase
        val query = "SELECT $COLUNA_EMAIL FROM $TABELA_USUARIOS WHERE $COLUNA_CPF = ?"
        val cursor = db.rawQuery(query, arrayOf(cpf))
        var email: String? = null
        if (cursor.moveToFirst()) {
            email = cursor.getString(cursor.getColumnIndexOrThrow(COLUNA_EMAIL))
        }
        cursor.close()
        db.close()
        return email
    }
}
