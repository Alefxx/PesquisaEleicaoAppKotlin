# Guia de apresentação — Pesquisa Eleitoral

O projeto existente foi continuado em Kotlin, com Activities e layouts XML. As telas prontas eram a abertura e o login; a MainActivity tinha somente o início de um formulário. Não havia persistência ou modelo de entrevista.

Foram mantidos o pacote com.example.appentrevista, Gradle 9.5.0, Android Gradle Plugin 9.3.3, compileSdk/targetSdk 37 e minSdk 24 (Android 7). O Kotlin usa o suporte integrado do plugin Android: não havia uma versão independente de plugin Kotlin declarada. Material, AppCompat, Activity, Core e ConstraintLayout já estavam no projeto. Nenhuma dependência foi adicionada.

## 1. Estrutura geral e classes

São quatro Activities, sem Fragments ou Compose. As classes estão em app/src/main/java/com/example/appentrevista.

| Arquivo ou objeto | Responsabilidade |
| --- | --- |
| EntradaActivity.kt | Exibe a imagem de abertura e abre o login após 1,5 segundo em primeiro plano. |
| LoginActivity.kt | Valida campos, confere credenciais e envia o perfil ao menu. |
| MainActivity.kt | Menu por perfil, resultados, entrevistados, detalhes, filtro, limpeza e saída. |
| PesquisaActivity.kt | Cinco etapas, validações, preservação das respostas e localização. |
| Entrevista.kt | Modelo dos dados; formatação de data, hora e coordenadas. |
| RegrasPesquisa, em Entrevista.kt | Contas, candidatos, problemas, limite, telefone, percentual e filtro. |
| BancoEntrevistas.kt | Criação, gravação, consulta, contagem e limpeza no SQLite nativo. |

Os layouts ficam em app/src/main/res/layout. Cores, textos e estilos ficam em res/values e res/values-night. A aparência utiliza os componentes Material existentes e suporta modo claro e escuro.

A imagem original da abertura foi preservada e movida para drawable-nodpi, evitando redimensionamento desnecessário por densidade.

## 2. Fluxo completo

**Abertura → Login → Menu → Iniciar pesquisa → Espontânea → Estimulada → Problemas → Dados → Confirmação → Salvar.**

Após salvar, é possível começar outra pesquisa ou voltar ao menu.

As etapas são seções de um único layout. mostrarEtapa() exibe a seção atual e atualiza o indicador e a barra de progresso. Não há uma Activity para cada pergunta.

Voltar e o retorno do Android levam à etapa anterior. Na primeira etapa, cancelar exige confirmação para descartar o rascunho. Os campos e as variáveis continuam disponíveis durante a navegação.

onSaveInstanceState() também guarda as respostas, a etapa e as coordenadas em um Bundle. restaurarRascunho() recupera esses valores quando o Android recria a Activity, por exemplo, ao girar o aparelho.

O rascunho não é uma entrevista salva. Somente a finalização grava no banco; descartar ou fechar definitivamente uma pesquisa incompleta não cria registro.

## 3. Onde os dados são armazenados

O banco pesquisa_eleitoral.db fica na pasta privada de bancos do aplicativo, normalmente /data/user/0/com.example.appentrevista/databases/pesquisa_eleitoral.db.

A tabela entrevistas tem id, nome, celular, voto espontâneo, voto estimulado, problemas, data/hora, latitude e longitude.

O celular é texto, preservando sua formatação. Os problemas são uma lista JSON em uma coluna de texto, sem tabelas adicionais para este exercício.

SQLiteOpenHelper cria o banco no primeiro acesso. ContentValues envia os pares coluna/valor à inserção sem concatenar respostas em SQL. Os cursores são fechados com use.

As entrevistas salvas permanecem depois de fechar e abrir o app. Não há servidor, Firebase ou API. O banco está na versão 1; mudanças futuras no esquema exigirão uma migração em onUpgrade() que preserve os registros.

## 4. Como funciona o login

