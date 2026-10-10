package com.example.appentrevista

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Entrevista(
    val id: Long = 0,
    val nome: String,
    val celular: String,
    val dataHora: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun dataFormatada(): String = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss", Locale.forLanguageTag("pt-BR"))
        .format(Date(dataHora))

    fun localizacaoFormatada(): String = if (latitude != null && longitude != null) {
        String.format(Locale.forLanguageTag("pt-BR"), "Latitude: %.6f\nLongitude: %.6f", latitude, longitude)
    } else "Localização não disponível"

    fun detalhes(): String = "Nome: $nome\nCelular: $celular\n\n" +
        "Data e hora: ${dataFormatada()}\n${localizacaoFormatada()}"
}
object RegrasPesquisa {
    const val ADMIN = "admin"
    const val ENTREVISTADOR = "entrevistador"
    const val EXTRA_PERFIL = "perfil"
    const val LIMITE_PROBLEMAS = 3
    val candidatos = listOf("Jorge Amado", "Candidato 2", "Candidato 3", "Candidato 4", "Candidato 5")
    val votos = candidatos + listOf("Branco", "Nulo", "Não sabe")
    val problemas = listOf("Saúde", "Educação", "Transporte", "Segurança/Violência", "Emprego",
        "Habitação", "Saneamento", "Trânsito", "Limpeza urbana", "Outro")
    fun celularValido(celular: String): Boolean {
        val digitos = celular.filter(Char::isDigit)
        return celular.all { it.isDigit() || it in " +()-" } &&
            (digitos.length in 10..11 || (digitos.startsWith("55") && digitos.length in 12..13))
    }

    fun problemasValidos(selecionados: List<String>): Boolean =
        selecionados.size in 1..LIMITE_PROBLEMAS && selecionados.distinct().size == selecionados.size &&
            selecionados.all { it in problemas }

    fun percentual(quantidade: Int, total: Int): Double =
        if (total == 0) 0.0 else quantidade * 100.0 / total

    fun filtrar(entrevistas: List<Entrevista>, busca: String): List<Entrevista> {
        val texto = busca.trim()
        val digitos = texto.filter(Char::isDigit)
        val buscaCelular = digitos.isNotEmpty() && texto.all { it.isDigit() || it in " +()-" }
        return entrevistas.filter {
            texto.isEmpty() || it.nome.contains(texto, ignoreCase = true) ||
                (buscaCelular && it.celular.filter(Char::isDigit).contains(digitos))
        }
    }
}
