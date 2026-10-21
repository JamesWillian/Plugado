package app.jammes.plugado.audio

import android.media.AudioDeviceInfo
import android.media.AudioManager
import app.jammes.domain.model.AudioKind
import app.jammes.domain.model.Output
import app.jammes.domain.model.OutputType
import app.jammes.domain.model.Volume
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnapshotRulesTest {

    private val at = Instant.parse("2026-10-20T23:00:00Z")

    private val a2dp = OutputDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "ZMI PurPods Pro")
    private val sco = OutputDevice(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, "ZMI PurPods Pro")
    private val fm = OutputDevice(AudioDeviceInfo.TYPE_FM, "2412DPC0AG") // nome do modelo do celular
    private val wired = OutputDevice(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, "Headphones")

    private val fone = Output(OutputType.BLUETOOTH, "ZMI PurPods Pro")

    // Volume falso: anota de qual stream e de qual saída o dB foi pedido.
    private val volumeAsked = mutableListOf<Pair<Int, Int>>()
    private val volumes = VolumeLookup { stream, deviceType ->
        volumeAsked += stream to deviceType
        Volume(index = 12, max = 30, db = -34f)
    }

    private fun read(
        vararg outputs: OutputDevice,
        musicActive: Boolean = true,
        mode: Int = AudioManager.MODE_NORMAL,
        notHeadphones: Set<Output> = emptySet(),
    ) = AudioReading(musicActive, mode, outputs.toList()).toSnapshot(at, notHeadphones, volumes)

    @Test
    fun `música no fone bluetooth conta, com o nome do fone`() {
        val snapshot = read(sco, a2dp)

        assertTrue(snapshot.isListening)
        assertEquals(AudioKind.MEDIA, snapshot.kind)
        assertEquals(fone, snapshot.output)
    }

    @Test
    fun `música no alto-falante não tem saída`() {
        // Log 00:16:20: isMusicActive true sem nenhuma saída de fone
        val snapshot = read()

        assertTrue(snapshot.playing)
        assertNull(snapshot.output)
        assertFalse(snapshot.isListening)
    }

    @Test
    fun `saída do rádio FM é ignorada`() {
        // Spike: o TYPE_FM aparece como saída, com o nome do modelo do celular
        val snapshot = read(fm)

        assertNull(snapshot.output)
    }

    @Test
    fun `fone com fio conta`() {
        val snapshot = read(wired)

        assertEquals(Output(OutputType.WIRED, "Headphones"), snapshot.output)
    }

    @Test
    fun `dB da mídia vem do A2DP, não do SCO`() {
        // O spike errou isso na primeira versão. Na reconexão o SCO chega antes, então vem primeiro.
        read(sco, a2dp)

        assertEquals(
            listOf(AudioManager.STREAM_MUSIC to AudioDeviceInfo.TYPE_BLUETOOTH_A2DP),
            volumeAsked,
        )
    }

    @Test
    fun `chamada conta mesmo sem isMusicActive e usa o volume de chamada`() {
        val snapshot = read(sco, a2dp, musicActive = false, mode = AudioManager.MODE_IN_COMMUNICATION)

        assertTrue(snapshot.isListening)
        assertEquals(AudioKind.CALL, snapshot.kind)
        assertEquals(
            listOf(AudioManager.STREAM_VOICE_CALL to AudioDeviceInfo.TYPE_BLUETOOTH_SCO),
            volumeAsked,
        )
    }

    @Test
    fun `A2DP e SCO do mesmo fone viram a mesma saída`() {
        // Assim, uma ligação no meio da música troca só o tipo do trecho, não o aparelho.
        val music = read(sco, a2dp)
        val call = read(sco, a2dp, mode = AudioManager.MODE_IN_CALL)

        assertEquals(music.output, call.output)
    }

    @Test
    fun `saída marcada como não é fone não conta`() {
        val speaker = OutputDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "JBL Flip 5")

        val snapshot = read(speaker, notHeadphones = setOf(Output(OutputType.BLUETOOTH, "JBL Flip 5")))

        assertNull(snapshot.output)
    }

    // Exercício do James (veja o CLAUDE.md): SCO sozinho não conta pra mídia.
}
