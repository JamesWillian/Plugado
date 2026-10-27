package app.jammes.plugado.ui

import app.jammes.domain.model.Output
import app.jammes.domain.model.OutputType
import app.jammes.domain.model.Volume
import app.jammes.domain.stats.DayTotal
import app.jammes.domain.stats.Span
import app.jammes.plugado.ui.history.buildHistoryState
import app.jammes.plugado.ui.today.buildTodayState
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** O estado das telas é montado por funções puras: dá pra testar sem Android. */
class ScreenStateTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 21)
    private val fone = Output(OutputType.BLUETOOTH, "ZMI PurPods Pro")

    private fun at(time: String) = Instant.parse("2026-10-21T${time}Z")

    private fun openSpan(from: String, lastHeartbeat: String) =
        Span("s1", at(from), at(lastHeartbeat), fone, Volume(15, 30, -30f), open = true)

    @Test
    fun `com alguém ouvindo, a sessão aberta conta até agora`() {
        // O banco só tem o heartbeat das 21:30, mas são 21:40 e a música continua tocando
        val state = buildTodayState(
            today, zone, now = at("21:40:00"), spans = listOf(openSpan("21:00:00", "21:30:00")),
            previousDays = emptyList(), firstDay = today, enabled = true, listening = true,
        )

        assertEquals(Duration.ofMinutes(40), state.total)
    }

    @Test
    fun `sem ninguém ouvindo, a sessão aberta para no último heartbeat`() {
        // Pausa curta, ou o processo morreu: não dá pra contar até agora
        val state = buildTodayState(
            today, zone, now = at("21:40:00"), spans = listOf(openSpan("21:00:00", "21:30:00")),
            previousDays = emptyList(), firstDay = today, enabled = true, listening = false,
        )

        assertEquals(Duration.ofMinutes(30), state.total)
    }

    @Test
    fun `hoje é comparado com a média dos 7 dias anteriores`() {
        val previous = (1..7).map { DayTotal(today.minusDays(it.toLong()), Duration.ofMinutes(60), 50.0) }

        val state = buildTodayState(
            today, zone, now = at("21:40:00"), spans = listOf(openSpan("20:00:00", "21:30:00")),
            previousDays = previous, firstDay = today.minusDays(30), enabled = true, listening = false,
        )

        assertEquals(Duration.ofMinutes(30), state.versusAverage) // 90 min hoje, média de 60
    }

    @Test
    fun `no primeiro dia de uso não tem com o que comparar`() {
        val state = buildTodayState(
            today, zone, now = at("21:40:00"), spans = listOf(openSpan("20:00:00", "21:30:00")),
            previousDays = emptyList(), firstDay = today, enabled = true, listening = false,
        )

        assertNull(state.versusAverage)
    }

    @Test
    fun `histórico preenche o período e começa com hoje selecionado`() {
        val totals = listOf(DayTotal(today.minusDays(2), Duration.ofMinutes(45), 60.0))

        val state = buildHistoryState(today, periodDays = 7, totals = totals, firstDay = today.minusDays(2), selected = null)

        assertEquals(7, state.days.size)
        assertEquals(today, state.selected)
        assertEquals(Duration.ofMinutes(15), state.averagePerDay) // 45 min em 3 dias (do primeiro registro até hoje)
    }
}
