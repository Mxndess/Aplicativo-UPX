package com.example.mosquitooff

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteConstraintException

class BancoSQLiteHelper(context: Context) : SQLiteOpenHelper(context, "usuarios.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cpf TEXT UNIQUE,
                senha TEXT,
                email TEXT
            )
        """
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    fun inserirUsuario(cpf: String, senha: String, email: String): Boolean {
        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("cpf", cpf)
            put("senha", senha)
            put("email", email)
        }

        return try {
            db.insertOrThrow("usuarios", null, valores)
            true
        } catch (e: SQLiteConstraintException) {
            false
        } finally {
            db.close()
        }
    }

    fun validarLogin(cpf: String, senha: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM usuarios WHERE cpf = ? AND senha = ?",
            arrayOf(cpf, senha)
        )
        val valido = cursor.count > 0
        cursor.close()
        db.close()
        return valido
    }

    fun buscarEmailPorCpf(cpf: String): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT email FROM usuarios WHERE cpf = ?", arrayOf(cpf))
        val email = if (cursor.moveToFirst()) cursor.getString(0) else null
        cursor.close()
        db.close()
        return email
    }
}
