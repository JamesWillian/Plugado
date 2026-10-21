package app.jammes.domain.tracker

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class OrphanRecoveryTest {

    private fun at(time: String) = Instant.parse("2026-10-20T${time}Z")

    @Test
    fun `trecho órfão fecha no último heartbeat`() {
        val orphan = OrphanSegment("t1", "s1", sessionStartedAt = at("20:00:00"), lastSeenAt = at("20:30:00"))

        val command = orphan.recover()

        assertEquals(TrackerCommand.Close("t1", at("20:30:00")), command)
    }

    @Test
    fun `sessão órfã curta demais é descartada`() {
        // O processo morreu 4 s depois do som de conexão do fone
        val orphan = OrphanSegment("t1", "s1", sessionStartedAt = at("20:00:00"), lastSeenAt = at("20:00:04"))

        val command = orphan.recover()

        assertEquals(TrackerCommand.DiscardSession("s1"), command)
    }

    @Test
    fun `a duração conta desde o primeiro trecho da sessão, não só o órfão`() {
        // O volume mudou às 20:10 (trecho novo) e o processo morreu 3 s depois
        val orphan = OrphanSegment("t2", "s1", sessionStartedAt = at("20:00:00"), lastSeenAt = at("20:10:03"))

        val command = orphan.recover()

        assertEquals(TrackerCommand.Close("t2", at("20:10:03")), command)
    }
}
