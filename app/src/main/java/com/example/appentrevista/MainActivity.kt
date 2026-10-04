package com.example.appentrevista

import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var banco: BancoEntrevistas
    private lateinit var perfil: String
    private var tela = "menu"
    private var entrevistas = emptyList<Entrevista>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        perfil = intent.getStringExtra(RegrasPesquisa.EXTRA_PERFIL).orEmpty()
        if (perfil !in listOf(RegrasPesquisa.ADMIN, RegrasPesquisa.ENTREVISTADOR)) {
            sair()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        banco = BancoEntrevistas(this)
        tela = savedInstanceState?.getString("tela") ?: "menu"
        findViewById<MaterialButton>(R.id.iniciarPesquisa).setOnClickListener {
            startActivity(Intent(this, PesquisaActivity::class.java).putExtra(RegrasPesquisa.EXTRA_PERFIL, perfil))
        }
        findViewById<MaterialButton>(R.id.abrirResultados).setOnClickListener { mudarTela("resultados") }
        findViewById<MaterialButton>(R.id.abrirEntrevistados).setOnClickListener { mudarTela("entrevistados") }
        findViewById<MaterialButton>(R.id.limparDados).setOnClickListener { confirmarLimpeza() }
        findViewById<MaterialButton>(R.id.sair).setOnClickListener { confirmarSaida() }
        findViewById<MaterialButton>(R.id.voltarMenu).setOnClickListener { mudarTela("menu") }
        findViewById<EditText>(R.id.buscaEntrevistados).doAfterTextChanged {
            if (tela == "entrevistados") atualizarLista()
        }
        onBackPressedDispatcher.addCallback(this) {
            if (tela != "menu") mudarTela("menu") else confirmarSaida()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::banco.isInitialized) mostrarTela()
    }

    private fun mudarTela(destino: String) {
        // Além de esconder os botões, verifica o perfil antes de abrir recursos administrativos.
        if (destino != "menu" && perfil != RegrasPesquisa.ADMIN) return
        tela = destino
        WindowCompat.getInsetsController(window, findViewById(R.id.main)).hide(WindowInsetsCompat.Type.ime())
        mostrarTela()
    }

    private fun mostrarTela() {
        if (perfil != RegrasPesquisa.ADMIN) tela = "menu"
        findViewById<View>(R.id.painelMenu).visibility = if (tela == "menu") View.VISIBLE else View.GONE
        findViewById<View>(R.id.painelResultados).visibility = if (tela == "resultados") View.VISIBLE else View.GONE
        findViewById<View>(R.id.rolagemMenu).visibility = if (tela == "entrevistados") View.GONE else View.VISIBLE
        findViewById<View>(R.id.painelEntrevistados).visibility = if (tela == "entrevistados") View.VISIBLE else View.GONE
        findViewById<View>(R.id.voltarMenu).visibility = if (tela != "menu") View.VISIBLE else View.GONE
        listOf(R.id.abrirResultados, R.id.abrirEntrevistados, R.id.limparDados).forEach {
            findViewById<View>(it).visibility = if (perfil == RegrasPesquisa.ADMIN) View.VISIBLE else View.GONE
        }
        findViewById<TextView>(R.id.tituloMenu).text = when (tela) {
            "resultados" -> "Resultados da pesquisa"
            "entrevistados" -> "Entrevistados"
            else -> if (perfil == RegrasPesquisa.ADMIN) "Administrador" else "Entrevistador"
        }
        try {
            if (tela == "menu") {
                findViewById<TextView>(R.id.resumoMenu).text = getString(R.string.resumo_menu, banco.contar(),
                    getString(if (perfil == RegrasPesquisa.ADMIN) R.string.descricao_admin else R.string.descricao_entrevistador))
            } else {
                entrevistas = banco.listar()
                if (tela == "resultados") mostrarResultados() else atualizarLista()
            }
        } catch (erro: SQLiteException) {
            Log.e("PesquisaEleitoral", "Erro ao consultar entrevistas", erro)
            findViewById<TextView>(R.id.resumoMenu).setText(R.string.erro_consulta)
            Toast.makeText(this, "Não foi possível consultar as entrevistas", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarResultados() {
        val total = entrevistas.size
        findViewById<TextView>(R.id.resumoMenu).text = getString(R.string.resumo_resultados, total,
            getString(if (total == 0) R.string.resultado_vazio else R.string.descricao_resultados))
        val painel = findViewById<LinearLayout>(R.id.painelResultados)
        painel.removeAllViews()
        adicionarTitulo(painel, "Pesquisa estimulada")
        RegrasPesquisa.votos.forEach { voto ->
            adicionarResultado(painel, voto, entrevistas.count { it.votoEstimulado == voto }, total, "votos")
        }
        adicionarTitulo(painel, "Pesquisa espontânea")
        // Agrupa respostas iguais ignorando maiúsculas, sem alterar a resposta original salva.
        entrevistas.groupBy { it.votoEspontaneo.trim().lowercase(Locale.forLanguageTag("pt-BR")) }
            .values.sortedByDescending { it.size }.forEach { grupo ->
                adicionarResultado(painel, grupo.first().votoEspontaneo, grupo.size, total, "respostas")
            }
        if (total == 0) adicionarDescricao(painel, "Nenhuma resposta espontânea registrada.")
        adicionarTitulo(painel, "Problemas apontados")
        adicionarDescricao(painel, "Cada entrevistado escolhe até três problemas. Por isso, a soma dos percentuais pode superar 100%.")
        RegrasPesquisa.problemas.forEach { problema ->
            adicionarResultado(painel, problema, entrevistas.count { problema in it.problemas }, total, "menções")
        }
        findViewById<ScrollView>(R.id.rolagemMenu).scrollTo(0, 0)
    }

    private fun adicionarTitulo(painel: LinearLayout, titulo: String) {
        painel.addView(TextView(this).apply {
            text = titulo
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            setPadding(0, (24 * resources.displayMetrics.density).roundToInt(), 0, 12)
        })
    }

    private fun adicionarDescricao(painel: LinearLayout, descricao: String) {
        painel.addView(TextView(this).apply {
            text = descricao
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        })
    }

    private fun adicionarResultado(painel: LinearLayout, nome: String, quantidade: Int, total: Int, unidade: String) {
        val item = layoutInflater.inflate(R.layout.item_resultado, painel, false)
        val percentual = RegrasPesquisa.percentual(quantidade, total)
        item.findViewById<TextView>(R.id.nomeResultado).text = nome
        item.findViewById<TextView>(R.id.contagemResultado).text =
            String.format(Locale.forLanguageTag("pt-BR"), "%d %s • %.1f%%", quantidade, unidade, percentual)
        item.findViewById<ProgressBar>(R.id.barraResultado).apply {
            progress = percentual.roundToInt()
            contentDescription = "$nome: $quantidade de $total"
        }
        painel.addView(item)
    }

    private fun atualizarLista() {
        val busca = findViewById<EditText>(R.id.buscaEntrevistados).text.toString()
        val filtradas = RegrasPesquisa.filtrar(entrevistas, busca)
        findViewById<TextView>(R.id.resumoMenu).text = getString(R.string.resumo_lista, filtradas.size, entrevistas.size)
        val lista = findViewById<ListView>(R.id.listaEntrevistados)
        // ListView e ArrayAdapter são nativos: reaproveitam as linhas durante a rolagem.
        val adapter = object : ArrayAdapter<Entrevista>(this, android.R.layout.simple_list_item_2, android.R.id.text1, filtradas) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val entrevista = getItem(position) ?: return view
                view.findViewById<TextView>(android.R.id.text1).text = entrevista.nome
                view.findViewById<TextView>(android.R.id.text2).apply {
                    text = getString(R.string.linha_entrevista, entrevista.celular, entrevista.votoEstimulado, entrevista.dataFormatada())
                    isSingleLine = false
                }
                return view
            }
        }
        lista.adapter = adapter
        lista.emptyView = findViewById(R.id.listaVazia)
        findViewById<TextView>(R.id.listaVazia).text =
            if (entrevistas.isEmpty()) "Nenhuma entrevista salva. Inicie uma pesquisa para começar."
            else "Nenhuma entrevista corresponde à busca."
        lista.setOnItemClickListener { _, _, posicao, _ ->
            val entrevista = adapter.getItem(posicao) ?: return@setOnItemClickListener
            MaterialAlertDialogBuilder(this).setTitle("Detalhes da entrevista")
                .setMessage(entrevista.detalhes()).setPositiveButton("Fechar", null).show()
        }
    }

    private fun confirmarLimpeza() {
        if (perfil != RegrasPesquisa.ADMIN) return
        // O banco só é apagado depois da confirmação explícita do administrador.
        MaterialAlertDialogBuilder(this).setTitle("Apagar pesquisas?")
            .setMessage("Tem certeza que deseja apagar todos os dados da pesquisa? Esta ação não pode ser desfeita.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Apagar dados") { _, _ ->
                try {
                    banco.limpar()
                    entrevistas = emptyList()
                    findViewById<EditText>(R.id.buscaEntrevistados).text.clear()
                    mostrarTela()
                    Toast.makeText(this, "Dados apagados", Toast.LENGTH_SHORT).show()
                } catch (erro: SQLiteException) {
                    Log.e("PesquisaEleitoral", "Erro ao apagar dados", erro)
                    Toast.makeText(this, "Não foi possível apagar os dados. Tente novamente.", Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    private fun confirmarSaida() {
        MaterialAlertDialogBuilder(this).setTitle("Sair da conta?")
            .setMessage("As entrevistas salvas continuarão neste aparelho.")
            .setNegativeButton("Cancelar", null).setPositiveButton("Sair") { _, _ -> sair() }.show()
    }

    private fun sair() {
        startActivity(Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("tela", tela)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (::banco.isInitialized) banco.close()
        super.onDestroy()
    }
}
