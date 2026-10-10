package com.example.appentrevista

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton

class PesquisaActivity : AppCompatActivity(), LocationListener {
    private lateinit var banco: BancoEntrevistas
    private lateinit var gerenciadorLocalizacao: LocationManager
    private var etapa = 0
    private var votoSelecionado: String? = null
    private val problemasSelecionados = linkedSetOf<String>()
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var entrevistaSalva = false
    private var buscandoLocalizacao = false
    private var mensagemLocalizacao = "Localização ainda não obtida."
    private val handler = Handler(Looper.getMainLooper())
    private val tempoLimite = Runnable {
        pararLocalizacao()
        informarLocalizacao("Não foi possível obter a posição. Verifique o sinal e tente novamente, ou continue sem localização.")
    }
    private val idsEtapas = listOf(R.id.etapaEspontanea, R.id.etapaEstimulada,
        R.id.etapaProblemas, R.id.etapaDados, R.id.etapaConfirmacao)
    private val titulosEtapas = listOf("Espontânea", "Estimulada", "Problemas", "Dados", "Confirmação")

    override fun onLocationChanged(location: Location) {
        if (!buscandoLocalizacao) return
        latitude = location.latitude
        longitude = location.longitude
        pararLocalizacao()
        informarLocalizacao(criarEntrevista().localizacaoFormatada())
    }

    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) {
        if (buscandoLocalizacao) informarLocalizacao("Localização desativada. Ative-a nas configurações do aparelho.")
    }

    @Deprecated("Necessário para compatibilidade com Android 7 a 9")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    private val permissaoLocalizacao = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) obterLocalizacao()
        else informarLocalizacao("Permissão recusada. Você pode continuar sem localização ou autorizar o acesso nas configurações do aplicativo.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val perfil = intent.getStringExtra(RegrasPesquisa.EXTRA_PERFIL)
        if (perfil !in listOf(RegrasPesquisa.ADMIN, RegrasPesquisa.ENTREVISTADOR)) {
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
            return
        }

        entrevistaSalva = savedInstanceState?.getBoolean("salva") ?: false
        if (entrevistaSalva) { finish(); return }
        enableEdgeToEdge()
        setContentView(R.layout.activity_pesquisa)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        banco = BancoEntrevistas(this)
        gerenciadorLocalizacao = getSystemService(LOCATION_SERVICE) as LocationManager
        restaurarRascunho(savedInstanceState)
        criarOpcoesVoto()
        criarOpcoesProblemas()
        findViewById<MaterialButton>(R.id.continuarEtapa).setOnClickListener {
            if (etapa == idsEtapas.lastIndex) confirmarGravacao()
            else if (validarEtapa()) { etapa++; mostrarEtapa() }
        }
        findViewById<MaterialButton>(R.id.voltarEtapa).setOnClickListener { voltar() }
        findViewById<MaterialButton>(R.id.obterLocalizacao).setOnClickListener { solicitarLocalizacao() }
        onBackPressedDispatcher.addCallback(this) { voltar() }
        mostrarEtapa()
        informarLocalizacao(mensagemLocalizacao)
    }

    private fun criarOpcoesVoto() {
        val grupo = findViewById<RadioGroup>(R.id.opcoesVoto)
        RegrasPesquisa.votos.forEach { voto ->
            grupo.addView(MaterialRadioButton(this).apply {
                id = View.generateViewId()
                text = voto
                minHeight = (48 * resources.displayMetrics.density).toInt()
                isSaveEnabled = false
                isChecked = voto == votoSelecionado
                layoutParams = RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT)
            })
        }
        grupo.setOnCheckedChangeListener { _, id ->
            votoSelecionado = grupo.findViewById<MaterialRadioButton>(id)?.text?.toString()
        }
    }

    private fun criarOpcoesProblemas() {
        val painel = findViewById<LinearLayout>(R.id.opcoesProblemas)
        RegrasPesquisa.problemas.forEach { problema ->
            painel.addView(MaterialCheckBox(this).apply {
                text = problema
                minHeight = (48 * resources.displayMetrics.density).toInt()
                isSaveEnabled = false
                isChecked = problema in problemasSelecionados
                setOnCheckedChangeListener { botao, marcado ->
                    if (marcado && problemasSelecionados.size >= RegrasPesquisa.LIMITE_PROBLEMAS) {
                        botao.isChecked = false
                        avisar("Limite de três problemas. Desmarque um para escolher outro.")
                    } else if (marcado) problemasSelecionados.add(problema)
                    else problemasSelecionados.remove(problema)
                    atualizarContadorProblemas()
                }
            })
        }
        atualizarContadorProblemas()
    }

    private fun atualizarContadorProblemas() {
        findViewById<TextView>(R.id.contadorProblemas).text = getString(R.string.contador_problemas,
            problemasSelecionados.size, RegrasPesquisa.LIMITE_PROBLEMAS,
            if (problemasSelecionados.size == RegrasPesquisa.LIMITE_PROBLEMAS) getString(R.string.limite_atingido) else "")
    }

    private fun mostrarEtapa() {
        if (etapa != 3) pararLocalizacao()
        idsEtapas.forEachIndexed { indice, id ->
            findViewById<View>(id).visibility = if (indice == etapa) View.VISIBLE else View.GONE
        }
        findViewById<TextView>(R.id.indicadorEtapa).text = getString(R.string.indicador_etapa, etapa + 1, idsEtapas.size, titulosEtapas[etapa])
        findViewById<ProgressBar>(R.id.progressoEtapas).progress = etapa + 1
        findViewById<MaterialButton>(R.id.continuarEtapa).text = if (etapa == 4) "Salvar" else "Continuar"
        findViewById<MaterialButton>(R.id.voltarEtapa).text = if (etapa == 0) "Cancelar" else "Voltar"
        if (etapa == 4) {
            val entrevista = criarEntrevista()
            findViewById<TextView>(R.id.resumoEntrevista).text = getString(R.string.revisao_entrevista,
                entrevista.nome, entrevista.celular, entrevista.localizacaoFormatada())
        }
        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, findViewById(R.id.main)).hide(WindowInsetsCompat.Type.ime())
        val rolagem = findViewById<ScrollView>(R.id.rolagemPesquisa)
        rolagem.post { rolagem.scrollTo(0, 0) }
    }

    private fun validarEtapa(): Boolean = when (etapa) {
        0 -> validarCampo(R.id.votoEspontaneo, "Informe a resposta espontânea")
        1 -> if (votoSelecionado == null) { avisar("Selecione uma opção de voto"); false } else true
        2 -> if (!RegrasPesquisa.problemasValidos(problemasSelecionados.toList())) {
            avisar("Escolha de um a três problemas"); false
        } else true
        3 -> {
            val nomeValido = validarCampo(R.id.etname, "Informe o nome")
            val celularValido = validarCampo(R.id.celular, "Informe o celular")
            val celular = findViewById<EditText>(R.id.celular)
            if (celularValido && !RegrasPesquisa.celularValido(celular.text.toString())) {
                celular.error = "Informe 10 ou 11 dígitos com DDD; o código 55 é opcional"
                celular.requestFocus()
                false
            } else nomeValido && celularValido
        }
        else -> true
    }

    private fun validarCampo(id: Int, mensagem: String): Boolean {
        val campo = findViewById<EditText>(id)
        val vazio = campo.text.toString().isBlank()
        campo.error = if (vazio) mensagem else null
        if (vazio) campo.requestFocus()
        return !vazio
    }

    private fun voltar() {
        if (entrevistaSalva) { finish(); return }
        if (etapa > 0) { etapa--; mostrarEtapa() }
        else MaterialAlertDialogBuilder(this).setTitle("Cancelar entrevista?")
            .setMessage("As respostas desta entrevista ainda não foram salvas.")
            .setNegativeButton("Continuar pesquisa", null)
            .setPositiveButton("Descartar") { _, _ -> finish() }.show()
    }

    private fun criarEntrevista(): Entrevista = Entrevista(
        nome = findViewById<EditText>(R.id.etname).text.toString().trim(),
        celular = findViewById<EditText>(R.id.celular).text.toString().trim(),
        dataHora = System.currentTimeMillis(),
        latitude = latitude, longitude = longitude
    )

    private fun confirmarGravacao() {
        if (entrevistaSalva) return
        if (latitude == null || longitude == null) {
            MaterialAlertDialogBuilder(this).setTitle("Salvar sem localização?")
                .setMessage("A posição geográfica não foi obtida. Você pode voltar para tentar novamente ou salvar com a localização indisponível.")
                .setNegativeButton("Voltar aos dados") { _, _ -> etapa = 3; mostrarEtapa() }
                .setPositiveButton("Salvar sem localização") { _, _ -> salvarEntrevista() }.show()
        } else salvarEntrevista()
    }

    private fun salvarEntrevista() {
        if (entrevistaSalva) return
        val botao = findViewById<MaterialButton>(R.id.continuarEtapa)
        botao.isEnabled = false
        try {
            banco.salvar(criarEntrevista(), findViewById<EditText>(R.id.votoEspontaneo).text.toString().trim(),
                votoSelecionado.orEmpty(), problemasSelecionados.toList())
            entrevistaSalva = true
            MaterialAlertDialogBuilder(this).setTitle("Entrevista salva!")
                .setMessage("Os dados foram registrados neste aparelho.")
                .setCancelable(false)
                .setNegativeButton("Voltar ao menu") { _, _ -> finish() }
                .setPositiveButton("Nova pesquisa") { _, _ ->
                    startActivity(Intent(this, PesquisaActivity::class.java)
                        .putExtra(RegrasPesquisa.EXTRA_PERFIL, intent.getStringExtra(RegrasPesquisa.EXTRA_PERFIL)))
                    finish()
                }.show()
        } catch (erro: SQLiteException) {
            Log.e("PesquisaEleitoral", "Erro ao salvar entrevista", erro)
            avisar("Não foi possível salvar. Suas respostas continuam aqui; tente novamente.")
            botao.isEnabled = true
        }
    }

    private fun temPermissao(permissao: String): Boolean =
        ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED

    private fun solicitarLocalizacao() {
        if (temPermissao(Manifest.permission.ACCESS_FINE_LOCATION) ||
            temPermissao(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            obterLocalizacao()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            MaterialAlertDialogBuilder(this).setTitle("Localização da entrevista")
                .setMessage("A permissão permite salvar a latitude e longitude do aparelho nesta entrevista. O acesso ocorre somente enquanto esta tela está aberta.")
                .setNegativeButton("Agora não", null).setPositiveButton("Permitir") { _, _ -> pedirPermissao() }.show()
        } else pedirPermissao()
    }

    private fun pedirPermissao() {
        permissaoLocalizacao.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @SuppressLint("MissingPermission")
    private fun obterLocalizacao() {
        pararLocalizacao()
        try {
            val preciso = temPermissao(Manifest.permission.ACCESS_FINE_LOCATION)
            if (!preciso && !temPermissao(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                informarLocalizacao("Autorize a localização nas configurações ou continue sem ela.")
                return
            }
            val provedores = gerenciadorLocalizacao.getProviders(true)
                .filter { it == LocationManager.NETWORK_PROVIDER || (it == LocationManager.GPS_PROVIDER && preciso) }
            if (provedores.isEmpty()) {
                informarLocalizacao("Localização desativada ou indisponível. Ative-a nas configurações e tente novamente.")
                return
            }

            val recente = provedores.mapNotNull { gerenciadorLocalizacao.getLastKnownLocation(it) }
                .filter { (SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) in 0..IDADE_MAXIMA_LOCALIZACAO_NS }
                .minByOrNull { it.accuracy }
            buscandoLocalizacao = true
            if (recente != null) {
                onLocationChanged(recente)
            } else {
                informarLocalizacao("Buscando posição do aparelho… aguarde até 20 segundos.")
                findViewById<MaterialButton>(R.id.obterLocalizacao).isEnabled = false
                provedores.forEach {
                    gerenciadorLocalizacao.requestLocationUpdates(it, 1000L, 0f, this, Looper.getMainLooper())
                }
                handler.postDelayed(tempoLimite, TEMPO_LIMITE_LOCALIZACAO_MS)
            }
        } catch (erro: SecurityException) {
            pararLocalizacao()
            informarLocalizacao("O acesso à localização foi negado. Você pode continuar sem ela.")
        } catch (erro: IllegalArgumentException) {
            pararLocalizacao()
            informarLocalizacao("O aparelho não possui um provedor disponível. Você pode continuar sem localização.")
        }
    }

    private fun informarLocalizacao(mensagem: String) {
        mensagemLocalizacao = mensagem
        findViewById<TextView>(R.id.statusLocalizacao).text = mensagem
    }

    private fun pararLocalizacao() {
        handler.removeCallbacks(tempoLimite)
        if (::gerenciadorLocalizacao.isInitialized) {
            try { gerenciadorLocalizacao.removeUpdates(this) }
            catch (_: SecurityException) {  }
        }
        if (buscandoLocalizacao && latitude == null) {
            informarLocalizacao("Busca interrompida. Toque em Obter localização para tentar novamente.")
        }
        buscandoLocalizacao = false
        if (::banco.isInitialized) findViewById<MaterialButton>(R.id.obterLocalizacao).isEnabled = true
    }

    private fun avisar(mensagem: String) = Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()

    private fun restaurarRascunho(state: Bundle?) {
        if (state == null) return
        etapa = state.getInt("etapa").coerceIn(0, idsEtapas.lastIndex)
        votoSelecionado = state.getString("voto")
        problemasSelecionados.addAll(state.getStringArrayList("problemas").orEmpty())
        findViewById<EditText>(R.id.votoEspontaneo).setText(state.getString("espontanea"))
        findViewById<EditText>(R.id.etname).setText(state.getString("nome"))
        findViewById<EditText>(R.id.celular).setText(state.getString("celular"))
        if (state.containsKey("latitude")) latitude = state.getDouble("latitude")
        if (state.containsKey("longitude")) longitude = state.getDouble("longitude")
        mensagemLocalizacao = state.getString("statusLocalizacao") ?: mensagemLocalizacao
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::banco.isInitialized && !entrevistaSalva) {
            outState.putInt("etapa", etapa)
            outState.putString("voto", votoSelecionado)
            outState.putStringArrayList("problemas", ArrayList(problemasSelecionados))
            outState.putString("espontanea", findViewById<EditText>(R.id.votoEspontaneo).text.toString())
            outState.putString("nome", findViewById<EditText>(R.id.etname).text.toString())
            outState.putString("celular", findViewById<EditText>(R.id.celular).text.toString())
            latitude?.let { outState.putDouble("latitude", it) }
            longitude?.let { outState.putDouble("longitude", it) }
            outState.putString("statusLocalizacao", if (buscandoLocalizacao) "Busca interrompida. Toque em Obter localização para tentar novamente." else mensagemLocalizacao)
        }
        outState.putBoolean("salva", entrevistaSalva)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        pararLocalizacao()
        super.onStop()
    }

    override fun onDestroy() {
        if (::banco.isInitialized) banco.close()
        super.onDestroy()
    }

    companion object {
        private const val TEMPO_LIMITE_LOCALIZACAO_MS = 20_000L
        private const val IDADE_MAXIMA_LOCALIZACAO_NS = 120_000_000_000L
    }
}