| Perfil | Usuário | Senha |
| --- | --- | --- |
| Administrador | admin | admin |
| Entrevistador | entrevistador | entrevistador |

entrar() valida os campos obrigatórios e chama RegrasPesquisa.autenticar(). O perfil segue para o menu por um Intent.

O entrevistador pode pesquisar e sair. O administrador também pode pesquisar e tem acesso aos resultados, entrevistados e limpeza.

Além de esconder opções administrativas, o código confere o perfil antes de abrir consultas ou apagar registros. As Activities internas não são exportadas. Ao sair, a pilha de telas é limpa e o login é aberto novamente.

As contas são fixas e didáticas, sem cadastro ou autenticação online.

## 5. Como os votos são registrados

O voto espontâneo é texto livre obrigatório, preenchido antes de mostrar a lista. Pode conter nome, branco, nulo ou não sabe.

O voto estimulado usa um RadioGroup: cinco candidatos didáticos, Branco, Nulo e Não sabe. O grupo permite uma seleção por vez, e a validação impede avançar sem responder.

Os votos são salvos em campos separados para permitir comparar as duas respostas.

Os cinco nomes ficam em RegrasPesquisa.candidatos. Foram usados Jorge Amado e Candidatos 2 a 5, seguindo a referência do protótipo.

## 6. Limite de problemas

São dez opções. O formulário aceita **de um a três problemas**, respeitando o requisito de no máximo três.

problemasSelecionados é um conjunto, que impede repetições. Cada checkbox atualiza esse conjunto. Antes de adicionar uma escolha, o código confere a quantidade: a quarta opção é desmarcada e um Toast explica como escolher outra.

O contador informa a quantidade e mostra Limite atingido. Desmarcar uma opção libera outra. O banco também valida essa lista antes de salvar.

## 7. Data e hora automáticas

criarEntrevista() usa System.currentTimeMillis(). Na gravação, o valor registra o instante em milissegundos desde 1970.

dataFormatada() converte o número para dd/MM/yyyy às HH:mm:ss, no fuso do aparelho. A data e a hora definitivas correspondem ao salvamento, sem digitação pelo entrevistador.

## 8. Localização e permissões

Obter localização solicita ACCESS_FINE_LOCATION e ACCESS_COARSE_LOCATION juntas. A permissão aproximada também é aceita.

obterLocalizacao() usa LocationManager, sem Google Maps ou biblioteca adicional. Consulta provedores de rede e GPS habilitados, respeitando a permissão. Usa a última posição apenas se tiver até dois minutos; caso contrário, espera uma atualização por até vinte segundos.

LocationListener recebe latitude e longitude. A solicitação é encerrada quando há resposta, mudança de etapa ou saída da tela, evitando acompanhamento contínuo.

Recusa de permissão, GPS desligado, ausência de provedor e tempo esgotado mostram mensagens. Sem coordenadas, o app confirma se o usuário deseja salvar sem localização. Os campos ficam NULL e os detalhes exibem Localização não disponível. Zero é uma coordenada válida, diferente de ausência.

