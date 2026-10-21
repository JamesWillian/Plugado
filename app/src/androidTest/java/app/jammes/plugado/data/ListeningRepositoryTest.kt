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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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

    private lateinit var db: AppDatabase
    private lateinit var repository: ListeningRepository

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repository = ListeningRepository(db)
    }

    @AfterTest
    fun tearDown() = db.close()

    private fun at(time: String) = Instant.parse("2026-10-20T${time}Z")

    private fun segment(id: String, startedAt: String, output: Output = fone, volumeDb: Float = -34f) =
        Segment(id, "s1", at(startedAt), day, AudioKind.MEDIA, output, Volume(12, 30, volumeDb))

    private suspend fun open(segment: Segment) = repository.execute(TrackerCommand.Open(segment))

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
        val speaker = Output(OutputType.BLUETOOTH, "JBL Flip 5")
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
    fun `dB que não pôde ser lido é guardado como nulo`() = runTest {
        open(segment("t1", "20:00:00", volumeDb = Float.NaN))

        assertNull(db.segments().segmentsOn(day).first().single().volumeDb)
    }
}
