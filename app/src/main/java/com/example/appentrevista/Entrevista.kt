package com.example.appentrevista

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Entrevista(
    val id: Long = 0,
    val nome: String,
    val celular: String,
    val votoEspontaneo: String,
    val votoEstimulado: String,
    val problemas: List<String>,
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
        "Voto espontâneo: $votoEspontaneo\nVoto estimulado: $votoEstimulado\n\n" +
        "Problemas: ${problemas.joinToString(", ")}\n\n" +
        "Data e hora: ${dataFormatada()}\n${localizacaoFormatada()}"
}

// As opções didáticas ficam em um único lugar para o formulário e os resultados.
object RegrasPesquisa {
    const val ADMIN = "admin"
    const val ENTREVISTADOR = "entrevistador"
    const val EXTRA_PERFIL = "perfil"
    const val LIMITE_PROBLEMAS = 3
    val candidatos = listOf("Jorge Amado", "Candidato 2", "Candidato 3", "Candidato 4", "Candidato 5")
    val votos = candidatos + listOf("Branco", "Nulo", "Não sabe")
    val problemas = listOf("Saúde", "Educação", "Transporte", "Segurança/Violência", "Emprego",
        "Habitação", "Saneamento", "Trânsito", "Limpeza urbana", "Outro")

    // Credenciais fixas para o exercício; o perfil determina quais recursos são exibidos.
    fun autenticar(usuario: String, senha: String): String? = when {
        usuario == ADMIN && senha == ADMIN -> ADMIN
        usuario == ENTREVISTADOR && senha == ENTREVISTADOR -> ENTREVISTADOR
        else -> null
    }

    fun podeSelecionarProblema(quantidade: Int): Boolean = quantidade < LIMITE_PROBLEMAS

    fun celularValido(celular: String): Boolean {
        val digitos = celular.filter(Char::isDigit)
        return celular.all { it.isDigit() || it in " +()-" } &&
            (digitos.length in 10..11 || (digitos.startsWith("55") && digitos.length in 12..13))
    }

    fun problemasValidos(selecionados: List<String>): Boolean =
        selecionados.size in 1..LIMITE_PROBLEMAS && selecionados.distinct().size == selecionados.size &&
            selecionados.all { it in problemas }

    // O denominador inclui todas as entrevistas, inclusive branco, nulo e não sabe.
    fun percentual(quantidade: Int, total: Int): Double =
        if (total == 0) 0.0 else quantidade * 100.0 / total

    // O plus aceita partes do nome e ignora a formatação do telefone na busca.
    fun filtrar(entrevistas: List<Entrevista>, busca: String): List<Entrevista> {
        val texto = busca.trim()
        val digitos = texto.filter(Char::isDigit)
        return entrevistas.filter {
            texto.isEmpty() || it.nome.contains(texto, ignoreCase = true) ||
                (digitos.isNotEmpty() && texto.all { caractere -> caractere.isDigit() || caractere in " +()-" } &&
                    it.celular.filter(Char::isDigit).contains(digitos))
        }
    }
}
