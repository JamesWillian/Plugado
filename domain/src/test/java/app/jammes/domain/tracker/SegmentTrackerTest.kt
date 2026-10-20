package app.jammes.domain.tracker

import app.jammes.domain.model.AudioKind
import app.jammes.domain.model.AudioSnapshot
import app.jammes.domain.model.Output
import app.jammes.domain.model.OutputType
import app.jammes.domain.model.Volume
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SegmentTrackerTest {

    private val zone = ZoneId.of("America/Bahia")
    private val base = LocalDateTime.of(2026, 10, 16, 20, 0).atZone(zone).toInstant()

    private val fone = Output(OutputType.BLUETOOTH, "ZMI PurPods Pro")
    private val vol12 = Volume(index = 12, max = 30, db = -34f)
    private val vol10 = Volume(index = 10, max = 30, db = -39f)

    private var nextId = 0
    private val tracker = SegmentTracker(
        config = TrackerConfig(zone = zone),
        newId = { "id${++nextId}" },
    )

    private fun at(seconds: Double) = base.plusMillis((seconds * 1000).toLong())

    private fun snap(
        seconds: Double,
        playing: Boolean = true,
        output: Output? = fone,
        volume: Volume = vol12,
    ) = AudioSnapshot(at(seconds), playing, AudioKind.MEDIA, output, volume)

    private fun List<TrackerCommand>.singleOpen() = single() as TrackerCommand.Open

    @Test
    fun `app iniciando com música já tocando abre sessão na hora`() {
        val open = tracker.onSnapshot(snap(0.0)).singleOpen()

        assertEquals(at(0.0), open.segment.startedAt)
        assertEquals(vol12, open.segment.volume)
    }

    @Test
    fun `som de conexão de 2s é descartado`() {
        // Log 00:17:02: players UNKNOWN por ~1,8 s ao conectar o fone
        val open = tracker.onSnapshot(snap(0.0)).singleOpen()
        assertTrue(tracker.onSnapshot(snap(1.8, playing = false)).isEmpty())

        val result = tracker.onTick(at(10.0))

        assertEquals(listOf(TrackerCommand.DiscardSession(open.segment.sessionId)), result)
    }

    @Test
    fun `troca de faixa não quebra o trecho`() {
        // Log 23:38:27: lista de players esvazia e volta 180 ms depois
        val open = tracker.onSnapshot(snap(0.0)).singleOpen()
        assertTrue(tracker.onSnapshot(snap(60.0, playing = false)).isEmpty())
        assertTrue(tracker.onSnapshot(snap(60.2)).isEmpty())

        val tick = tracker.onTick(at(120.0))

        assertEquals(listOf(TrackerCommand.Heartbeat(open.segment.id, at(120.0))), tick)
    }

    @Test
    fun `pausa longa fecha o trecho no momento da pausa`() {
        val open = tracker.onSnapshot(snap(0.0)).singleOpen()
        tracker.onSnapshot(snap(120.0, playing = false))

        val result = tracker.onTick(at(130.0))

        assertEquals(listOf(TrackerCommand.Close(open.segment.id, at(120.0))), result)
    }

    @Test
    fun `mudar o volume abre um trecho novo na mesma sessão`() {
        val first = tracker.onSnapshot(snap(0.0)).singleOpen()

        val result = tracker.onSnapshot(snap(60.0, volume = vol10))

        assertEquals(2, result.size)
        assertEquals(TrackerCommand.Close(first.segment.id, at(60.0)), result[0])
        val second = result[1] as TrackerCommand.Open
        assertEquals(first.segment.sessionId, second.segment.sessionId)
        assertEquals(at(60.0), second.segment.startedAt)
        assertEquals(vol10, second.segment.volume)
    }

    @Test
    fun `tirar o fone fecha quando a música para, não quando o fone some`() {
        // Log 23:54:08: a música para 0,7 s antes do A2DP desconectar
        val open = tracker.onSnapshot(snap(0.0)).singleOpen()
        tracker.onSnapshot(snap(300.0, playing = false))
        tracker.onSnapshot(snap(300.7, playing = false, output = null))

        val result = tracker.onTick(at(310.0))

        assertEquals(listOf(TrackerCommand.Close(open.segment.id, at(300.0))), result)
    }

    @Test
    fun `música no alto-falante não conta`() {
        // Log 00:16:20: isMusicActive true sem nenhuma saída de fone
        assertTrue(tracker.onSnapshot(snap(0.0, output = null)).isEmpty())
        assertTrue(tracker.onTick(at(60.0)).isEmpty())
    }

    @Test
    fun `sessão que atravessa a meia-noite é dividida`() {
        val beforeMidnight = LocalDateTime.of(2026, 10, 16, 23, 59).atZone(zone).toInstant()
        val midnight = LocalDate.of(2026, 10, 17).atStartOfDay(zone).toInstant()
        val first = tracker.onSnapshot(
            AudioSnapshot(beforeMidnight, true, AudioKind.MEDIA, fone, vol12),
        ).singleOpen()

        val result = tracker.onTick(midnight.plusSeconds(60))

        assertEquals(TrackerCommand.Close(first.segment.id, midnight), result[0])
        val second = result[1] as TrackerCommand.Open
        assertEquals(midnight, second.segment.startedAt)
        assertEquals(LocalDate.of(2026, 10, 17), second.segment.day)
        assertIs<TrackerCommand.Heartbeat>(result[2])
    }

    @Test
    fun `desligar o monitoramento fecha a sessão aberta`() {
        val open = tracker.onSnapshot(snap(0.0)).singleOpen()

        val result = tracker.stop(at(90.0))

        assertEquals(listOf(TrackerCommand.Close(open.segment.id, at(90.0))), result)
    }
}