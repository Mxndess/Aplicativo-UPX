package com.example.mosquitooff

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MarcadorDBHelper(context: Context) : SQLiteOpenHelper(context, "MarcadoresDB", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE marcadores (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "latitude REAL, " +
                    "longitude REAL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS marcadores")
        onCreate(db)
    }

    fun salvarMarcador(latitude: Double, longitude: Double) {
        val db = writableDatabase
        val valores = ContentValues().apply {
            put("latitude", latitude)
            put("longitude", longitude)
        }
        db.insert("marcadores", null, valores)
    }

    fun buscarTodos(): List<Pair<Double, Double>> {
        val lista = mutableListOf<Pair<Double, Double>>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT latitude, longitude FROM marcadores", null)
        while (cursor.moveToNext()) {
            val lat = cursor.getDouble(0)
            val lon = cursor.getDouble(1)
            lista.add(Pair(lat, lon))
        }
        cursor.close()
        return lista
    }

    fun removerMarcador(latitude: Double, longitude: Double) {
        val db = writableDatabase
        db.delete(
            "marcadores",
            "latitude = ? AND longitude = ?",
            arrayOf(latitude.toString(), longitude.toString())
        )
    }
}
