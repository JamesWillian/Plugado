package app.jammes.plugado.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.domain.stats.DayTotal
import app.jammes.domain.stats.averagePerDay
import app.jammes.domain.stats.averageVolumePercent
import app.jammes.domain.stats.fillDays
import app.jammes.plugado.data.ListeningRepository
import app.jammes.plugado.util.ticker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

data class HistoryState(
    val periodDays: Int,
    val today: LocalDate,
    val days: List<DayTotal>, // do mais antigo pro mais novo, com zero nos dias sem escuta
    val total: Duration,
    val averagePerDay: Duration?,
    val averageVolumePercent: Int?,
    val selected: LocalDate,
) {
    val from: LocalDate get() = days.first().day
    val hasData: Boolean get() = !total.isZero
    val longestDay: DayTotal? get() = days.filter { !it.listened.isZero }.maxByOrNull { it.listened }
}

/** Monta o estado da tela a partir dos totais do banco. Pura, pra dar pra testar sem Android. */
fun buildHistoryState(
    today: LocalDate,
    periodDays: Int,
    totals: List<DayTotal>,
    firstDay: LocalDate?,
    selected: LocalDate?,
): HistoryState {
    val from = today.minusDays(periodDays - 1L)
    val days = totals.fillDays(from, today)
    return HistoryState(
        periodDays = periodDays,
        today = today,
        days = days,
        total = days.fold(Duration.ZERO) { sum, day -> sum + day.listened },
        averagePerDay = days.averagePerDay(from, today, firstDay),
        averageVolumePercent = days.averageVolumePercent(),
        selected = selected?.takeIf { !it.isBefore(from) && !it.isAfter(today) } ?: today,
    )
}

class HistoryViewModel(private val repository: ListeningRepository) : ViewModel() {

    private val period = MutableStateFlow(7)
    private val selected = MutableStateFlow<LocalDate?>(null)
    private val today = ticker(1.minutes).onStart { emit(Unit) }.map { LocalDate.now() }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HistoryState?> = combine(today, period) { day, days -> day to days }
        .flatMapLatest { (day, days) ->
            combine(
                repository.dailyTotals(day.minusDays(days - 1L), day),
                repository.firstDay,
                selected,
            ) { totals, firstDay, chosen -> buildHistoryState(day, days, totals, firstDay, chosen) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPeriod(days: Int) {
        period.value = days
        selected.value = null
    }

    fun select(day: LocalDate) {
        selected.value = day
    }
}
