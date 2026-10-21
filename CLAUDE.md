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
- Processo combinado: validar o comportamento no aparelho real antes de abstrair (por isso o spike veio antes da arquitetura).

## Estado atual (20/out/2026)

- [x] Projeto criado: minSdk 31, targetSdk 37, AGP 9.4.1 (Kotlin embutido), Kotlin 2.2.10, Gradle 9.6.0 e JVM 11 no `:app` e no `:domain`.
- [x] Spike descartável `AudioSpike.kt` no `:app`, rodado num aparelho real. Os achados estão mais abaixo.
- [x] Módulo `:domain` (Kotlin/JVM puro) criado. Packages: `app.jammes.domain.model` e `app.jammes.domain.tracker`. O app usa `app.jammes.plugado`.
- [x] Modelos (um arquivo por classe em `model/`), `SegmentTracker.kt` (com `TrackerConfig` e `TrackerCommand`) e `SegmentTrackerTest.kt`: 11 testes passando no projeto real (`./gradlew :domain:test`, 20/out).
- [x] `audio/` (fase 2): as quatro fontes, as regras puras (`SnapshotRules.kt`), o leitor do `AudioManager` e o `AudioStateAggregator`, com 15 testes na JVM (`./gradlew :app:testDebugUnitTest`). As fontes e o leitor ainda não rodaram num aparelho; isso vem com o serviço.
- [ ] Serviço e Room.
- [ ] Testes de chamada, WhatsApp, notificação e vídeo (dependem do serviço rodando em background).
- [ ] UI, onboarding e publicação.

## Próximos passos

1. Exercícios reservados pro James (não escreva por ele, a menos que ele peça):
   - `SegmentTrackerTest`, "pausa curta e volta com outro volume": toca em 0 s, pausa em 60 s, volta em 62 s com outro volume → `Close` em 60 + `Open` em 62, na mesma sessão.
   - `SnapshotRulesTest`, "SCO sozinho não conta pra mídia": só o SCO conectado, música tocando e mode normal → `output` null.
2. `monitor/MonitorService` (FGS `specialUse`), ligando agregador → tracker → repositório, com a notificação "hoje: X min" e uma ação de pausar.
3. `data/`: Room (`segments` e `devices`), `ListeningRepository` e DataStore pra lembrar se o monitoramento está ligado.
4. Trocar o spike por uma tela de log só no build de debug, mostrando os snapshots e os comandos do tracker. Depois, apagar o spike.
5. Com o serviço em background, rodar os testes pendentes (lista abaixo).
6. UI: tela de hoje (total, linha do tempo, volume médio), histórico com gráfico (Vico), tela de dispositivos e onboarding (notificação e bateria). Depois, endurecimento e Play Store.

## Arquitetura

