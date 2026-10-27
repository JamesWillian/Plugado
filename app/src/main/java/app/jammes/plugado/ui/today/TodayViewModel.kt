package app.jammes.plugado.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.domain.stats.DayTotal
import app.jammes.domain.stats.SessionSummary
import app.jammes.domain.stats.Span
import app.jammes.domain.stats.averagePerDay
import app.jammes.domain.stats.averageVolumePercent
import app.jammes.domain.stats.toSessions
import app.jammes.plugado.data.ListeningRepository
import app.jammes.plugado.data.prefs.MonitorSettings
import app.jammes.plugado.monitor.MonitorStatus
import app.jammes.plugado.monitor.MonitorSwitch
import app.jammes.plugado.util.ticker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

data class TodayState(
    val day: LocalDate,
    val zone: ZoneId,
    val now: Instant,
    val enabled: Boolean,       // o usuário deixou o monitoramento ligado
    val listening: Boolean,     // tem som saindo pelo fone agora
    val total: Duration,
    val versusAverage: Duration?, // positivo = acima da média dos 7 dias anteriores
    val spans: List<Span>,
    val sessions: List<SessionSummary>,
    val averageVolumePercent: Int?,
) {
    val hasData: Boolean get() = sessions.isNotEmpty()
    val longest: Duration? get() = sessions.maxOfOrNull { it.listened }

    private val dayStart: Instant get() = day.atStartOfDay(zone).toInstant()
    private val dayEnd: Instant get() = day.plusDays(1).atStartOfDay(zone).toInstant()

    /** Onde [instant] cai no dia, de 0 (meia-noite) a 1 (meia-noite seguinte). */
    fun fractionOfDay(instant: Instant): Float =
        (Duration.between(dayStart, instant).toMillis().toFloat() / Duration.between(dayStart, dayEnd).toMillis())
            .coerceIn(0f, 1f)
}

/** Monta o estado da tela a partir dos dados crus. Pura, pra dar pra testar sem Android. */
fun buildTodayState(
    day: LocalDate,
    zone: ZoneId,
    now: Instant,
    spans: List<Span>,
    previousDays: List<DayTotal>,
    firstDay: LocalDate?,
    enabled: Boolean,
    listening: Boolean,
): TodayState {
    // Com alguém ouvindo, o trecho aberto vai até agora: o banco só grava o heartbeat a cada 30 s.
    val live = spans.map { if (it.open && listening && now.isAfter(it.end)) it.copy(end = now) else it }
    val total = live.fold(Duration.ZERO) { sum, span -> sum + span.duration }
    val average = previousDays.averagePerDay(day.minusDays(7), day.minusDays(1), firstDay)
    return TodayState(
        day = day,
        zone = zone,
        now = now,
        enabled = enabled,
        listening = listening,
        total = total,
        versusAverage = average?.let { total - it },
        spans = live,
        sessions = live.toSessions(),
        averageVolumePercent = live.averageVolumePercent(),
    )
}

class TodayViewModel(
    private val repository: ListeningRepository,
    private val settings: MonitorSettings,
    private val status: MonitorStatus,
    private val switch: MonitorSwitch,
) : ViewModel() {

    // A tela se atualiza sozinha: a sessão ao vivo cresce e o dia vira na meia-noite.
    private val now = ticker(15.seconds).onStart { emit(Unit) }.map { Instant.now() }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TodayState?> = now
        .map { it.atZone(ZoneId.systemDefault()).toLocalDate() }
        .distinctUntilChanged()
        .flatMapLatest { day ->
            val data = combine(
                repository.countedSpansOn(day),
                repository.dailyTotals(day.minusDays(7), day.minusDays(1)),
                repository.firstDay,
            ) { spans, previous, firstDay -> Triple(spans, previous, firstDay) }
            combine(data, settings.enabled, status.listening, now) { (spans, previous, firstDay), enabled, listening, instant ->
                buildTodayState(day, ZoneId.systemDefault(), instant, spans, previous, firstDay, enabled, listening)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setMonitoring(on: Boolean) {
        viewModelScope.launch { if (on) switch.turnOn() else switch.turnOff() }
    }
}
