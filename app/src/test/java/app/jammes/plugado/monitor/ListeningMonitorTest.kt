package app.jammes.plugado.monitor

import app.jammes.domain.model.AudioKind
import app.jammes.domain.model.AudioSnapshot
import app.jammes.domain.model.Output
import app.jammes.domain.model.OutputType
import app.jammes.domain.model.Volume
import app.jammes.domain.tracker.SegmentTracker
import app.jammes.domain.tracker.TrackerCommand
import app.jammes.domain.tracker.TrackerConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Tempo virtual, como no AudioStateAggregatorTest. O relógio começa em 0 ms. */
@OptIn(ExperimentalCoroutinesApi::class)
class ListeningMonitorTest {

    private val fone = Output(OutputType.BLUETOOTH, "ZMI PurPods Pro")
    private val vol12 = Volume(index = 12, max = 30, db = -34f)

    private var nextId = 0
    private val tracker = SegmentTracker(
        config = TrackerConfig(zone = ZoneOffset.UTC),
        newId = { "id${++nextId}" },
    )

    private val snapshots = Channel<AudioSnapshot>(Channel.UNLIMITED)
    private val executed = mutableListOf<TrackerCommand>() // o que chegou no sink
    private val seen = mutableListOf<AudioSnapshot>() // o que o listener (o log) viu

    private val listener = object : MonitorListener {
        override fun onSnapshot(snapshot: AudioSnapshot) {
            seen += snapshot
        }
    }

    private fun TestScope.start(): Job {
        val monitor = ListeningMonitor(
            snapshots = snapshots.receiveAsFlow(),
            tracker = tracker,
            sink = CommandSink { executed += it },
            listener = listener,
            now = { ms(currentTime) },
        )
        return backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { monitor.run() }
    }

    /** O agregador mandou um snapshot agora. */
    private fun TestScope.send(playing: Boolean) {
        snapshots.trySend(AudioSnapshot(ms(currentTime), playing, AudioKind.MEDIA, fone, vol12))
        runCurrent()
    }

    private fun TestScope.advanceTo(time: Long) {
        advanceTimeBy(time - currentTime)
        runCurrent()
    }

    private fun ms(time: Long) = Instant.ofEpochMilli(time)

    private fun openedSegmentId() = (executed.first() as TrackerCommand.Open).segment.id

    @Test
    fun `snapshot passa pelo tracker e o comando vai pro sink`() = runTest {
        start()

        send(playing = true)

        assertIs<TrackerCommand.Open>(executed.single())
        assertEquals(1, seen.size)
    }

    @Test
    fun `com sessão aberta, um tick a cada 5 s gera heartbeat`() = runTest {
        start()
        send(playing = true)

        advanceTo(12_000)

        val id = openedSegmentId()
        assertEquals(
            listOf(TrackerCommand.Heartbeat(id, ms(5_000)), TrackerCommand.Heartbeat(id, ms(10_000))),
            executed.drop(1),
        )
    }

    @Test
    fun `pausa longa é fechada pelo tick, sem precisar de outro snapshot`() = runTest {
        start()
        send(playing = true)
        advanceTo(60_000)
        send(playing = false)

        advanceTo(70_000) // o tick de 65 s ainda está dentro dos 5 s; o de 70 s expira a pausa

        assertEquals(TrackerCommand.Close(openedSegmentId(), ms(60_000)), executed.last())
    }

    @Test
    fun `o status diz se tem alguém ouvindo agora`() = runTest {
        val status = MonitorStatus()
        val monitor = ListeningMonitor(
            snapshots = snapshots.receiveAsFlow(),
            tracker = tracker,
            sink = CommandSink { executed += it },
            status = status,
            now = { ms(currentTime) },
        )
        val monitoring = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { monitor.run() }

        send(playing = true)
        val whilePlaying = status.listening.value
        send(playing = false)
        val whilePaused = status.listening.value
        send(playing = true)
        monitoring.cancelAndJoin()

        assertEquals(listOf(true, false, false), listOf(whilePlaying, whilePaused, status.listening.value))
    }

    @Test
    fun `desligar o monitor fecha a sessão aberta`() = runTest {
        val monitoring = start()
        send(playing = true)
        advanceTo(90_000)

        monitoring.cancelAndJoin()

        assertEquals(TrackerCommand.Close(openedSegmentId(), ms(90_000)), executed.last())
    }
}