Referências da implementação: [permissões de localização](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime) e [LocationManager](https://developer.android.com/reference/android/location/LocationManager).

## 9. Cálculo dos resultados

mostrarResultados() lê o banco e mostra:

- Total de entrevistados.
- Contagem dos cinco candidatos, Branco, Nulo e Não sabe.
- Percentual e barra de cada opção estimulada.
- Respostas espontâneas agrupadas, ignorando maiúsculas e minúsculas.
- Contagem e percentual das menções dos dez problemas.

A fórmula é **quantidade × 100,0 ÷ total**. O total inclui todas as entrevistas, inclusive branco, nulo e não sabe. Com banco vazio, o percentual é zero, evitando divisão por zero.

Exemplo: um voto em quatro entrevistas representa 25%.

Cada problema também utiliza o total de entrevistados como denominador. Como uma pessoa pode apontar três problemas, a soma desses percentuais pode superar 100%.

As barras são ProgressBar nativas, sem biblioteca de gráficos.

## 10. Lista e detalhes

atualizarLista() usa ListView com ArrayAdapter, componentes nativos que reutilizam linhas durante a rolagem. O projeto não tinha uma solução de lista; essa opção mantém o exercício simples.

As entrevistas aparecem da mais recente para a mais antiga. Cada linha mostra nome, celular, voto estimulado e data/hora. Ao tocar, um diálogo apresenta todos os campos, incluindo voto espontâneo, problemas e coordenadas.

O banco é consultado ao abrir a tela e retornar ao app. O menu também atualiza o total após uma nova entrevista.

## 11. Limpeza dos dados

Somente o administrador vê e executa Limpar dados. confirmarLimpeza() mostra um diálogo com Cancelar e Apagar dados.

banco.limpar() é chamado apenas após confirmar. Depois, os registros em memória são esvaziados, a busca é limpa e o menu consulta o novo total. Ao abrir resultados ou entrevistados, a tela reflete o banco vazio.

Falhas de gravação, consulta ou limpeza mostram mensagens. Na gravação, as respostas continuam na tela para uma nova tentativa. O botão Salvar fica desabilitado durante a operação, e entrevistaSalva impede duplicação por cliques repetidos.

## 12. Plus: filtro por nome ou celular

O campo reage a cada alteração por doAfterTextChanged. RegrasPesquisa.filtrar() procura partes do nome sem distinguir maiúsculas/minúsculas, ou partes do celular ignorando espaços, parênteses e traços.

Ana encontra Ana Maria; 999991234 encontra (11) 99999-1234. Busca vazia mostra todos. Sem correspondências, aparece uma mensagem. O filtro não altera o banco.

## 13. O que estudar para apresentar

1. LoginActivity.entrar() e RegrasPesquisa.autenticar(): campos, credenciais e perfil.
2. PesquisaActivity.mostrarEtapa() e validarEtapa(): navegação e validações.
3. criarOpcoesProblemas(): conjunto, callbacks e limite.
4. criarEntrevista() e salvarEntrevista(): criação do registro e gravação.
5. BancoEntrevistas.onCreate(), salvar() e listar(): SQL, ContentValues e Cursor.
6. solicitarLocalizacao(), obterLocalizacao() e pararLocalizacao(): permissão, resposta assíncrona e ciclo de vida.
7. MainActivity.mostrarResultados() e RegrasPesquisa.percentual(): contagem e cálculo.
8. atualizarLista(), filtrar() e confirmarLimpeza(): consulta, plus e confirmação.
9. onSaveInstanceState() e restaurarRascunho(): preservação do formulário.

Uma explicação inicial possível: “Mantive as Activities e o XML do projeto. O entrevistador registra duas intenções de voto, até três problemas e os dados da pessoa. Na finalização, salvo a entrevista em SQLite local. O administrador consulta percentuais, busca entrevistados e pode apagar os dados após confirmar.”

## 14. Perguntas prováveis do professor

| Pergunta | Resposta curta |
| --- | --- |
| Por que SQLite? | É nativo, persistente e suficiente para um app local sem servidor. |
| O que é uma Activity? | Uma tela/controlador Android com ciclo de vida. |
| Por que as etapas usam a mesma Activity? | O formulário é pequeno; alternar seções simplifica a navegação. |
| Para que serve Intent? | Para abrir uma Activity e enviar o perfil do usuário. |
| Como impede dois votos estimulados? | Com um RadioGroup. |
| Como bloqueia o quarto problema? | Verifico o tamanho do conjunto e desmarco a nova opção. |
| Como calcula o percentual? | Contagem dividida pelo total, vezes 100; para total zero retorno zero. |
| Branco e nulo entram no total? | Sim, os percentuais representam todas as entrevistas. |
| Por que problemas podem somar mais de 100%? | Cada pessoa pode selecionar até três problemas. |
| O que acontece ao negar localização? | Mostro uma mensagem e permito salvar sem coordenadas após confirmar. |
| Por que coordenadas são Double nullable? | Têm casas decimais; null representa posição indisponível. |
| Como evita localização antiga? | Aceito a última posição somente até dois minutos; senão peço uma nova. |
| Como conserva respostas ao voltar? | Uso as mesmas Views e variáveis; Bundle cobre recriações. |
| O que é ContentValues? | Um conjunto de pares coluna/valor para gravar no SQLite. |
| Como guarda os problemas? | Como uma lista JSON em uma coluna de texto. |
| Como funciona o plus? | Filtro a lista por parte do nome ou pelos dígitos do telefone. |
| Para que serve o adapter? | Para transformar cada Entrevista em uma linha reutilizável. |
| Precisa de internet? | Banco e login são locais; a disponibilidade da localização depende do aparelho. |
| E se houver milhares de entrevistas? | Seria adequado fazer I/O em outra thread e paginar a consulta. Esta versão atende à amostra acadêmica. |
| Como evita apagar por engano? | Exijo confirmação antes de chamar delete. |
| Onde estão as senhas? | As contas didáticas estão fixas no código, sem servidor. |

## 15. Validação e roteiro no aparelho

Verificado neste ambiente:

- APK debug gerado por :app:assembleDebug.
- Cinco testes unitários aprovados por :app:testDebugUnitTest.
- :app:lintDebug aprovado, sem erros.
- APK de testes instrumentados compilado por :app:assembleDebugAndroidTest.
- git diff --check sem erros de whitespace.

Restaram três avisos do Lint: novas versões de AGP/Core disponíveis, mantidas conforme o pedido, e pesos de layout aninhados na lista. Os erros do Lint não foram suprimidos.

Não havia aparelho conectado nem imagem de emulador instalada. Os dois testes instrumentados foram **compilados, mas não executados**. A aparência em execução, as permissões e o GPS precisam de conferência em dispositivo.

Os testes estão em:

- app/src/test/java/com/example/appentrevista/RegrasPesquisaTest.kt: login, limite, percentual, filtro e celular.
- app/src/androidTest/java/com/example/appentrevista/PesquisaInstrumentedTest.kt: persistência ao reabrir, limpeza, coordenadas zero/null e preservação do formulário ao recriar a Activity. A persistência usa banco de teste separado.

No Android Studio, selecione um dispositivo e use Run. Com aparelho conectado, execute os testes instrumentados pelo Gradle usando a tarefa :app:connectedDebugAndroidTest.

Roteiro manual, ainda pendente em aparelho:

- [ ] Testar login vazio e credenciais erradas.
- [ ] Entrar como entrevistador; conferir que consultas e limpeza não aparecem.
- [ ] Preencher a espontânea, avançar e voltar; conferir a resposta.
- [ ] Trocar o candidato e verificar uma única seleção.
- [ ] Escolher três problemas, tentar o quarto, desmarcar um e escolher outro.
- [ ] Testar nome/celular vazios e telefone sem DDD.
- [ ] Girar o aparelho nas etapas e conferir a preservação.
- [ ] Obter localização precisa e aproximada; verificar coordenadas nos detalhes.
- [ ] Recusar a permissão e confirmar gravação sem localização.
- [ ] Desligar a localização ou testar sem sinal; conferir mensagem e continuação.
- [ ] Revisar e salvar; começar outra pesquisa e conferir campos vazios.
- [ ] Fechar e reabrir; entrar como administrador e conferir os registros.
- [ ] Verificar percentuais com entrevistas de votos diferentes.
- [ ] Buscar nome e celular, com e sem pontuação; abrir os detalhes.
- [ ] Cancelar a limpeza e conferir que os dados permanecem.
- [ ] Confirmar a limpeza; verificar total zero, lista vazia e percentuais zero.

O APK está em app/build/outputs/apk/debug/app-debug.apk.

