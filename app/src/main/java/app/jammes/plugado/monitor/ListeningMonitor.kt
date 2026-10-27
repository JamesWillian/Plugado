package app.jammes.plugado.monitor

import app.jammes.domain.model.AudioSnapshot
import app.jammes.domain.tracker.SegmentTracker
import app.jammes.domain.tracker.TrackerCommand
import app.jammes.plugado.util.ticker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class MonitorConfig(
    /** Ticks enquanto houver sessão aberta: expiram a pausa, dividem na meia-noite e geram heartbeat. */
    val tickInterval: Duration = 5.seconds,
)

/**
 * O laço do monitoramento: cada snapshot e cada tick passam pelo tracker, e os comandos que
 * voltam vão pro [sink]. Não conhece Android: o serviço só hospeda o [run].
 * Use uma instância por vez que o monitoramento liga (o tracker guarda estado).
 */
class ListeningMonitor(
    private val snapshots: Flow<AudioSnapshot>,
    private val tracker: SegmentTracker,
    private val sink: CommandSink,
    private val listener: MonitorListener = MonitorListener.None,
    private val status: MonitorStatus = MonitorStatus(),
    private val config: MonitorConfig = MonitorConfig(),
    private val now: () -> Instant = Instant::now,
) {
    private sealed interface Input {
        data class Snapshot(val snapshot: AudioSnapshot) : Input
        data object Tick : Input
    }

    /** Roda até ser cancelado. Ao cancelar, fecha a sessão que estiver aberta. */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun run() {
        // Ticks só com sessão aberta: sem sessão, o onTick não teria o que fazer.
        val sessionOpen = MutableStateFlow(false)
        val ticks = sessionOpen.flatMapLatest { open ->
            if (open) ticker(config.tickInterval) else emptyFlow()
        }

        listener.onStarted()
        try {
            // Snapshots e ticks numa fila só: o tracker não é thread-safe.
            merge(snapshots.map { Input.Snapshot(it) }, ticks.map { Input.Tick }).collect { input ->
                val commands = when (input) {
                    is Input.Snapshot -> {
                        listener.onSnapshot(input.snapshot)
                        status.setListening(input.snapshot.isListening)
                        tracker.onSnapshot(input.snapshot)
                    }
                    Input.Tick -> tracker.onTick(now())
                }
                execute(commands)
                sessionOpen.value = tracker.hasOpenSession
            }
        } finally {
            withContext(NonCancellable) {
                execute(tracker.stop(now()))
                status.setListening(false)
                listener.onStopped()
            }
        }
    }

    private suspend fun execute(commands: List<TrackerCommand>) {
        if (commands.isEmpty()) return
        listener.onCommands(commands)
        commands.forEach { sink.execute(it) }
    }
}
