package com.example.appentrevista

import android.content.Intent
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PesquisaInstrumentedTest {
    @Test
    fun bancoPreservaEntrevistaAoReabrirEAceitaCoordenadaZero() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nomeBanco = "teste_persistencia.db"
        context.deleteDatabase(nomeBanco)
        var banco = BancoEntrevistas(context, nomeBanco)
        try {
            val entrevista = Entrevista(nome = "Ana D'Ávila", celular = "(11) 99999-1234",
                votoEspontaneo = "Não sabe", votoEstimulado = "Branco",
                problemas = listOf("Saúde", "Educação", "Outro"), dataHora = 1_700_000_000_000,
                latitude = 0.0, longitude = 0.0)
            val id = banco.salvar(entrevista)
            banco.close()
            banco = BancoEntrevistas(context, nomeBanco)
            assertEquals(listOf(entrevista.copy(id = id)), banco.listar())
            assertEquals(1L, banco.contar())
            val semLocalizacao = entrevista.copy(nome = "Bruno", latitude = null, longitude = null)
            banco.salvar(semLocalizacao)
            assertEquals(2L, banco.contar())
            assertNull(banco.listar().first().latitude)
            banco.limpar()
            assertEquals(0L, banco.contar())
            assertTrue(banco.listar().isEmpty())
        } finally {
            banco.close()
            // O teste usa um banco próprio; as entrevistas do usuário não são apagadas.
            context.deleteDatabase(nomeBanco)
        }
    }

    @Test
    fun formularioBloqueiaQuartoProblemaEPreservaRascunhoNaRecriacao() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, PesquisaActivity::class.java)
            .putExtra(RegrasPesquisa.EXTRA_PERFIL, RegrasPesquisa.ENTREVISTADOR)
        ActivityScenario.launch<PesquisaActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val continuar = activity.findViewById<MaterialButton>(R.id.continuarEtapa)
                continuar.performClick()
                assertNotNull(activity.findViewById<EditText>(R.id.votoEspontaneo).error)
                activity.findViewById<EditText>(R.id.votoEspontaneo).setText("Jorge Amado")
                continuar.performClick()
                val grupo = activity.findViewById<RadioGroup>(R.id.opcoesVoto)
                (grupo.getChildAt(0) as MaterialRadioButton).performClick()
                continuar.performClick()
                val opcoes = activity.findViewById<LinearLayout>(R.id.opcoesProblemas)
                repeat(4) { (opcoes.getChildAt(it) as MaterialCheckBox).isChecked = true }
                assertFalse((opcoes.getChildAt(3) as MaterialCheckBox).isChecked)
                continuar.performClick()
                activity.findViewById<EditText>(R.id.etname).setText("Ana Maria")
                activity.findViewById<EditText>(R.id.celular).setText("11999991234")
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals("Ana Maria", activity.findViewById<EditText>(R.id.etname).text.toString())
                assertEquals("11999991234", activity.findViewById<EditText>(R.id.celular).text.toString())
                activity.findViewById<MaterialButton>(R.id.voltarEtapa).performClick()
                assertTrue(activity.findViewById<TextView>(R.id.contadorProblemas).text.contains("3/3"))
                activity.findViewById<MaterialButton>(R.id.voltarEtapa).performClick()
                val grupo = activity.findViewById<RadioGroup>(R.id.opcoesVoto)
                assertTrue((grupo.getChildAt(0) as MaterialRadioButton).isChecked)
                activity.findViewById<MaterialButton>(R.id.voltarEtapa).performClick()
                assertEquals("Jorge Amado", activity.findViewById<EditText>(R.id.votoEspontaneo).text.toString())
            }
        }
    }
}
