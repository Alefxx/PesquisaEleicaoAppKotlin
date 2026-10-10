package com.example.appentrevista

import org.junit.Assert.*
import org.junit.Test

class RegrasPesquisaTest {
    @Test
    fun limiteDeProblemasAceitaTresERecusaQuatroOuRepeticoes() {
        assertTrue(RegrasPesquisa.problemasValidos(listOf("Saúde", "Educação", "Emprego")))
        assertFalse(RegrasPesquisa.problemasValidos(listOf("Saúde", "Educação", "Emprego", "Outro")))
        assertFalse(RegrasPesquisa.problemasValidos(listOf("Saúde", "Saúde")))
        assertFalse(RegrasPesquisa.problemasValidos(listOf("Inexistente")))
        assertFalse(RegrasPesquisa.problemasValidos(emptyList()))
    }

    @Test
    fun percentuaisConsideramTodasAsEntrevistasESuportamBancoVazio() {
        assertEquals(0.0, RegrasPesquisa.percentual(0, 0), 0.001)
        assertEquals(25.0, RegrasPesquisa.percentual(1, 4), 0.001)
        assertEquals(100.0, RegrasPesquisa.percentual(4, 4), 0.001)
    }

    @Test
    fun filtroBuscaNomeSemDistinguirCaixaETelefoneSemPontuacao() {
        val ana = Entrevista(nome = "Ana Maria", celular = "(11) 99999-1234", dataHora = 1)
        val bruno = ana.copy(nome = "Bruno", celular = "(11) 98888-5678")
        val entrevistas = listOf(ana, bruno)
        assertEquals(listOf(ana), RegrasPesquisa.filtrar(entrevistas, "  ANA  "))
        assertEquals(listOf(ana), RegrasPesquisa.filtrar(entrevistas, "999991234"))
        assertEquals(listOf(bruno), RegrasPesquisa.filtrar(entrevistas, "98888-5678"))
        assertEquals(entrevistas, RegrasPesquisa.filtrar(entrevistas, " "))
        assertTrue(RegrasPesquisa.filtrar(entrevistas, "Carlos").isEmpty())
        assertTrue(RegrasPesquisa.filtrar(entrevistas, "Pessoa 1234").isEmpty())
    }

    @Test
    fun celularExigeDddEAceitaCodigoDoBrasil() {
        assertTrue(RegrasPesquisa.celularValido("(11) 99999-1234"))
        assertTrue(RegrasPesquisa.celularValido("+55 (11) 99999-1234"))
        assertFalse(RegrasPesquisa.celularValido("99999-1234"))
        assertFalse(RegrasPesquisa.celularValido("telefone 11999991234"))
        assertFalse(RegrasPesquisa.celularValido(""))
    }
}
