package app.jammes.domain.stats

import app.jammes.domain.model.Output
import app.jammes.domain.model.OutputType
import app.jammes.domain.model.Volume
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListeningStatsTest {

    private val fone = Output(OutputType.BLUETOOTH, "ZMI PurPods Pro")
    private val fio = Output(OutputType.WIRED, "Fone com fio")
    private val vol15 = Volume(index = 15, max = 30, db = -30f) // 50%
    private val vol18 = Volume(index = 18, max = 30, db = -26f) // 60%

    private fun at(time: String) = Instant.parse("2026-10-21T${time}Z")
    private fun day(n: Int) = LocalDate.of(2026, 10, n)

    private fun span(session: String, from: String, to: String, output: Output = fone, volume: Volume = vol15) =
        Span(session, at(from), at(to), output, volume)

    @Test
    fun `sessão junta os trechos e soma o tempo`() {
        // O volume mudou às 20:10: dois trechos, uma sessão só
        val spans = listOf(span("s1", "20:00:00", "20:10:00"), span("s1", "20:10:00", "20:40:00", volume = vol18))

        val session = spans.toSessions().single()

        assertEquals(at("20:00:00"), session.start)
        assertEquals(at("20:40:00"), session.end)
        assertEquals(Duration.ofMinutes(40), session.listened)
        assertEquals(58, session.volumePercent) // (10 × 50 + 30 × 60) / 40 = 57,5
    }

    @Test
    fun `a saída da sessão é a que tocou mais tempo`() {
        val spans = listOf(span("s1", "20:00:00", "20:05:00", output = fio), span("s1", "20:05:00", "20:30:00"))

        val session = spans.toSessions().single()

        assertEquals(fone, session.output)
    }

    @Test
    fun `o gráfico da sessão tem um degrau por trecho, em ordem`() {
        // O volume subiu de 50% pra 60% às 20:10
        val spans = listOf(span("s1", "20:00:00", "20:10:00"), span("s1", "20:10:00", "20:40:00", volume = vol18))

        val session = spans.toSessions().single()

        assertEquals("s1", session.id)
        assertEquals(listOf(at("20:00:00"), at("20:10:00")), session.steps.map { it.start })
        assertEquals(listOf(50.0, 60.0), session.steps.map { it.percent })
    }

    @Test
    fun `o pico é o volume mais alto, com o tempo que ficou nele`() {
        // 10 min em 50%, 30 min em 60% e mais 5 min em 50%
        val spans = listOf(
            span("s1", "20:00:00", "20:10:00"),
            span("s1", "20:10:00", "20:40:00", volume = vol18),
            span("s1", "20:40:00", "20:45:00"),
        )

        val session = spans.toSessions().single()

        assertEquals(60, session.peakPercent)
        assertEquals(Duration.ofMinutes(30), session.timeAtPeak)
    }

    @Test
    fun `sessões vêm da mais recente pra mais antiga`() {
        val spans = listOf(span("manhã", "07:42:00", "08:16:00"), span("noite", "21:23:00", "21:40:00"))

        val sessions = spans.toSessions()

        assertEquals(listOf(at("21:23:00"), at("07:42:00")), sessions.map { it.start })
    }

    @Test
    fun `média por dia conta zero nos dias sem escuta`() {
        val totals = listOf(DayTotal(day(15), Duration.ofMinutes(70), 50.0), DayTotal(day(20), Duration.ofMinutes(70), 60.0))

        val average = totals.averagePerDay(from = day(15), to = day(21), firstDay = day(1))

        assertEquals(Duration.ofMinutes(20), average) // 140 min em 7 dias
    }

    @Test
    fun `média ignora os dias antes do primeiro registro`() {
        // O app começou a medir no dia 19: só os dias 19, 20 e 21 contam
        val totals = listOf(DayTotal(day(19), Duration.ofMinutes(90), 50.0))

        val average = totals.averagePerDay(from = day(15), to = day(21), firstDay = day(19))

        assertEquals(Duration.ofMinutes(30), average)
    }

    @Test
    fun `sem nenhum registro, não existe média`() {
        assertNull(emptyList<DayTotal>().averagePerDay(from = day(15), to = day(21), firstDay = null))
    }

    @Test
    fun `os dias sem escuta entram com zero`() {
        val filled = listOf(DayTotal(day(20), Duration.ofMinutes(30), 50.0)).fillDays(from = day(19), to = day(21))

        assertEquals(listOf(day(19), day(20), day(21)), filled.map { it.day })
        assertTrue(filled[0].listened.isZero && filled[2].listened.isZero)
    }

    @Test
    fun `volume médio do período pesa pelo tempo de cada dia`() {
        val totals = listOf(DayTotal(day(20), Duration.ofMinutes(30), 50.0), DayTotal(day(21), Duration.ofMinutes(90), 70.0))

        assertEquals(65, totals.averageVolumePercent()) // (30 × 50 + 90 × 70) / 120
    }
}