```
:domain   Kotlin puro, nenhum import android.*
  model/     AudioSnapshot, AudioKind, Output, OutputType, Volume, Segment   [pronto]
  tracker/   SegmentTracker, TrackerConfig, TrackerCommand                   [pronto]
  stats/     DailyStats, média de volume                                     [planejado]
:app
  audio/     PlaybackSource, AudioModeSource, OutputDeviceSource, VolumeSource,
             SnapshotRules, AudioStateReader, AudioStateAggregator              [pronto]
  monitor/   MonitorService, BootReceiver, MonitorNotification
  data/      db/ (entities, DAOs, AppDatabase), prefs/ (DataStore), ListeningRepository
  ui/        today/, history/, devices/, onboarding/
  di/        módulos Koin
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

### Banco (planejado)

- `segments`: `id, sessionId, day, startedAt, endedAt?, lastSeenAt, kind, outputType, outputName, volumeIndex, volumeMax, volumeDb`, com índices em `day` e `sessionId` e TypeConverters pra `Instant` e `LocalDate`.
- `devices`: `name, type, isHeadphone`, pro usuário marcar caixa de som ou som do carro como "não conta".
- Comandos → SQL: `Open` = insert (com `lastSeenAt = startedAt`), `Close` = update de `endedAt`, `Heartbeat` = update de `lastSeenAt`, `DiscardSession` = delete por `sessionId`.
- Ao iniciar o serviço, trechos com `endedAt` nulo (o processo morreu) fecham em `lastSeenAt`. Aplique também a regra dos 10 s por sessão.
- Totais por dia e volume médio saem de uma query com `GROUP BY day`, exposta como `Flow`.

### Serviço e Android (planejado)

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

## Ainda não testado (fazer com o serviço rodando em background)

- Ligação normal e chamada de WhatsApp: o `mode` muda? O SCO conta como fone? O `getCommunicationDevice()` (API 31) mostra a rota real da chamada (fone, auricular ou alto-falante)? Se mostrar, ele substitui a suposição da saída de chamada.
- Notificação chegando com a música tocando.
- YouTube, e vídeo **sem som** no feed (prévias, Reels mudos). Se o `isMusicActive` ligar nesse caso, rolar o feed de fone vira escuta falsa, e talvez isso tenha que ficar como limitação conhecida.
- Fone com fio ou USB-C (o `productName` pode vir genérico, e um adaptador USB-C pode aparecer como `TYPE_USB_DEVICE`, que hoje não conta), caixa de som Bluetooth e som do carro.
- Outros fabricantes (Samsung, outras marcas) e Android 17 com as restrições de áudio forçadas.
- `getAudioDevicesForAttributes` (API 33): confirmar se existe e se mostra a rota real da mídia. Se sim, dá pra refinar a regra da saída.

## Decisões em aberto

- dB `NaN` (quando o `getStreamVolumeDb` lança exceção): como guardar no banco (nullable ou um valor mínimo)?
- Volume médio: média simples ponderada pelo tempo (boa pra UI) ou média energética, `10·log10(Σ t·10^(dB/10) / Σ t)` (melhor pra falar de exposição).
- Intervalos: a releitura (1,5 s) e o polling (5 s) estão no `AggregatorConfig` e falta confirmar no aparelho. O do heartbeat ainda não foi definido (sugestão: gravar a cada ~30 s).
- Rota manual pro alto-falante com o fone conectado conta como fone. É um caso raro e, por enquanto, aceito.

## Regras do produto e da Play Store

- Nunca identificar o app nem a mídia. Não use `RECORD_AUDIO`, MediaProjection/AudioPlaybackCapture, NotificationListenerService, Accessibility nem `PACKAGE_USAGE_STATS`.
- Os dados ficam só no aparelho (por enquanto). Mesmo assim, o app precisa de política de privacidade, e o formulário de Data safety fica sem coleta.
- O maior risco de rejeição na Play é a declaração do FGS `specialUse` (descrição, vídeo e caso de uso). A justificativa: monitoramento contínuo de exposição sonora em fones, ativado pelo usuário.
- Nada de promessa médica nos textos ("previne perda auditiva", "diagnostica"). Fale de hábito e bem-estar, e de "volume do sistema", nunca de dB SPL.
- Seja honesto na UI sobre as limitações: o volume do sistema não é o volume real (existe o volume interno de cada app e fones com volume próprio), e o A2DP não diferencia fone de caixa de som.

## Comandos

- `./gradlew :domain:test`: testes do domínio.
- `./gradlew :app:testDebugUnitTest`: testes do `:app` na JVM (regras e agregador).
- `./gradlew :app:installDebug`: instala no aparelho.
- `adb logcat -s PlugadoSpike`: logs do spike.

## Configuração

- No `domain/build.gradle.kts`: `testImplementation(kotlin("test"))` e `tasks.test { useJUnitPlatform() }`.
- A versão da JVM do `:domain` precisa ser a mesma do `compileOptions` do `:app` (11 ou 17). Se não for, o Gradle reclama de "incompatible JVM version".
- Pode usar `java.time` no `:domain`, porque o minSdk é 31.
- No `:app`, os testes usam `kotlin("test-junit")` (JUnit 4) e `kotlinx-coroutines-test`. Com o AGP 9, o `kotlin("test")` sozinho não resolve o `kotlin.test.Test`.

## Referências

- Background audio hardening (Android 17): https://developer.android.com/about/versions/17/changes/bg-audio
- Iniciar FGS a partir do background: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Tipos de FGS: https://developer.android.com/develop/background-work/services/fgs/service-types
- Usuário parando o FGS: https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping
- Declaração de FGS na Play: https://support.google.com/googleplay/android-developer/answer/13392821
