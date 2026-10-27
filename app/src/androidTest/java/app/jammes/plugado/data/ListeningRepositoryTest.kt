package app.jammes.plugado.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.jammes.domain.model.AudioKind
import app.jammes.domain.model.Output
import app.jammes.domain.model.OutputType
import app.jammes.domain.model.Segment
import app.jammes.domain.model.Volume
import app.jammes.domain.tracker.TrackerCommand
import app.jammes.plugado.data.db.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Teste instrumentado: roda no emulador (ou no celular), porque o Room precisa do SQLite de
 * verdade. Cada teste usa um banco novo, só em memória.
 * Rode com `./gradlew :app:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class ListeningRepositoryTest {

    private val day = LocalDate.of(2026, 10, 20)
    private val fone = Output(OutputType.BLUETOOTH, "ZMI PurPods Pro")
    private val speaker = Output(OutputType.BLUETOOTH, "JBL Flip 5")

    /** O tempo mínimo dos Ajustes. Começa em zero: os testes antigos não dependem dele. */
    private val minimum = MutableStateFlow(Duration.ZERO)

    private lateinit var db: AppDatabase
    private lateinit var repository: ListeningRepository

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repository = ListeningRepository(db, minimumSession = minimum)
    }

    @AfterTest
    fun tearDown() = db.close()

    private fun at(time: String) = Instant.parse("2026-10-20T${time}Z")

    private fun segment(id: String, startedAt: String, output: Output = fone, volumeDb: Float = -34f) =
        Segment(id, "s1", at(startedAt), day, AudioKind.MEDIA, output, Volume(12, 30, volumeDb))

    private suspend fun open(segment: Segment) = repository.execute(TrackerCommand.Open(segment))

    /** Grava um trecho já fechado. O dia é o do início (os testes usam UTC). */
    private suspend fun listen(session: String, id: String, from: Instant, to: Instant, output: Output = fone) {
        open(Segment(id, session, from, from.atZone(ZoneOffset.UTC).toLocalDate(), AudioKind.MEDIA, output, Volume(12, 30, -34f)))
        repository.execute(TrackerCommand.Close(id, to))
    }

    private suspend fun listened() = repository.listenedOn(day).first()

    @Test
    fun `trecho fechado conta do início ao fim`() = runTest {
        open(segment("t1", "20:00:00"))
        repository.execute(TrackerCommand.Close("t1", at("20:10:00")))

        assertEquals(Duration.ofMinutes(10), listened())
    }

    @Test
    fun `trecho aberto conta até o último heartbeat gravado`() = runTest {
        open(segment("t1", "20:00:00"))
        repository.execute(TrackerCommand.Heartbeat("t1", at("20:00:40")))

        assertEquals(Duration.ofSeconds(40), listened())
    }

    @Test
    fun `heartbeat só é gravado a cada 30 s`() = runTest {
        open(segment("t1", "20:00:00"))
        for (second in 5..35 step 5) { // o tracker manda um a cada tick de 5 s
            repository.execute(TrackerCommand.Heartbeat("t1", at("20:00:%02d".format(second))))
        }

        assertEquals(Duration.ofSeconds(30), listened()) // o de 35 s caiu na folga
    }

    @Test
    fun `sessão descartada some do total`() = runTest {
        open(segment("t1", "20:00:00"))
        repository.execute(TrackerCommand.DiscardSession("s1"))

        assertEquals(Duration.ZERO, listened())
    }

    @Test
    fun `saída marcada como não é fone para de contar inclusive no passado`() = runTest {
        open(segment("t1", "20:00:00", output = speaker))
        repository.execute(TrackerCommand.Close("t1", at("20:10:00")))

        db.devices().setHeadphone(speaker.type, speaker.name, isHeadphone = false)

        assertEquals(Duration.ZERO, listened())
        assertEquals(setOf(speaker), repository.notHeadphones.first())
    }

    @Test
    fun `recuperação fecha o trecho órfão no último heartbeat`() = runTest {
        open(segment("t1", "20:00:00"))
        repository.execute(TrackerCommand.Heartbeat("t1", at("20:05:00")))
        // O processo morreu aqui. O serviço volta com um repositório novo, no mesmo banco.
        val afterRestart = ListeningRepository(db)

        afterRestart.recoverOrphans()

        assertEquals(Duration.ofMinutes(5), listened())
        assertTrue(db.segments().orphans().isEmpty())
    }

    @Test
    fun `recuperação descarta a sessão órfã curta demais`() = runTest {
        open(segment("t1", "20:00:00")) // morreu antes do primeiro heartbeat

        ListeningRepository(db).recoverOrphans()

        assertTrue(db.segments().segmentsOn(day).first().isEmpty())
    }

    @Test
    fun `trechos do dia vêm sem as saídas que não contam`() = runTest {
        open(segment("t1", "20:00:00"))
        repository.execute(TrackerCommand.Close("t1", at("20:10:00")))
        open(segment("t2", "21:00:00", output = speaker))
        repository.execute(TrackerCommand.Close("t2", at("21:30:00")))
        db.devices().setHeadphone(speaker.type, speaker.name, isHeadphone = false)

        val spans = repository.countedSpansOn(day).first()

        assertEquals(listOf(fone), spans.map { it.output })
        assertEquals(Duration.ofMinutes(10), spans.single().duration)
    }

    @Test
    fun `totais por dia somam o tempo e pesam o volume pelo tempo`() = runTest {
        open(segment("t1", "20:00:00")) // volume 12 de 30 = 40%
        repository.execute(TrackerCommand.Close("t1", at("20:10:00")))
        open(Segment("t2", "s1", at("20:10:00"), day, AudioKind.MEDIA, fone, Volume(24, 30, -10f))) // 80%
        repository.execute(TrackerCommand.Close("t2", at("20:40:00")))

        val total = repository.dailyTotals(day.minusDays(6), day).first().single()

        assertEquals(Duration.ofMinutes(40), total.listened)
        assertEquals(70.0, total.volumePercent!!, 0.001) // (10 × 40 + 30 × 80) / 40
    }

    @Test
    fun `cada dispositivo vem com o tempo total dele`() = runTest {
        open(segment("t1", "20:00:00"))
        repository.execute(TrackerCommand.Close("t1", at("20:25:00")))

        val device = repository.devicesWithTotals.first().single()

        assertEquals(fone, device.output)
        assertTrue(device.counts)
        assertEquals(Duration.ofMinutes(25), device.listened)
    }

    @Test
    fun `excluir tudo apaga o histórico e os dispositivos`() = runTest {
        open(segment("t1", "20:00:00"))
        repository.execute(TrackerCommand.Close("t1", at("20:25:00")))

        repository.deleteAll()

        assertEquals(Duration.ZERO, listened())
        assertTrue(repository.devicesWithTotals.first().isEmpty())
        assertNull(repository.firstDay.first())
    }

    @Test
    fun `dB que não pôde ser lido é guardado como nulo`() = runTest {
        open(segment("t1", "20:00:00", volumeDb = Float.NaN))

        assertNull(db.segments().segmentsOn(day).first().single().volumeDb)
    }

    @Test
    fun `sessão mais curta que o mínimo não conta mas continua no banco`() = runTest {
        listen("curta", "t1", at("20:00:00"), at("20:00:40"))
        listen("longa", "t2", at("21:00:00"), at("21:10:00"))

        minimum.value = Duration.ofMinutes(1)

        assertEquals(Duration.ofMinutes(10), listened())
        assertEquals(listOf("longa"), repository.countedSpansOn(day).first().map { it.sessionId })
        assertEquals(Duration.ofMinutes(10), repository.dailyTotals(day, day).first().single().listened)
        assertEquals(2, db.segments().segmentsOn(day).first().size) // a curta só ficou escondida
    }

    @Test
    fun `baixar o mínimo traz a sessão escondida de volta`() = runTest {
        listen("s1", "t1", at("20:00:00"), at("20:01:30"))
        minimum.value = Duration.ofMinutes(2)
        assertEquals(Duration.ZERO, listened())

        minimum.value = Duration.ofMinutes(1)

        assertEquals(Duration.ofSeconds(90), listened())
    }

    @Test
    fun `sessão que passa da meia-noite conta pelo tempo inteiro`() = runTest {
        // 2 min antes da meia-noite e 30 min depois: a sessão tem 32 min, então os 2 min contam
        listen("s1", "t1", at("23:58:00"), Instant.parse("2026-10-21T00:00:00Z"))
        listen("s1", "t2", Instant.parse("2026-10-21T00:00:00Z"), Instant.parse("2026-10-21T00:30:00Z"))

        minimum.value = Duration.ofMinutes(5)

        assertEquals(Duration.ofMinutes(2), listened())
        assertEquals(Duration.ofMinutes(30), repository.listenedOn(day.plusDays(1)).first())
    }

    @Test
    fun `tempo em saída que não é fone não ajuda a sessão a chegar no mínimo`() = runTest {
        // 40 s no fone e depois 10 min na caixa de som, na mesma sessão
        listen("s1", "t1", at("20:00:00"), at("20:00:40"))
        listen("s1", "t2", at("20:00:40"), at("20:10:40"), output = speaker)
        db.devices().setHeadphone(speaker.type, speaker.name, isHeadphone = false)

        minimum.value = Duration.ofMinutes(1)

        assertEquals(Duration.ZERO, listened())
    }

    @Test
    fun `total do dispositivo deixa de fora a sessão curta`() = runTest {
        listen("curta", "t1", at("20:00:00"), at("20:00:40"))
        listen("longa", "t2", at("21:00:00"), at("21:25:00"))

        minimum.value = Duration.ofMinutes(1)

        assertEquals(Duration.ofMinutes(25), repository.devicesWithTotals.first().single().listened)
    }
}
