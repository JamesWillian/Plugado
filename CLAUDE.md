# Plugado

Contexto do projeto para o Claude Code, montado a partir da conversa de planejamento (16 e 17/out/2026). Se algo aqui divergir do código, o código vence. Atualize este arquivo quando uma decisão mudar.

## O app

App Android (Kotlin) que mede quanto tempo por dia o usuário ouve áudio pelo fone de ouvido, e com que volume. Conta qualquer áudio (música, vídeo, chamada) de qualquer app. Cada trecho tem início, fim e volume, e a base gera os minutos por dia e o volume médio.

Princípio do produto: o app **não** sabe qual app está tocando nem qual mídia. Só sabe que tem som saindo pelo fone, e com que volume.

## Como trabalhar com o James

- Responda em português (BR), em tom casual.
- Ele é dev Android experiente (Kotlin, Compose, Flow, Room, Koin, MVVM/Clean). Pode ir direto ao ponto em Android.
- Testes automatizados são novidade pra ele. Ao escrever testes, explique a estrutura (monta a situação → executa → confere) e, quando fizer sentido, deixe um caso pra ele escrever.
- Quando ele pedir explicação "em linguagem fácil", use analogias e frases curtas.
- Nas respostas, prefira linguagem comum. Termo de programação pode aparecer, mas explicado de forma clara; jargão de Android ou de áudio, só com explicação (pedido de 20/out).
- Ele não tem como fazer testes de campo elaborados (outros aparelhos, carro, caixa de som). Peça no máximo testes simples, de poucos minutos, e diga quando são opcionais.
- Antes de mudar algo que aparece no aparelho dele (ícone novo, nome do app, tela inicial, notificação nova), pergunte explicitamente. Não deixe esse tipo de decisão escondida no meio de um plano. (Em 20/out, o ícone "Plugado log" foi criado sem essa pergunta; a pedido dele, o log passou pra aba Today.)
- Processo combinado: validar o comportamento no aparelho real antes de abstrair (por isso o spike veio antes da arquitetura).

## Estado atual (20/out/2026)

- [x] Projeto criado: minSdk 31, targetSdk 37, AGP 9.4.1 (Kotlin embutido), Kotlin 2.2.10, Gradle 9.6.0 e JVM 11 no `:app` e no `:domain`.
- [x] Spike descartável `AudioSpike.kt` no `:app`, rodado num aparelho real. Os achados estão mais abaixo. Foi apagado na fase 3 e continua no histórico do git.
- [x] Módulo `:domain` (Kotlin/JVM puro) criado. Packages: `app.jammes.domain.model` e `app.jammes.domain.tracker`. O app usa `app.jammes.plugado`.
- [x] Modelos (um arquivo por classe em `model/`), `SegmentTracker.kt` (com `TrackerConfig` e `TrackerCommand`) e `SegmentTrackerTest.kt`: 11 testes passando no projeto real (`./gradlew :domain:test`, 20/out).
- [x] `audio/` (fase 2): as quatro fontes, as regras puras (`SnapshotRules.kt`), o leitor do `AudioManager` e o `AudioStateAggregator`, com 15 testes na JVM (`./gradlew :app:testDebugUnitTest`). As fontes e o leitor ainda não rodaram num aparelho; isso vem com o serviço.
- [x] `monitor/` (fase 3): `ListeningMonitor` (com 4 testes), `MonitorService` (FGS `specialUse`), `MonitorNotification`, Koin (`di/`) e a tela de log só no debug (hoje na aba Today do BottomNavigation). No emulador (API 37): o FGS sobe como `specialUse` mesmo com a notificação negada, o processo fica em `FOREGROUND_SERVICE`, uma mudança de volume com o app em background vira snapshot ~0,45 s depois, e desligar funciona. Validado pelo James no aparelho real, com música no fone e a tela desligada.
- [x] Testes de campo (fase 4): **pulados por decisão do James** (20/out). Ele não tem como fazer a maioria. O caso principal (música no fone BT) está validado; o resto virou a lista "Ainda não testado", pra observar no uso do dia a dia e corrigir pelo log quando algum número parecer errado.
- [x] `data/` (fase 5): Room (`segments`, `devices`), `ListeningRepository` (é o `CommandSink`), recuperação dos trechos órfãos (regra no domínio, `OrphanRecovery.kt`), DataStore (`MonitorSettings`), `MonitorSwitch`, `BootReceiver` e a notificação "Hoje: X min no fone". Testes: 3 no domínio e 8 instrumentados no emulador (`./gradlew :app:connectedDebugAndroidTest`). No emulador (API 37): a notificação mostra "Hoje: 0 min no fone", reinstalar o app com o monitoramento ligado faz ele voltar sozinho (`MY_PACKAGE_REPLACED`), e o "Pausar" desliga e continua desligado depois de reinstalar. Falta ver o total subir no aparelho real, ouvindo música.
- [x] UI (fase 6, 21/out): Hoje, Histórico (7 e 30 dias), Dispositivos, Ajustes, Privacidade e o primeiro uso, em Compose, em português, inglês e espanhol, com tema Claro/Escuro/Sistema. Conferido no emulador (API 37) com dados de exemplo; os prints estão em `docs/prints/`. Falta o James usar no celular dele.
- [x] Sessão aberta na tela Hoje (27/out): um toque na sessão mostra o gráfico do volume, o pico, o tempo no pico e o fone (veja "Sessão aberta" mais abaixo). Conferido no emulador (API 37) com dados de exemplo, nos dois temas e nos 3 idiomas; o print é o `docs/prints/4-sessao-aberta.png`. Falta ver no celular do James, com sessões de verdade.
- [ ] Endurecimento e publicação na Play Store.

