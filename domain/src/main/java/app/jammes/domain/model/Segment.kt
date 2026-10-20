package app.jammes.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Trecho contínuo com os mesmos parâmetros (tipo, saída, volume).
 * Vários trechos seguidos formam uma sessão de escuta (mesmo sessionId).
 */
data class Segment(
    val id: String,
    val sessionId: String,
    val startedAt: Instant,
    val day: LocalDate,
    val kind: AudioKind,
    val output: Output,
    val volume: Volume,
)