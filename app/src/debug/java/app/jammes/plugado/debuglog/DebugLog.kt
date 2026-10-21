package app.jammes.plugado.debuglog

import android.util.Log
import app.jammes.domain.model.AudioSnapshot
import app.jammes.domain.tracker.TrackerCommand
import app.jammes.plugado.monitor.MonitorListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "Plugado"
private const val MAX_ENTRIES = 1000
private val TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

/**
 * Log em memória do monitor, só no debug. Some se o processo morrer, então tudo vai também
 * pro logcat: `adb logcat -s Plugado`.
 */
class DebugLog : MonitorListener {

    private val _entries = MutableStateFlow<List<String>>(emptyList())

    /** Mais recente primeiro. */
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    override fun onStarted() {
        _running.value = true
        add("MONITOR LIGADO")
    }

    override fun onSnapshot(snapshot: AudioSnapshot) = add(snapshot.describe())

    override fun onCommands(commands: List<TrackerCommand>) {
        // O heartbeat só importa pro banco. No log, ele só polui.
        commands.filterNot { it is TrackerCommand.Heartbeat }.forEach { add(it.describe()) }
    }

    override fun onStopped() {
        _running.value = false
        add("MONITOR DESLIGADO")
    }

    fun mark(label: String) = add("════════ $label ════════")

    fun clear() = _entries.update { emptyList() }

    private fun add(text: String) {
        val entry = "${LocalTime.now().format(TIME)}  $text"
        Log.d(TAG, entry)
        _entries.update { (listOf(entry) + it).take(MAX_ENTRIES) }
    }
}

private fun AudioSnapshot.describe(): String {
    val state = when {
        isListening -> "OUVINDO"
        playing -> "tocando, não conta"
        else -> "parado"
    }
    val where = output?.let { "${it.type} \"${it.name}\"" } ?: "sem fone"
    val db = "%.1f".format(volume.db)
    return "SNAPSHOT $state · $kind · $where · vol ${volume.index}/${volume.max} ($db dB)"
}

private fun TrackerCommand.describe(): String = when (this) {
    is TrackerCommand.Open ->
        "OPEN trecho ${segment.id.short()} (sessão ${segment.sessionId.short()}) desde ${segment.startedAt.time()}"
    is TrackerCommand.Close -> "CLOSE trecho ${segmentId.short()} em ${endedAt.time()}"
    is TrackerCommand.DiscardSession -> "DESCARTE da sessão ${sessionId.short()} (curta demais)"
    is TrackerCommand.Heartbeat -> "HEARTBEAT trecho ${segmentId.short()} em ${at.time()}"
}

private fun String.short() = take(4)

private fun Instant.time() = atZone(ZoneId.systemDefault()).toLocalTime().format(TIME)
