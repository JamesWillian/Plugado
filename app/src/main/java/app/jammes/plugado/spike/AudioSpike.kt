package app.jammes.plugado.spike

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// SPIKE DESCARTÁVEL: só mostra o que o Android informa sobre o áudio. Apague depois.

private const val TAG = "PlugadoSpike"
private const val MAX_ENTRIES = 500

private val BUILT_IN_TYPES = setOf(
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
    AudioDeviceInfo.TYPE_TELEPHONY,
    AudioDeviceInfo.TYPE_REMOTE_SUBMIX,
    AudioDeviceInfo.TYPE_FM, // saída interna do rádio FM (aparece com o nome do modelo do celular)
)

private val HEADPHONE_TYPES = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_HEARING_AID,
)

class AudioSpy(private val context: Context) {

    private val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    private val lastVolumeBySource = mutableMapOf<String, Pair<Int, Int>>()
    private var pollJob: Job? = null

    // Vigia 1: players de qualquer app (anonimizados)
    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>?) {
            record("PLAYBACK", configs)
        }
    }

    // Vigia 2: modo de áudio (ligação / VoIP)
    private val modeListener = AudioManager.OnModeChangedListener { record("MODE") }

    // Vigia 3: saídas conectadas/desconectadas
    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) = logDevices("+", added)
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) = logDevices("-", removed)
    }

    // Vigia 4: volume, por dois caminhos, pra descobrir qual dispara melhor
    private val volumeObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            recordVolumeIfChanged("VOLUME (observer)")
        }
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            recordVolumeIfChanged("VOLUME (broadcast)")
        }
    }

    fun start() {
        am.registerAudioPlaybackCallback(playbackCallback, mainHandler)
        am.addOnModeChangedListener(context.mainExecutor, modeListener)
        am.registerAudioDeviceCallback(deviceCallback, mainHandler)
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, volumeObserver,
        )
        // Broadcast não documentado; só no spike, pra comparar com o observer.
        ContextCompat.registerReceiver(
            context,
            volumeReceiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
            ContextCompat.RECEIVER_EXPORTED,
        )

        // Polling de 500ms só pra medir quanto tempo isMusicActive/mode levam pra mudar.
        pollJob = scope.launch {
            var lastActive = am.isMusicActive
            var lastMode = am.mode
            while (isActive) {
                delay(500)
                val active = am.isMusicActive
                val mode = am.mode
                if (active != lastActive || mode != lastMode) {
                    record(
                        "POLL (isMusicActive $lastActive→$active, " +
                                "mode ${modeName(lastMode)}→${modeName(mode)})",
                    )
                    lastActive = active
                    lastMode = mode
                }
            }
        }

        record("START")
    }

    fun stop() {
        am.unregisterAudioPlaybackCallback(playbackCallback)
        am.removeOnModeChangedListener(modeListener)
        am.unregisterAudioDeviceCallback(deviceCallback)
        context.contentResolver.unregisterContentObserver(volumeObserver)
        context.unregisterReceiver(volumeReceiver)
        pollJob?.cancel()
    }

    fun mark(label: String) = append("════════ $label ════════")

    fun snapshot() = record("MANUAL")

    fun clear() = _entries.update { emptyList() }

    private fun logDevices(sign: String, devices: Array<out AudioDeviceInfo>?) {
        val relevant = devices.orEmpty().filter { it.isSink && it.type !in BUILT_IN_TYPES }
        if (relevant.isNotEmpty()) {
            record("DEVICE $sign " + relevant.joinToString { describe(it) })
        }
    }

    private fun recordVolumeIfChanged(source: String) {
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC) to
                am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        if (lastVolumeBySource.put(source, current) != current) record(source)
    }

    private fun record(source: String, configs: List<AudioPlaybackConfiguration>? = null) {
        val players = (configs ?: am.activePlaybackConfigurations)
            .joinToString(prefix = "[", postfix = "]") { cfg ->
                val output = cfg.audioDeviceInfo?.let { typeName(it.type) } ?: "?"
                "${usageName(cfg.audioAttributes.usage)}→$output"
            }

        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val outputs = devices
            .filter { it.type !in BUILT_IN_TYPES }
            .joinToString(prefix = "[", postfix = "]") { describe(it) }
        // Mídia sai pelo A2DP; o SCO do mesmo fone é o perfil de chamada.
        val headphone = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?: devices.firstOrNull { it.type in HEADPHONE_TYPES }

        val musicVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val musicMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val callVol = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        val callMax = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        val db = runCatching {
            am.getStreamVolumeDb(
                AudioManager.STREAM_MUSIC,
                musicVol,
                headphone?.type ?: AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            )
        }.map { "%.1f dB".format(it) }.getOrDefault("dB=?")

        append(
            buildString {
                appendLine("${LocalTime.now().format(timeFormat)}  $source")
                appendLine("  musicActive=${am.isMusicActive}  mode=${modeName(am.mode)}")
                appendLine("  saídas=$outputs")
                appendLine("  players=$players")
                append("  vol mídia=$musicVol/$musicMax ($db)  vol chamada=$callVol/$callMax")
            },
        )
    }

    private fun append(entry: String) {
        Log.d(TAG, entry)
        _entries.update { (listOf(entry) + it).take(MAX_ENTRIES) }
    }
}

