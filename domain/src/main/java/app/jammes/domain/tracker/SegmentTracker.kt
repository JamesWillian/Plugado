package app.jammes.domain.tracker

import app.jammes.domain.model.AudioSnapshot
import app.jammes.domain.model.Segment
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class TrackerConfig(
    /** Pausa máxima que ainda conta como a mesma escuta (troca de faixa, buffering). */
    val gapTolerance: Duration = Duration.ofSeconds(5),
    /** Sessões mais curtas que isso são descartadas (som de conexão, notificação). */
    val minSessionDuration: Duration = Duration.ofSeconds(10),
    /** Fuso usado pra dividir os trechos na meia-noite. */
    val zone: ZoneId = ZoneId.systemDefault(),
)

/** Ordens que o tracker devolve. Quem executa (gravar no Room) é o serviço. */
sealed interface TrackerCommand {
    /** Inserir um trecho novo. */
    data class Open(val segment: Segment) : TrackerCommand

    /** Gravar o fim de um trecho. */
    data class Close(val segmentId: String, val endedAt: Instant) : TrackerCommand

    /** Atualizar o "visto por último" do trecho aberto (recuperação se o processo morrer). */
    data class Heartbeat(val segmentId: String, val at: Instant) : TrackerCommand

    /** Apagar TODOS os trechos da sessão: ela foi curta demais pra contar. */
    data class DiscardSession(val sessionId: String) : TrackerCommand
}

/**
 * O cronometrista. Recebe snapshots e ticks, devolve comandos.
 * Não conhece Android, banco nem relógio: tudo entra por parâmetro.
 */
class SegmentTracker(
    private val config: TrackerConfig = TrackerConfig(),
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private class OpenSession(
        val id: String,
        val startedAt: Instant,
        var segment: Segment,
        var pausedAt: Instant? = null,
    )

    private var session: OpenSession? = null

    /** O serviço só precisa mandar ticks enquanto isso for true. */
    val hasOpenSession: Boolean get() = session != null

    fun onSnapshot(s: AudioSnapshot): List<TrackerCommand> {
        val out = mutableListOf<TrackerCommand>()
        expireIfGapExceeded(s.at, out)
        session?.let { splitAtMidnights(it, it.pausedAt ?: s.at, out) }

        val current = session
        when {
            current == null -> if (s.isListening) openSession(s, out)
            !s.isListening -> if (current.pausedAt == null) current.pausedAt = s.at
            else -> resumeOrSwitch(current, s, out)
        }

        session?.let { splitAtMidnights(it, it.pausedAt ?: s.at, out) }
        return out
    }

    fun onTick(now: Instant): List<TrackerCommand> {
        val out = mutableListOf<TrackerCommand>()
        expireIfGapExceeded(now, out)
        val current = session ?: return out
        val lastSeen = current.pausedAt ?: now
        splitAtMidnights(current, lastSeen, out)
        out += TrackerCommand.Heartbeat(current.segment.id, lastSeen)
        return out
    }

    /** Monitoramento desligado ou serviço encerrando. */
    fun stop(now: Instant): List<TrackerCommand> {
        val out = mutableListOf<TrackerCommand>()
        session?.let { endSession(it, it.pausedAt ?: now, out) }
        return out
    }

    private fun openSession(s: AudioSnapshot, out: MutableList<TrackerCommand>) {
        val sessionId = newId()
        val opened = OpenSession(sessionId, s.at, newSegment(sessionId, s))
        session = opened
        out += TrackerCommand.Open(opened.segment)
    }

    private fun resumeOrSwitch(
        current: OpenSession,
        s: AudioSnapshot,
        out: MutableList<TrackerCommand>,
    ) {
        val pausedAt = current.pausedAt
        current.pausedAt = null
        // Mesmos parâmetros: o trecho continua (uma pausa curta conta como escuta).
        if (current.segment.matches(s)) return

        out += TrackerCommand.Close(current.segment.id, pausedAt ?: s.at)
        current.segment = newSegment(current.id, s)
        out += TrackerCommand.Open(current.segment)
    }

    private fun expireIfGapExceeded(now: Instant, out: MutableList<TrackerCommand>) {
        val current = session ?: return
        val pausedAt = current.pausedAt ?: return
        if (Duration.between(pausedAt, now) > config.gapTolerance) {
            endSession(current, pausedAt, out)
        }
    }

    private fun endSession(
        current: OpenSession,
        endedAt: Instant,
        out: MutableList<TrackerCommand>,
    ) {
        splitAtMidnights(current, endedAt, out)
        val listened = Duration.between(current.startedAt, endedAt)
        out += if (listened < config.minSessionDuration) {
            TrackerCommand.DiscardSession(current.id)
        } else {
            TrackerCommand.Close(current.segment.id, endedAt)
        }
        session = null
    }

    /** Garante que cada trecho pertença a um dia só. */
    private fun splitAtMidnights(
        current: OpenSession,
        until: Instant,
        out: MutableList<TrackerCommand>,
    ) {
        while (true) {
            val nextDay = current.segment.day.plusDays(1)
            val midnight = nextDay.atStartOfDay(config.zone).toInstant()
            if (until <= midnight) return
            out += TrackerCommand.Close(current.segment.id, midnight)
            current.segment = current.segment.copy(id = newId(), startedAt = midnight, day = nextDay)
            out += TrackerCommand.Open(current.segment)
        }
    }

    private fun newSegment(sessionId: String, s: AudioSnapshot) = Segment(
        id = newId(),
        sessionId = sessionId,
        startedAt = s.at,
        day = s.at.atZone(config.zone).toLocalDate(),
        kind = s.kind,
        output = requireNotNull(s.output),
        volume = s.volume,
    )

    private fun Segment.matches(s: AudioSnapshot) =
        kind == s.kind && output == s.output && volume == s.volume
}