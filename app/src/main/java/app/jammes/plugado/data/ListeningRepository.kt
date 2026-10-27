package app.jammes.plugado.data

import app.jammes.domain.model.Output
import app.jammes.domain.model.Volume
import app.jammes.domain.stats.DayTotal
import app.jammes.domain.stats.Span
import app.jammes.domain.tracker.TrackerCommand
import app.jammes.domain.tracker.recover
import app.jammes.plugado.data.db.AppDatabase
import app.jammes.plugado.data.db.DeviceEntity
import app.jammes.plugado.data.db.toEntity
import app.jammes.plugado.monitor.CommandSink
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Grava os comandos do tracker no Room e expõe o que a notificação e a UI leem. */
class ListeningRepository(
    db: AppDatabase,
    /** Folga entre heartbeats gravados: o tracker manda um a cada tick, o banco não precisa de todos. */
    private val heartbeatEvery: Duration = Duration.ofSeconds(30),
) : CommandSink {

    private val segments = db.segments()
    private val devices = db.devices()

    // O último heartbeat gravado. O tracker só tem um trecho aberto por vez, então basta um.
    private var lastHeartbeat: TrackerCommand.Heartbeat? = null

    override suspend fun execute(command: TrackerCommand) {
        when (command) {
            is TrackerCommand.Open -> {
                val segment = command.segment
                segments.insert(segment.toEntity())
                devices.insertIfNew(DeviceEntity(segment.output.type, segment.output.name))
                lastHeartbeat = TrackerCommand.Heartbeat(segment.id, segment.startedAt)
            }
            is TrackerCommand.Close -> segments.close(command.segmentId, command.endedAt)
            is TrackerCommand.Heartbeat -> {
                val last = lastHeartbeat
                if (last == null || last.segmentId != command.segmentId ||
                    Duration.between(last.at, command.at) >= heartbeatEvery
                ) {
                    segments.updateLastSeen(command.segmentId, command.at)
                    lastHeartbeat = command
                }
            }
            is TrackerCommand.DiscardSession -> segments.deleteSession(command.sessionId)
        }
    }

    /** Fecha os trechos que ficaram abertos porque o processo morreu. Rode antes de ligar o monitor. */
    suspend fun recoverOrphans() {
        segments.orphans().forEach { execute(it.recover()) }
    }

    /** Tempo ouvido no dia, atualizado a cada gravação. */
    fun listenedOn(day: LocalDate): Flow<Duration> =
        segments.listenedMillis(day).map(Duration::ofMillis)

    /** Os trechos que contam no dia. O fim de um trecho aberto é o último heartbeat gravado. */
    fun countedSpansOn(day: LocalDate): Flow<List<Span>> =
        segments.countedSegmentsOn(day).map { list ->
            list.map {
                Span(
                    sessionId = it.sessionId,
                    start = it.startedAt,
                    end = it.endedAt ?: it.lastSeenAt,
                    output = Output(it.outputType, it.outputName),
                    volume = Volume(it.volumeIndex, it.volumeMax, it.volumeDb ?: Float.NaN),
                    open = it.endedAt == null,
                )
            }
        }

    /** Os dias com escuta entre [from] e [to] (os dias sem escuta não vêm: veja `fillDays`). */
    fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> =
        segments.dailyTotals(from, to).map { rows ->
            rows.map { row ->
                DayTotal(
                    day = row.day,
                    listened = Duration.ofMillis(row.listenedMillis),
                    volumePercent = if (row.listenedMillis > 0) row.volumePercentMillis / row.listenedMillis else null,
                )
            }
        }

    /** O primeiro dia com algum registro, ou null se o banco está vazio. */
    val firstDay: Flow<LocalDate?> = segments.firstDay()

    /** Todas as saídas que já tocaram, com o tempo de cada uma. */
    val devicesWithTotals: Flow<List<DeviceTotal>> =
        devices.devicesWithTotals().map { rows ->
            rows.map { DeviceTotal(Output(it.type, it.name), it.isHeadphone, Duration.ofMillis(it.listenedMillis)) }
        }

    suspend fun setCounts(output: Output, counts: Boolean) =
        devices.setHeadphone(output.type, output.name, counts)

    /** Apaga o histórico e a lista de dispositivos. As preferências (tema, idioma) ficam. */
    suspend fun deleteAll() {
        segments.deleteAll()
        devices.deleteAll()
    }

    /** Saídas que o usuário marcou como "não é fone". O agregador ignora elas. */
    val notHeadphones: Flow<Set<Output>> =
        devices.notHeadphones().map { list -> list.mapTo(mutableSetOf()) { Output(it.type, it.name) } }
}

/** Uma saída com o tempo total dela. `counts` = o usuário deixou contar como fone. */
data class DeviceTotal(val output: Output, val counts: Boolean, val listened: Duration)