## Próximos passos

1. Exercícios reservados pro James (não escreva por ele, a menos que ele peça):
   - `SegmentTrackerTest`, "pausa curta e volta com outro volume": toca em 0 s, pausa em 60 s, volta em 62 s com outro volume → `Close` em 60 + `Open` em 62, na mesma sessão.
   - `SnapshotRulesTest`, "SCO sozinho não conta pra mídia": só o SCO conectado, música tocando e mode normal → `output` null.
   - `ListeningStatsTest`, "tempo no pico soma os trechos separados": 60% por 10 min, 50% por 5 min e 60% de novo por 15 min → `peakPercent` 60 e `timeAtPeak` 25 min.
2. Investigar o vídeo do Facebook com som, que não contou (veja "Achados do uso"). Precisa de dados crus no log de debug: `isMusicActive`, `mode`, saídas e players ativos em cada gatilho, mesmo quando o snapshot não muda.
3. Endurecimento e Play Store: página da política de privacidade (e preencher `PRIVACY_POLICY_URL`), textos e vídeo da declaração do FGS `specialUse`, Data safety, conferir que o app segue sem a permissão `INTERNET`, testes em outras marcas e no Android 17, troca de fuso (o `zone` do tracker é fixo desde a criação).

Os protótipos aprovados estão no canvas "Protótipos do Plugado" (https://claude.ai/artifact/VzZMj63xRzMnkXpGZ9V69s). As telas em Compose seguem eles; mudança visual nova passa por lá (ou pelo James) antes.

### Interface (decisões de 21/out)

- Inspiração Uber: preto e branco puros, números grandes em negrito, muito espaço e cantos arredondados. Cada tela tem uma ação principal (botão invertido). Única cor: vermelho só pra ação destrutiva (excluir dados).
- Fonte: Inter (licença OFL, dá pra embutir no app), pesos 400 a 700, espaçamento negativo nos tamanhos grandes. Números em coluna usam algarismos tabulares; números grandes, não.
- Tema: Claro, Escuro ou Sistema (escolha nos Ajustes; o padrão é Sistema).
- Barra de baixo com 4 abas: Hoje, Histórico, Dispositivos e Ajustes ("Ajustes" e não "Configurações", porque cabe melhor em 4 abas; em inglês "Settings", em espanhol "Ajustes"). Os rótulos atuais em inglês no `AppDestinations` mudam.
- O app é multi-idioma: português (Brasil), inglês e espanhol, mais "Padrão do sistema". Cada idioma aparece com o nome no próprio idioma (English, Español, Português (Brasil)).
- Ajustes: Tema, Idioma, Monitorar o fone (interruptor), Bateria sem restrições (abre os ajustes do celular), Política de privacidade (tela própria com resumo e link pra política completa) e Excluir todos os dados (confirmação; apaga histórico e dispositivos, mantém as preferências).
- A tela de Privacidade afirma que o app "não usa a internet". Isso só continua verdade se o app seguir sem a permissão `INTERNET`; confira o manifest final antes de publicar.

Como ficou no código (fase 6):
- Volume médio = média ponderada pelo tempo do **% do volume do sistema** (índice ÷ máximo), decidido na construção em 21/out porque a tela mostra porcentagem. A média energética em dB ficou de fora (o dB do sistema não é o volume no ouvido). As contas ficam no `:domain/stats` (`ListeningStats.kt`, com testes).
- Média por dia: dias sem escuta contam como zero, mas só a partir do primeiro dia com registro (`firstDay`). A comparação da tela Hoje é contra a média dos 7 dias anteriores; no primeiro dia de uso ela não aparece.
- Sessão aberta: com alguém ouvindo agora (`MonitorStatus.listening`), o total vai até agora; senão, para no último heartbeat. A tela se atualiza a cada 15 s.
- Cada tela tem um ViewModel (Koin `viewModelOf`, `koinViewModel()`) com o estado montado por uma função pura (`buildTodayState`, `buildHistoryState`), testada na JVM (`ScreenStateTest`).
- Idioma: `MainActivity` é `AppCompatActivity` e o idioma usa `AppCompatDelegate.setApplicationLocales` (no 13+ quem guarda é o sistema; no 12, o `AppLocalesMetadataHolderService` do manifest). O `locales_config` é gerado pelo AGP (`generateLocaleConfig`) a partir de `values/` (inglês, o padrão, via `res/resources.properties`), `values-pt/` e `values-es/`.
- Formatos: durações em "2 h 14 min" (iguais nos 3 idiomas), datas e intervalos pelo idioma (ICU `DateIntervalFormat`), horário em 24 h ou AM/PM conforme o celular e o idioma.
- O tema escolhido fica no DataStore `app` (`AppPreferences`), junto com "o primeiro uso já foi visto". As barras do sistema seguem o tema do app, não o do celular.
- Ao abrir, a `MainActivity` religa o monitoramento se ele estava ligado e o processo tinha sido fechado.
- Fonte: os 4 pesos da Inter em `res/font/` (TTF do Google Fonts) e a licença em `assets/licenses/inter_OFL.txt`. Ícones: `PlugadoIcons`, desenhados a partir dos mesmos caminhos SVG do protótipo.
- Primeiro uso: a nota "seus dados ficam só neste celular" fica só na boas-vindas. No "Antes de começar" ela saiu (pedido do James em 21/out, pra tela caber sem rolar).
- Ícone do app (escolhido pelo James em 21/out, opção "Contorno forte" do canvas): fone minimalista em linha, com o arco descendo reto por fora e as conchas presas por dentro (um "b" e um "d"). Fixo no preto: fundo preto e fone branco (cores `launcher_background` e `launcher_glyph`), por causa do achado abaixo.
  - O desenho fica num lugar só, o `drawable/ic_plugado.xml`: 72 x 72 é a área visível do ícone, já com o respiro dele. Grade de 24 ampliada 2,1x, traço 2, ocupando 52,5% da largura e 55% da altura. Ele é branco; pra outra cor (preto no fundo branco, por exemplo), use tint.
  - Posição: centrado na horizontal e 2,2 (de 72) acima do centro na vertical, de propósito (centro ótico). No círculo, os cantos de baixo do fone chegam mais perto da borda que o topo redondo; com a subida, os dois ficam com quase o mesmo respiro (13,9 e 12,7). Pedido do James em 21/out: na primeira versão (2,5x, no centro exato) os cantos de baixo pareciam encostar na borda e sobrava espaço em cima. O teste de tamanhos e posições nos recortes de círculo, squircle e Play está no script do scratchpad da conversa (não versionado).
  - O `ic_launcher_foreground.xml` é um `<inset>` de 1/6 em volta do `ic_plugado` (vira os 108 dp do ícone adaptativo) e também é a camada monocromática. Ícone adaptativo só em XML (`mipmap-anydpi`; com minSdk 31 não precisa dos PNGs por densidade).
  - Pra repetir o ícone em outro lugar sem distorcer, desenhe o `ic_plugado` do mesmo tamanho do fundo (quadrado). O logo da boas-vindas é o `PlugadoLogo` (`ui/common/Components.kt`): círculo invertido de 32 dp com o fone por cima.
  - O PNG de 512 px pra loja é o `docs/loja/icone-512.png`, desenhado por script com a mesma geometria.
  - Medido no emulador (pixels dos prints): no ícone da gaveta (círculo de 159 px), respiro de 30 px no topo e 28 px nos cantos de baixo; no logo (84 px), 16 e 15 px, nos dois temas. Na horizontal, centrado (±0,5 px).
  - O mesmo fone aparece em todo o app: `PlugadoIcons.Headphones` (abas, cartões, Privacidade), na grade de 24 com o traço de 1,75 da interface e centrado (o desenho desce 0,5); e o ícone pequeno da notificação, `drawable/ic_stat_plugado.xml` (grade de 24, traço 2). O `ic_headphones` do Material foi apagado.
  - Achado (21/out, emulador API 37): a gaveta de notificações mostra o ícone do app, e o sistema guarda o ícone antigo até reiniciar. Depois de atualizar o app, a notificação ainda mostrava o robô verde do modelo; depois de reiniciar o celular, o ícone novo. No celular do James pode acontecer o mesmo.
- Achado do ícone (21/out, emulador API 37, Pixel Launcher): o launcher guarda o ícone e não troca quando o celular muda de tema. A versão do `values-night` só vale na hora da instalação ou da atualização (instalado no escuro, o ícone continua preto quando o celular volta pro claro). A tela de informações do app, que não guarda o ícone, troca certo.

### Sessão aberta (decisões de 27/out)

- Um toque numa sessão da lista de Hoje abre um cartão embaixo dela, com o gráfico do volume e os detalhes. Outro toque fecha. Fica uma aberta por vez. A tela guarda o id da sessão (`rememberSaveable`), então ela continua aberta quando a tela se atualiza e quando o tema ou o idioma mudam.
- O subtítulo do item ficou só "Volume médio X%". O fone saiu dali e foi pros detalhes (pedido do James).
- Gráfico (`VolumeChart`, desenhado num Canvas): linha em degraus, um por trecho, porque o volume muda aos pulos. A escala vai sempre de 0 a 100%, pra mesma altura ser o mesmo volume em qualquer sessão. O ponto do pico fica no meio do trecho mais longo nesse volume. Na sessão ao vivo, outro ponto marca o "agora", e a linha cresce a cada 15 s. Numa pausa curta entre dois trechos, a linha segue no volume anterior.
- Detalhes: Pico; Tempo no pico, que soma os trechos com o mesmo % arredondado (abaixo de 1 min aparece "< 1 min"); e Dispositivo, a saída onde a sessão passou mais tempo (sem nome, mostra o tipo, como na tela Dispositivos). Os dois números ocupam só o espaço que precisam, e o nome do fone fica com o resto, numa linha só: a fonte diminui de 20 até 12 sp pra caber (`TextAutoSize`, pedido do James). Se nem em 12 sp couber, o nome é cortado com reticências.
- No domínio, o `SessionSummary` ganhou `id`, `steps` (`VolumeStep`), `peakPercent` e `timeAtPeak`, montados no `toSessions()`. O banco não mudou.
- Ficaram de fora: "ajustes de volume" (trocado pelo dispositivo), dB e pausas (uma pausa de mais de 5 s já encerra a sessão). Ideias pra depois, mostradas só quando acontecerem: troca de fone na mesma sessão, marca de chamada e "começou ontem" (a sessão que passou da meia-noite aparece hoje começando às 00:00).
- No canvas: "Hoje · sessão aberta" (dá pra tocar nas sessões no modo Play), "Hoje · sessão ao vivo aberta" e "Hoje · sessão aberta, claro". A lista do "Hoje" também ganhou a setinha e o subtítulo novo.

## Arquitetura

```
:domain   Kotlin puro, nenhum import android.*
  model/     AudioSnapshot, AudioKind, Output, OutputType, Volume, Segment   [pronto]
  tracker/   SegmentTracker, TrackerConfig, TrackerCommand, OrphanRecovery   [pronto]
  stats/     ListeningStats: Span, sessões (degraus e pico), DayTotal, médias [pronto]
:app
  audio/     PlaybackSource, AudioModeSource, OutputDeviceSource, VolumeSource,
             SnapshotRules, AudioStateReader, AudioStateAggregator              [pronto]
  monitor/   ListeningMonitor, CommandSink, MonitorListener, MonitorService,
             MonitorNotification, MonitorSwitch, BootReceiver                [pronto]
  data/      db/ (SegmentEntity, DeviceEntity, DAOs, Converters, AppDatabase),
             prefs/MonitorSettings, ListeningRepository                       [pronto]
  ui/        PlugadoApp (raiz e 4 abas), AppViewModel, theme/ (cores, Inter,
             ícones), common/ (componentes, formatos, atalhos do sistema),
             today/, history/, devices/, settings/, privacy/, onboarding/    [pronto]
  di/        AppModule; variantModule existe em src/debug e em src/release   [pronto]
  util/      ticker
  debuglog/  (src/debug) DebugLog (vai pro logcat) e DebugLogScreen, que ficou
             sem lugar na tela desde a fase 6 (decidir com o James)
```

Fluxo: fontes → agregador (`AudioSnapshot`) → `SegmentTracker` (`TrackerCommand`) → serviço grava no Room → notificação e UI leem do Room via `Flow`.

- Toda regra de negócio fica no tracker, que é testável e não conhece Android. O serviço é "burro": liga as fontes, repassa snapshots e ticks e executa os comandos.
- Sem KMP: o iOS não deixa monitorar áudio de outros apps em background. Mesmo assim, o `:domain` puro deixa a porta aberta.
- minSdk 31 por causa do `addOnModeChangedListener`. Baixar pra 30 no futuro só mexe no `AudioModeSource`.

### Domínio (implementado)

- `AudioSnapshot(at, playing, kind, output?, volume)`, com `isListening = playing && output != null && volume.index > 0`.
- Volume 0 não conta como escuta (decidido em 20/out): no zero não se ouve nada. Assim, nenhum trecho fica com volume 0, e o `-Infinity` dB nunca chega no banco.
- `Output(type, name)` representa um aparelho físico: o A2DP e o SCO do mesmo fone viram um `Output` só (`BLUETOOTH` + `productName`).
- `Volume(index, max, db)`: guarde sempre os três, porque o máximo muda por aparelho (15 no Android puro, 30 no aparelho de teste).
- `Segment(id, sessionId, startedAt, day, kind, output, volume)` é um trecho com parâmetros constantes. Uma **sessão** é uma escuta contínua, com um ou mais trechos.
- `TrackerConfig`: `gapTolerance = 5 s`, `minSessionDuration = 10 s` e `zone`.
- `TrackerCommand`: `Open(segment)`, `Close(segmentId, endedAt)`, `Heartbeat(segmentId, at)` e `DiscardSession(sessionId)`.
- API do tracker: `onSnapshot(s)`, `onTick(now)`, `stop(now)` e `hasOpenSession`.

Regras do tracker:
- Sem sessão e `isListening` → abre sessão e trecho. Vale também pro primeiro snapshot, se o app iniciar com música já tocando.
- Parou de ouvir → marca `pausedAt`. Se voltar em até 5 s com os mesmos parâmetros, segue o mesmo trecho (a pausa conta como escuta). Se voltar com parâmetros diferentes, `Close` em `pausedAt` + `Open` em `s.at` (a pausa fica de fora).
- Volume, saída ou tipo mudou enquanto toca → `Close` + `Open` no mesmo instante, na mesma sessão.
- Volume chegou a 0 → conta como parar de ouvir: marca `pausedAt`, e as regras de pausa valem do mesmo jeito.
- Pausa maior que 5 s → a sessão termina em `pausedAt`, e não no momento em que isso foi detectado.
- Sessão com menos de 10 s (do início ao fim, contando pausas) → `DiscardSession`, que apaga todos os trechos dela. É isso que elimina o som de conexão do fone.
- Meia-noite (no `zone`) → `Close` na meia-noite + `Open` de um trecho novo no dia seguinte, na mesma sessão.
- `onTick` expira a pausa vencida, divide na meia-noite e emite `Heartbeat` (com `pausedAt ?: now`).

Os 11 testes do `SegmentTrackerTest` cobrem: app iniciando com música tocando, som de conexão (~2 s) descartado, troca de faixa sem quebrar o trecho, pausa longa fechando no momento da pausa, volume abrindo trecho novo na mesma sessão, tirar o fone fechando quando a música para, alto-falante não contando, divisão na meia-noite, `stop` fechando a sessão aberta, volume 0 não contando e volume indo a 0 funcionando como pausa. Cada caso veio de um log do spike (comentários `// Log hh:mm:ss`).

### Agregador (implementado, com regras tiradas do spike)

As fontes (`TriggerSource`, um `callbackFlow` cada) emitem só **gatilhos** (`Unit`). O `SystemAudioStateReader` lê o `AudioManager` numa `AudioReading` crua, e o `AudioReading.toSnapshot()` (em `SnapshotRules.kt`) aplica as regras abaixo. Ele é puro: usa só constantes do Android, e o volume entra por um `VolumeLookup`. Por isso roda em teste na JVM.

- `playing` = `isMusicActive`, ou `mode` em `IN_CALL`/`IN_COMMUNICATION`.
- `kind` = `CALL` se o mode for de chamada, senão `MEDIA`.
- `output` pra mídia: o primeiro que existir entre `BLUETOOTH_A2DP`, `WIRED_HEADPHONES`/`WIRED_HEADSET`, `USB_HEADSET`, `BLE_HEADSET` e `HEARING_AID`. O **SCO sozinho não conta pra mídia**, porque nesse caso a mídia ainda sai no alto-falante. Ignore `TYPE_FM` (14) e as saídas internas.
- `output` pra chamada: SCO, fio, USB ou BLE. *(Suposição, porque chamadas ainda não foram testadas.)*
- Saídas que o usuário marcar como "não é fone" (tabela `devices`) viram `output = null`.
- `volume`: stream `MUSIC` pra mídia e `VOICE_CALL` pra chamada. O dB vem de `getStreamVolumeDb(stream, index, tipo)` com o tipo da saída onde o som realmente toca (A2DP, não SCO; o spike errou isso na primeira versão).
- Não tome decisões pela lista de players: ela oscila e vem anonimizada.
- Debounce de 400 ms (os callbacks chegam em rajadas) e `distinctUntilChanged` comparando **sem** o `at`. O `at` é o horário da leitura.
- Se o `getStreamVolumeDb` lançar exceção (tipo de saída que o aparelho não aceita), o dB vira `NaN`.
- Na troca de dispositivo, o volume novo já aparece na leitura feita no próprio callback de dispositivo. O `ContentObserver` nem sempre dispara nessa hora, e o broadcast de volume não disparou nenhuma vez nessa situação.

⚠️ **O `isMusicActive` não tem callback.** Ele vira `false` até ~0,7 s *depois* do último callback de playback, e nenhum evento avisa dessa virada. Se o agregador só ler o estado nos gatilhos, um pause pode passar despercebido até o próximo evento qualquer, e horas seriam contadas a mais. O spike só viu essas viradas porque tinha polling de 500 ms. Como ficou no `AudioStateAggregator` (valores no `AggregatorConfig`):
- Depois de **cada** rajada de gatilhos (não só de playback, porque ler de novo é barato), uma releitura 1,5 s depois.
- Enquanto o último snapshot for `isListening`, polling a cada 5 s. Isso mantém o agregador independente do tracker: durante a pausa não precisa, porque a volta do play sempre chega com callback de playback. As expirações de pausa continuam vindo dos ticks do serviço.
- A lista de "não é fone" entra como `Flow<Set<Output>>` (vazia até existir o Room) e, quando muda, relê na hora.

### Banco (implementado na fase 5)

- Room 2.8.5 com KSP 2.3.12 (funciona com o AGP 9 e o Kotlin 2.2.10). O esquema de cada versão fica em `app/schemas/` e vai pro git, pras migrações futuras.
- `segments`: `id, sessionId, day, startedAt, endedAt?, lastSeenAt, kind, outputType, outputName, volumeIndex, volumeMax, volumeDb?`, com índices em `day` e `sessionId`. `Instant` vira milissegundos (dá pra subtrair no SQL) e `LocalDate` vira texto ISO. Enums viram texto (suporte nativo do Room).
- `volumeDb` é nullable, decidido em 20/out: o SQLite não guarda `NaN` (vira NULL), então o `NaN` do leitor vira `null` no `toEntity()`.
- `devices` (chave `type` + `name`, `isHeadphone`): toda saída entra como fone no primeiro `Open`. Marcar como "não é fone" tira ela do total, **inclusive no passado**, e alimenta o `notHeadphones` do agregador.
- Comandos → SQL: `Open` = insert (com `lastSeenAt = startedAt`) + insert-or-ignore em `devices`, `Close` = update de `endedAt`, `Heartbeat` = update de `lastSeenAt`, `DiscardSession` = delete por `sessionId`.
- Heartbeat gravado no máximo a cada 30 s (o tracker manda um a cada tick de 5 s). Decidido em 20/out.
- Ao iniciar, o serviço roda o `recoverOrphans()` antes do monitor: cada trecho aberto vira `OrphanSegment` (direto da query) e o `recover()` do domínio decide entre fechar no `lastSeenAt` ou descartar a sessão (< 10 s, contando desde o primeiro trecho dela).
- Total do dia: `SUM(COALESCE(endedAt, lastSeenAt) - startedAt)`, exposto como `Flow`. O volume médio fica pra fase de UI, calculado no `:domain/stats` (a média energética precisa de log/pow, que o SQLite do Android não garante).
- O liga/desliga fica no DataStore (`MonitorSettings`). O `MonitorSwitch` grava e liga/desliga o serviço. O "Pausar" da notificação também grava "desligado". O `BootReceiver` (não exportado: o sistema entrega mesmo assim) religa no `BOOT_COMPLETED` e no `MY_PACKAGE_REPLACED` se estiver ligado.
- A notificação mostra "Hoje: X min no fone". Ela relê a data a cada minuto, pra virar o dia na meia-noite. Sem a permissão de notificação, ela não é atualizada.

### Serviço e Android (fase 3 implementada)

Como ficou:
- O `ListeningMonitor` é o laço: snapshots e ticks entram num `merge` coletado por um coroutine só (o tracker não é thread-safe), e os comandos vão pro `CommandSink` e pro `MonitorListener`. Ticks a cada 5 s (`MonitorConfig`), só com sessão aberta. Ao ser cancelado, ele chama o `tracker.stop()` dentro de `NonCancellable`. É puro e é testado em tempo virtual.
- `CommandSink` é quem grava (hoje é um no-op; na fase 5 é o Room). `MonitorListener` é quem só acompanha (o `DebugLog` no debug, `MonitorListener.None` no release). O Koin escolhe pelo `variantModule`.
- O `MonitorService` só hospeda o `run()` num `CoroutineScope` próprio. `onDestroy` cancela o scope, e é isso que fecha a sessão. `START_STICKY`. Se o `startForeground` for recusado (`ForegroundServiceStartNotAllowedException`), o serviço se desliga em vez de quebrar.
- A ação "Pausar" da notificação manda um `ACTION_STOP` pro próprio serviço. A notificação usa `FOREGROUND_SERVICE_IMMEDIATE`.
- A tela de log (`DebugLogScreen`) fica na aba **Today** do BottomNavigation do `PlugadoApp`, decidido pelo James em 20/out (antes ela tinha um ícone próprio, "Plugado log", que foi removido). Ela continua num arquivo separado: quando a tela de hoje existir, troque a chamada no `PlugadoApp` e apague as duas `DebugLogScreen` (a do debug e o aviso do release). O log só existe no debug; no release, a aba mostra um aviso. Na prévia do Android Studio ela mostra só um texto, porque o Koin não roda lá. O log fica em memória e vai também pro logcat (tag `Plugado`).

Regras que continuam valendo:
- Foreground service do tipo `specialUse`. Não use `mediaPlayback` (o app não toca mídia) nem `dataSync` (tem limite de tempo). Declare o `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` no manifest e use `ServiceCompat.startForeground`. O tipo `specialUse` só existe a partir da API 34; nas APIs 31–33 o tipo não é exigido.
- O serviço inicia pela UI (quando o usuário liga o monitoramento) e pelo `BootReceiver` (`BOOT_COMPLETED`, e também `MY_PACKAGE_REPLACED` pra voltar depois de uma atualização), se o monitoramento estiver ligado no DataStore.
- Permissões: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` (runtime a partir do 13) e `RECEIVE_BOOT_COMPLETED`. `BLUETOOTH_CONNECT` é opcional.
- Ticks só com sessão aberta (veja o ⚠️ do agregador). Grave o heartbeat no banco com folga (por exemplo, a cada ~30 s), não a cada leitura.
- Desde o Android 13 o usuário pode parar o serviço pelo gerenciador de tarefas. Detecte isso e mostre "monitoramento desligado" na UI.
- Android 17: o "background audio hardening" restringe reprodução, foco e mudança de volume em background. O app só lê, então deve passar, mas teste (incluindo o início pelo boot). O 17 também limita a memória por app, então mantenha o serviço enxuto.
- Fabricantes como Xiaomi e Samsung matam serviços. O onboarding precisa pedir pra liberar a bateria (dontkillmyapp.com tem as instruções por marca).
- Ideia pra uma v2: Companion Device Manager, pra o sistema avisar quando o fone Bluetooth aparecer.

## O que o spike descobriu

Aparelho de teste: modelo `2412DPC0AG` (30 degraus de volume), com o fone Bluetooth "ZMI PurPods Pro".

| Pergunta | Resposta |
|---|---|
| O callback de playback dispara no play, no pause e na troca de faixa? | Sim, nos três. Na troca de faixa a lista de players esvazia e volta em ~180 ms. |
| A saída de cada player (`getAudioDeviceInfo()`) vem preenchida? | **Não**, vem `null` pra apps de terceiros. Por isso a regra é "tocando + fone de mídia conectado = conta". |
| Qual o atraso do `isMusicActive`? | Liga praticamente junto com o play. Desliga até ~0,7 s depois do pause, **sem callback**. |
| Volume: observer ou broadcast? | Os dois disparam ao apertar o volume. O `VOLUME_CHANGED_ACTION` (não documentado) chega 200–500 ms antes, um por degrau; o observer junta tudo num aviso só. Use o `ContentObserver` em `Settings.System.CONTENT_URI`. |
| E as chamadas (`mode`)? | **Não testado.** |

Outros achados:
- O mesmo fone aparece como `BT_SCO` (chamada) e `BT_A2DP` (mídia). Na reconexão, o SCO chega ~1,5 s antes do A2DP.
- O volume é guardado por dispositivo: ao tirar o fone, o volume de mídia vira o do alto-falante, e ao reconectar ele volta.
- Ao tirar o fone com música tocando, a música para primeiro (08,1 s), o `isMusicActive` cai (08,6 s) e só então o A2DP some (08,8 s). Nada vaza pro alto-falante.
- Ao conectar o fone toca um som (provavelmente o aviso de conexão) com dois players `usage=UNKNOWN` e `isMusicActive=true` por ~1,8 s. A duração mínima descarta isso.
- Música no alto-falante: `isMusicActive=true` sem nenhuma saída de fone, então não conta (correto).
- O `getStreamVolumeDb` retorna `-Infinity` no índice 0.
- Referência de dB no A2DP: 10/30 ≈ -39 dB e 12/30 ≈ -34 dB. No alto-falante, 4/30 ≈ -47 dB.
- As pausas de troca de faixa e de pause rápido ficaram entre 0,2 e 1,8 s, daí a tolerância de 5 s.
- Com o app em segundo plano, o Android congela o processo e entrega os eventos represados de uma vez quando ele volta, com os horários errados. Por isso o teste do YouTube não valeu, e por isso o app real precisa do foreground service.

## Achados do uso (depois do spike)

- Vídeo do Facebook, com som e sem som, no aparelho de teste (20/out): nenhum snapshot de escuta. Uma mudança de volume no meio registrou "parado", ou seja, `isMusicActive` falso naquela leitura. Ainda não se sabe se o `isMusicActive` fica falso o tempo todo com o Facebook ou se só nenhum gatilho chegou. O lado bom: o vídeo mudo não contou como escuta falsa. O lado ruim: o vídeo com som também não contou. A API pública dos players só mostra o tipo de uso e a saída de cada um, não se está tocando ou pausado.

## Ainda não testado (observar no uso do dia a dia)

- Ligação normal e chamada de WhatsApp: o `mode` muda? O SCO conta como fone? O `getCommunicationDevice()` (API 31) mostra a rota real da chamada (fone, auricular ou alto-falante)? Se mostrar, ele substitui a suposição da saída de chamada.
- Notificação chegando com a música tocando.
- YouTube, e vídeo **sem som** no feed (prévias, Reels mudos). Se o `isMusicActive` ligar nesse caso, rolar o feed de fone vira escuta falsa, e talvez isso tenha que ficar como limitação conhecida.
- Fone com fio ou USB-C (o `productName` pode vir genérico, e um adaptador USB-C pode aparecer como `TYPE_USB_DEVICE`, que hoje não conta), caixa de som Bluetooth e som do carro.
- Outros fabricantes (Samsung, outras marcas) e Android 17 com as restrições de áudio forçadas.
- Reinício pelo `START_STICKY` depois que o sistema mata o processo: o `startForeground` é aceito? Se for recusado, o serviço se desliga sem quebrar e deixa um aviso no logcat.
- `getAudioDevicesForAttributes` (API 33): confirmar se existe e se mostra a rota real da mídia. Se sim, dá pra refinar a regra da saída.

## Decisões em aberto

- Intervalos: a releitura (1,5 s) e o polling (5 s) estão no `AggregatorConfig`; o heartbeat grava a cada 30 s. Revisar se o uso mostrar algum problema.
- Rota manual pro alto-falante com o fone conectado conta como fone. É um caso raro e, por enquanto, aceito.
- Trocar o ícone do app pelo tema ficou de fora: exigiria `activity-alias`, e no Xiaomi o ícone pode mudar de lugar a cada troca.

## Regras do produto e da Play Store

- Nunca identificar o app nem a mídia. Não use `RECORD_AUDIO`, MediaProjection/AudioPlaybackCapture, NotificationListenerService, Accessibility nem `PACKAGE_USAGE_STATS`.
- Os dados ficam só no aparelho (por enquanto). Mesmo assim, o app precisa de política de privacidade, e o formulário de Data safety fica sem coleta.
- Backup na nuvem do Google **desligado** (decidido em 20/out): `allowBackup="false"` no manifest, e o `data_extraction_rules.xml` também exclui tudo do `cloud-backup`. A cópia direta de um celular pro outro (cabo ou Wi-Fi, sem nuvem) continua levando os dados; pra bloquear, é só adicionar um `<device-transfer>` com as mesmas exclusões. O `backup_rules.xml` foi apagado: ele só vale até o Android 11.
- O maior risco de rejeição na Play é a declaração do FGS `specialUse` (descrição, vídeo e caso de uso). A justificativa: monitoramento contínuo de exposição sonora em fones, ativado pelo usuário.
- Nada de promessa médica nos textos ("previne perda auditiva", "diagnostica"). Fale de hábito e bem-estar, e de "volume do sistema", nunca de dB SPL.
- Seja honesto na UI sobre as limitações: o volume do sistema não é o volume real (existe o volume interno de cada app e fones com volume próprio), e o A2DP não diferencia fone de caixa de som.

## Comandos

- `./gradlew :domain:test`: testes do domínio.
- `./gradlew :app:testDebugUnitTest`: testes do `:app` na JVM (regras, agregador, monitor e o estado das telas).
- `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest`: testes do banco **só no emulador** (veja o aviso em Configuração).
- `adb -s emulator-5554 shell cmd locale set-app-locales app.jammes.plugado --locales pt-BR`: muda o idioma do app sem passar pela tela (no Android 13+). O `pm clear` apaga essa escolha.
- `./gradlew :app:installDebug`: instala no aparelho.
- `adb logcat -s Plugado`: log do monitor (build de debug).
- O `adb` fica em `~/Library/Android/sdk/platform-tools/adb`, que pode não estar no PATH do terminal.
- Dados de exemplo no emulador (pros prints), **nunca no celular do James**: com o app parado (`am force-stop`), copie o `databases/plugado.db` e o `-wal` com `adb exec-out run-as app.jammes.plugado cat <arquivo>`, insira os trechos no SQLite, rode `PRAGMA wal_checkpoint(TRUNCATE)` e devolva o `.db` por `/data/local/tmp` com `run-as app.jammes.plugado cp`, apagando o `-wal` e o `-shm` do aparelho.
- `adb shell cmd audio set-volume 3 <n>`: muda o volume de mídia, pra testar a fonte de volume. O `cmd media_session volume --set` e a tecla de volume sem mídia tocando **não** mudam o volume de mídia no emulador.

## Configuração

- No `domain/build.gradle.kts`: `testImplementation(kotlin("test"))` e `tasks.test { useJUnitPlatform() }`.
- A versão da JVM do `:domain` precisa ser a mesma do `compileOptions` do `:app` (11 ou 17). Se não for, o Gradle reclama de "incompatible JVM version".
- Pode usar `java.time` no `:domain`, porque o minSdk é 31.
- No `:app`, os testes usam `kotlin("test-junit")` (JUnit 4) e `kotlinx-coroutines-test`. Com o AGP 9, o `kotlin("test")` sozinho não resolve o `kotlin.test.Test`.
- Nos testes instrumentados (`androidTest`), o nome do teste com crases pode ter espaço (minSdk ≥ 30), mas **não pode ter vírgula**: o D8 recusa ("cannot be represented in dex format"). Nos testes da JVM, pode.
- O `./gradlew :app:installDebug` instala em **todos** os aparelhos conectados (o celular do James costuma estar conectado por adb sem fio). Pra mirar um só, use `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`.
- ⚠️ O `connectedDebugAndroidTest` também roda em **todos** os aparelhos conectados e **desinstala o app no fim**, o que apaga o histórico de escuta. Nunca rode com o celular do James conectado: confira `adb devices` antes ou use `ANDROID_SERIAL=emulator-5554`.

## Referências

- Background audio hardening (Android 17): https://developer.android.com/about/versions/17/changes/bg-audio
- Iniciar FGS a partir do background: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Tipos de FGS: https://developer.android.com/develop/background-work/services/fgs/service-types
- Usuário parando o FGS: https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping
- Declaração de FGS na Play: https://support.google.com/googleplay/android-developer/answer/13392821
