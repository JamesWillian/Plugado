package app.jammes.plugado.audio

import app.jammes.domain.model.AudioKind
import app.jammes.domain.model.AudioSnapshot
import app.jammes.domain.model.Output
import app.jammes.domain.model.OutputType
import app.jammes.domain.model.Volume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Roda em tempo virtual: o `runTest` controla o relógio, então "esperar 5 s" é instantâneo
 * e dá sempre o mesmo resultado. O relógio começa em 0 ms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AudioStateAggregatorTest {

    private val fone = Output(OutputType.BLUETOOTH, "ZMI PurPods Pro")
    private val vol12 = Volume(index = 12, max = 30, db = -34f)

    // O "aparelho" do teste: o leitor falso devolve esse estado e conta as leituras.
    private var playing = true
    private var output: Output? = fone
    private var reads = 0
    private val reader = AudioStateReader { at, notHeadphones ->
        reads++
        AudioSnapshot(at, playing, AudioKind.MEDIA, output?.takeUnless { it in notHeadphones }, vol12)
    }

    private val callbacks = Channel<Unit>(Channel.UNLIMITED)
    private val notHeadphones = MutableStateFlow(emptySet<Output>())
    private val emitted = mutableListOf<AudioSnapshot>()

    private fun TestScope.start() {
        val aggregator = AudioStateAggregator(
            triggers = callbacks.receiveAsFlow(),
            reader = reader,
            notHeadphones = notHeadphones,
            now = { ms(currentTime) },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            aggregator.snapshots.toList(emitted)
        }
    }

    /** Uma fonte avisou que algo mudou. */
    private fun callback() {
        callbacks.trySend(Unit)
    }

    /** Avança o relógio até [time] ms e roda tudo que estava agendado até ali. */
    private fun TestScope.advanceTo(time: Long) {
        advanceTimeBy(time - currentTime)
        runCurrent()
    }

    private fun ms(time: Long) = Instant.ofEpochMilli(time)

    @Test
    fun `lê o estado assim que começa, pra pegar música que já está tocando`() = runTest {
        start()

        advanceTo(400) // o debounce segura a primeira leitura por 400 ms

        assertTrue(emitted.single().isListening)
        assertEquals(ms(400), emitted.single().at)
    }

    @Test
    fun `pause sem callback é pego pela releitura`() = runTest {
        // Spike: o callback do pause chega com o isMusicActive ainda true
        start()
        advanceTo(1_000)
        callback() // leitura em 1,4 s ainda vê tocando
        advanceTo(1_700)
        playing = false // o isMusicActive cai 0,7 s depois, sem callback

        advanceTo(2_900) // releitura: 1,4 s + 1,5 s

        assertFalse(emitted.last().playing)
        assertEquals(ms(2_900), emitted.last().at)
    }

    @Test
    fun `polling pega a pausa mesmo sem callback nenhum`() = runTest {
        start()
        advanceTo(3_000)
        playing = false // virou sem nenhum callback

        advanceTo(5_400) // primeira volta do polling: 0,4 s + 5 s

        assertFalse(emitted.last().playing)
        assertEquals(ms(5_400), emitted.last().at)
    }

    @Test
    fun `sem ninguém ouvindo, não fica relendo`() = runTest {
        output = null // música no alto-falante
        start()
        advanceTo(2_000) // leitura inicial (0,4 s) e releitura (1,9 s)
        val readsBefore = reads

        advanceTo(60_000)

        assertEquals(readsBefore, reads)
    }

    @Test
    fun `rajada de callbacks vira uma leitura só`() = runTest {
        output = null // sem polling, pra contar só as leituras dos callbacks
        start()
        advanceTo(3_000)
        val readsBefore = reads

        repeat(5) { // 5 callbacks em 200 ms, como na troca de faixa
            callback()
            advanceTo(currentTime + 50)
        }
        advanceTo(3_700) // leitura em 3,2 s + 0,4 s

        assertEquals(readsBefore + 1, reads)
    }

    @Test
    fun `estado igual não é emitido de novo, mesmo lido em outro horário`() = runTest {
        start()
        repeat(3) {
            advanceTo(currentTime + 2_000)
            callback()
        }

        advanceTo(20_000)

        assertTrue(reads > 1)
        assertEquals(1, emitted.size)
    }

    @Test
    fun `marcar a saída como não é fone relê na hora`() = runTest {
        start()
        advanceTo(3_000)

        notHeadphones.value = setOf(fone) // o usuário marcou: é caixa de som
        runCurrent()

        assertNull(emitted.last().output)
        assertEquals(ms(3_000), emitted.last().at)
    }
}