private fun describe(d: AudioDeviceInfo) = "${typeName(d.type)} \"${d.productName}\""

private fun typeName(type: Int) = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "ALTO_FALANTE"
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "AURICULAR"
    AudioDeviceInfo.TYPE_WIRED_HEADSET -> "FIO_HEADSET"
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "FIO_FONE"
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BT_A2DP"
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BT_SCO"
    AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
    AudioDeviceInfo.TYPE_USB_DEVICE -> "USB_DEVICE"
    AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE_HEADSET"
    AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE_SPEAKER"
    AudioDeviceInfo.TYPE_HEARING_AID -> "APARELHO_AUDITIVO"
    AudioDeviceInfo.TYPE_HDMI -> "HDMI"
    else -> "tipo=$type"
}

private fun usageName(usage: Int) = when (usage) {
    AudioAttributes.USAGE_MEDIA -> "MEDIA"
    AudioAttributes.USAGE_GAME -> "GAME"
    AudioAttributes.USAGE_VOICE_COMMUNICATION -> "VOIP"
    AudioAttributes.USAGE_NOTIFICATION -> "NOTIF"
    AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> "RINGTONE"
    AudioAttributes.USAGE_ALARM -> "ALARME"
    AudioAttributes.USAGE_ASSISTANCE_SONIFICATION -> "SOM_SISTEMA"
    AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> "NAVEGACAO"
    AudioAttributes.USAGE_ASSISTANT -> "ASSISTENTE"
    AudioAttributes.USAGE_UNKNOWN -> "UNKNOWN"
    else -> "usage=$usage"
}

private fun modeName(mode: Int) = when (mode) {
    AudioManager.MODE_NORMAL -> "NORMAL"
    AudioManager.MODE_RINGTONE -> "TOCANDO"
    AudioManager.MODE_IN_CALL -> "LIGACAO"
    AudioManager.MODE_IN_COMMUNICATION -> "VOIP"
    AudioManager.MODE_CALL_SCREENING -> "TRIAGEM"
    else -> "mode=$mode"
}

@Composable
fun SpikeScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val spy = remember { AudioSpy(context.applicationContext) }
    val entries by spy.entries.collectAsState()
    var scenario by remember { mutableStateOf("") }

    DisposableEffect(spy) {
        view.keepScreenOn = true
        spy.start()
        onDispose {
            spy.stop()
            view.keepScreenOn = false
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = scenario,
                    onValueChange = { scenario = it },
                    label = { Text("Cenário (ex: Spotify + fone BT)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    spy.mark(scenario.ifBlank { "marca" })
                    scenario = ""
                }) { Text("Marcar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = spy::snapshot) { Text("Snapshot") }
                OutlinedButton(onClick = spy::clear) { Text("Limpar") }
                OutlinedButton(onClick = { shareLog(context, entries) }) { Text("Compartilhar") }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(entries) { entry ->
                    Text(
                        text = entry,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun shareLog(context: Context, entries: List<String>) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Plugado: log do spike")
        putExtra(Intent.EXTRA_TEXT, entries.reversed().joinToString("\n\n")) // ordem cronológica
    }
    context.startActivity(Intent.createChooser(send, "Compartilhar log"))
}