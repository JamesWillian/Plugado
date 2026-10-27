package app.jammes.domain.stats

import app.jammes.domain.model.Output
import app.jammes.domain.model.Volume
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Um trecho já gravado. O fim de um trecho aberto é o último instante conhecido dele. */
data class Span(
    val sessionId: String,
    val start: Instant,
    val end: Instant,
    val output: Output,
    val volume: Volume,
    val open: Boolean = false,
) {
    val duration: Duration get() = Duration.between(start, end)
}

/** Uma sessão de escuta: os trechos dela somados. */
data class SessionSummary(
    val start: Instant,
    val end: Instant,
    val listened: Duration,
    val output: Output,     // a saída onde a sessão passou mais tempo
    val volumePercent: Int, // ponderado pelo tempo
    val open: Boolean,
)

/** O total de um dia. `volumePercent` é null nos dias sem escuta. */
data class DayTotal(val day: LocalDate, val listened: Duration, val volumePercent: Double?)

/** Volume em % do máximo do sistema: 12 de 30 é 40%. */
val Volume.percent: Double get() = index * 100.0 / max

/** Agrupa os trechos por sessão, da mais recente pra mais antiga. */
fun List<Span>.toSessions(): List<SessionSummary> =
    groupBy { it.sessionId }.values
        .map { spans ->
            SessionSummary(
                start = spans.minOf { it.start },
                end = spans.maxOf { it.end },
                listened = spans.fold(Duration.ZERO) { total, span -> total + span.duration },
                output = spans.groupBy { it.output }.maxBy { (_, same) -> same.sumOf { it.duration.toMillis() } }.key,
                volumePercent = spans.averageVolumePercent() ?: 0,
                open = spans.any { it.open },
            )
        }
        .sortedByDescending { it.start }

/** Volume médio dos trechos, ponderado pelo tempo de cada um. Null se não houve escuta. */
fun List<Span>.averageVolumePercent(): Int? {
    val total = sumOf { it.duration.toMillis() }
    if (total <= 0) return null
    return (sumOf { it.duration.toMillis() * it.volume.percent } / total).roundToInt()
}

/** Os dias de [from] a [to], com zero nos dias sem escuta. */
fun List<DayTotal>.fillDays(from: LocalDate, to: LocalDate): List<DayTotal> {
    val byDay = associateBy { it.day }
    return generateSequence(from) { it.plusDays(1) }
        .takeWhile { !it.isAfter(to) }
        .map { byDay[it] ?: DayTotal(it, Duration.ZERO, null) }
        .toList()
}

/**
 * Média por dia entre [from] e [to]. Dia sem escuta conta como zero, mas os dias antes do
 * primeiro registro ([firstDay]) ficam de fora: o app ainda não estava medindo.
 */
fun List<DayTotal>.averagePerDay(from: LocalDate, to: LocalDate, firstDay: LocalDate?): Duration? {
    if (firstDay == null || firstDay.isAfter(to)) return null
    val start = maxOf(from, firstDay)
    val days = ChronoUnit.DAYS.between(start, to) + 1
    val total = filter { !it.day.isBefore(start) && !it.day.isAfter(to) }.sumOf { it.listened.toMillis() }
    return Duration.ofMillis(total / days)
}

/** Volume médio do período, ponderado pelo tempo de cada dia. Null se não houve escuta. */
@JvmName("averageVolumePercentOfDays")
fun List<DayTotal>.averageVolumePercent(): Int? {
    val heard = filter { it.volumePercent != null && !it.listened.isZero }
    val total = heard.sumOf { it.listened.toMillis() }
    if (total <= 0) return null
    return (heard.sumOf { it.listened.toMillis() * it.volumePercent!! } / total).roundToInt()
}
