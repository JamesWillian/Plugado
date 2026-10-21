package app.jammes.domain.tracker

import java.time.Duration
import java.time.Instant

/**
 * Trecho que ficou aberto no banco porque o processo morreu no meio da escuta.
 * Ninguém mandou o `Close`, então o melhor fim que existe é o último heartbeat gravado.
 */
data class OrphanSegment(
    val segmentId: String,
    val sessionId: String,
    val sessionStartedAt: Instant, // início do primeiro trecho da sessão
    val lastSeenAt: Instant,
)

/** Fecha no último heartbeat ou, se a sessão ficou curta demais, descarta ela inteira. */
fun OrphanSegment.recover(config: TrackerConfig = TrackerConfig()): TrackerCommand =
    if (Duration.between(sessionStartedAt, lastSeenAt) < config.minSessionDuration) {
        TrackerCommand.DiscardSession(sessionId)
    } else {
        TrackerCommand.Close(segmentId, lastSeenAt)
    }
