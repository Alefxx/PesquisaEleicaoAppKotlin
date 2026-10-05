package com.example.appentrevista

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

class BancoEntrevistas(context: Context, nomeBanco: String = "pesquisa_eleitoral.db") :
    SQLiteOpenHelper(context, nomeBanco, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE entrevistas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                celular TEXT NOT NULL,
                voto_espontaneo TEXT NOT NULL,
                voto_estimulado TEXT NOT NULL,
                problemas TEXT NOT NULL,
                data_hora INTEGER NOT NULL,
                latitude REAL,
                longitude REAL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    fun salvar(entrevista: Entrevista): Long {
        require(entrevista.nome.isNotBlank() && entrevista.celular.isNotBlank())
        require(entrevista.votoEspontaneo.isNotBlank() && entrevista.votoEstimulado in RegrasPesquisa.votos)
        require(RegrasPesquisa.problemasValidos(entrevista.problemas))
        val valores = ContentValues().apply {
            put("nome", entrevista.nome)
            put("celular", entrevista.celular)
            put("voto_espontaneo", entrevista.votoEspontaneo)
            put("voto_estimulado", entrevista.votoEstimulado)
            put("problemas", JSONArray(entrevista.problemas).toString())
            put("data_hora", entrevista.dataHora)
            put("latitude", entrevista.latitude)
            put("longitude", entrevista.longitude)
        }
        return writableDatabase.insertOrThrow("entrevistas", null, valores)
    }

    fun listar(): List<Entrevista> {
        val entrevistas = mutableListOf<Entrevista>()
        readableDatabase.query("entrevistas", null, null, null, null, null, "data_hora DESC, id DESC").use { cursor ->
            while (cursor.moveToNext()) {
                fun texto(coluna: String) = cursor.getString(cursor.getColumnIndexOrThrow(coluna))
                fun coordenada(coluna: String): Double? {
                    val indice = cursor.getColumnIndexOrThrow(coluna)
                    return if (cursor.isNull(indice)) null else cursor.getDouble(indice)
                }
                val problemasJson = JSONArray(texto("problemas"))
                entrevistas.add(Entrevista(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    nome = texto("nome"), celular = texto("celular"),
                    votoEspontaneo = texto("voto_espontaneo"), votoEstimulado = texto("voto_estimulado"),
                    problemas = List(problemasJson.length()) { problemasJson.getString(it) },
                    dataHora = cursor.getLong(cursor.getColumnIndexOrThrow("data_hora")),
                    latitude = coordenada("latitude"), longitude = coordenada("longitude")
                ))
            }
        }
        return entrevistas
    }

    fun contar(): Long = readableDatabase.rawQuery("SELECT COUNT(*) FROM entrevistas", null).use {
        it.moveToFirst()
        it.getLong(0)
    }

    fun limpar() {
        writableDatabase.delete("entrevistas", null, null)
    }
}
