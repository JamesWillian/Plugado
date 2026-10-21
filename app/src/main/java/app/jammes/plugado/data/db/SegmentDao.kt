package app.jammes.plugado.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.jammes.domain.tracker.OrphanSegment
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentDao {

    @Insert
    suspend fun insert(segment: SegmentEntity)

    @Query("UPDATE segments SET endedAt = :endedAt WHERE id = :id")
    suspend fun close(id: String, endedAt: Instant)

    @Query("UPDATE segments SET lastSeenAt = :at WHERE id = :id")
    suspend fun updateLastSeen(id: String, at: Instant)

    @Query("DELETE FROM segments WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    /** Trechos que ficaram abertos, cada um com o início da sua sessão. */
    @Query(
        """
        SELECT s.id AS segmentId, s.sessionId, s.lastSeenAt,
               (SELECT MIN(f.startedAt) FROM segments f WHERE f.sessionId = s.sessionId) AS sessionStartedAt
        FROM segments s
        WHERE s.endedAt IS NULL
        """,
    )
    suspend fun orphans(): List<OrphanSegment>

    /**
     * Milissegundos ouvidos no dia. O trecho aberto conta até o último heartbeat.
     * Saídas marcadas como "não é fone" não contam.
     */
    @Query(
        """
        SELECT COALESCE(SUM(COALESCE(s.endedAt, s.lastSeenAt) - s.startedAt), 0)
        FROM segments s
        WHERE s.day = :day AND NOT EXISTS (
            SELECT 1 FROM devices d
            WHERE d.type = s.outputType AND d.name = s.outputName AND d.isHeadphone = 0
        )
        """,
    )
    fun listenedMillis(day: LocalDate): Flow<Long>

    /** Os trechos do dia, em ordem. Pra linha do tempo da tela de hoje. */
    @Query("SELECT * FROM segments WHERE day = :day ORDER BY startedAt")
    fun segmentsOn(day: LocalDate): Flow<List<SegmentEntity>>
}
