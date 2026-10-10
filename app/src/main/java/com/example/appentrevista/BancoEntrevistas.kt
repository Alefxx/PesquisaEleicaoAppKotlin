package com.example.appentrevista

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import org.json.JSONArray
import java.util.Locale

class BancoEntrevistas(context: Context, nomeBanco: String = "pesquisa_eleitoral.db") :
    SQLiteOpenHelper(context, nomeBanco, null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE entrevistas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL, celular TEXT NOT NULL,
                data_hora INTEGER NOT NULL,
                latitude REAL, longitude REAL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS votos (
                tipo TEXT NOT NULL, chave TEXT NOT NULL, resposta TEXT NOT NULL,
                quantidade INTEGER NOT NULL, PRIMARY KEY (tipo, chave)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("PRAGMA secure_delete = ON")
        db.execSQL("ALTER TABLE entrevistas RENAME TO entrevistas_antigas")
        onCreate(db)
        db.execSQL("""
            INSERT INTO entrevistas (id, nome, celular, data_hora, latitude, longitude)
            SELECT id, nome, celular, data_hora, latitude, longitude FROM entrevistas_antigas
        """)
        db.rawQuery("SELECT * FROM entrevistas_antigas ORDER BY data_hora, id", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (oldVersion == 1) {
                    registrarVoto(db, "espontaneo", cursor.getString(cursor.getColumnIndexOrThrow("voto_espontaneo")))
                    registrarVoto(db, "estimulado", cursor.getString(cursor.getColumnIndexOrThrow("voto_estimulado")))
                }
                val problemas = JSONArray(cursor.getString(cursor.getColumnIndexOrThrow("problemas")))
                for (i in 0 until problemas.length()) registrarVoto(db, "problema", problemas.getString(i))
            }
        }
        db.execSQL("DROP TABLE entrevistas_antigas")
    }

    fun salvar(entrevista: Entrevista, votoEspontaneo: String, votoEstimulado: String, problemas: List<String>): Long {
        require(entrevista.nome.isNotBlank() && entrevista.celular.isNotBlank())
        require(votoEspontaneo.isNotBlank() && votoEstimulado in RegrasPesquisa.votos)
        require(RegrasPesquisa.problemasValidos(problemas))
        val valores = ContentValues().apply {
            put("nome", entrevista.nome)
            put("celular", entrevista.celular)
            put("data_hora", entrevista.dataHora)
            put("latitude", entrevista.latitude)
            put("longitude", entrevista.longitude)
        }
        val db = writableDatabase
        var id = 0L
        db.transaction {
            id = db.insertOrThrow("entrevistas", null, valores)
            registrarVoto(db, "espontaneo", votoEspontaneo)
            registrarVoto(db, "estimulado", votoEstimulado)
            problemas.forEach { registrarVoto(db, "problema", it) }
        }
        return id
    }

    private fun registrarVoto(db: SQLiteDatabase, tipo: String, resposta: String) {
        val chave = resposta.trim().lowercase(Locale.forLanguageTag("pt-BR"))
        db.execSQL("INSERT OR IGNORE INTO votos (tipo, chave, resposta, quantidade) VALUES (?, ?, ?, 0)",
            arrayOf(tipo, chave, resposta))
        db.execSQL("UPDATE votos SET quantidade = quantidade + 1, resposta = ? WHERE tipo = ? AND chave = ?", arrayOf(resposta, tipo, chave))
    }

    fun contarVotos(tipo: String): Map<String, Int> {
        val resultados = linkedMapOf<String, Int>()
        readableDatabase.rawQuery("SELECT resposta, quantidade FROM votos WHERE tipo = ? ORDER BY quantidade DESC, rowid", arrayOf(tipo)).use {
            while (it.moveToNext()) resultados[it.getString(0)] = it.getInt(1)
        }
        return resultados
    }

    fun listar(): List<Entrevista> {
        val entrevistas = mutableListOf<Entrevista>()
        readableDatabase.rawQuery("""
            SELECT id, nome, celular, data_hora, latitude, longitude
            FROM entrevistas ORDER BY data_hora DESC, id DESC
        """, null).use { cursor ->
            while (cursor.moveToNext()) {
                entrevistas.add(Entrevista(
                    id = cursor.getLong(0), nome = cursor.getString(1), celular = cursor.getString(2),
                    dataHora = cursor.getLong(3),
                    latitude = if (cursor.isNull(4)) null else cursor.getDouble(4),
                    longitude = if (cursor.isNull(5)) null else cursor.getDouble(5)
                ))
            }
        }
        return entrevistas
    }

    fun contar(): Long = readableDatabase.rawQuery("SELECT COUNT(*) FROM entrevistas", null).use {
        it.moveToFirst()
        it.getLong(0)
    }

    fun limpar() = writableDatabase.transaction {
        delete("entrevistas", null, null)
        delete("votos", null, null)
    }
}
