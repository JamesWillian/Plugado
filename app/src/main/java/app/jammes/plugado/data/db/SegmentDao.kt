package app.jammes.plugado.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.jammes.domain.tracker.OrphanSegment
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * O trecho `s` é de uma sessão que chega no tempo mínimo (`:minMillis`). A sessão soma os trechos
 * dela em todos os dias (a que passa da meia-noite conta pelo tempo inteiro), só nas saídas que
 * contam. A sessão curta continua no banco e volta a contar se o mínimo baixar.
 */
private const val SESSION_REACHES_MINIMUM = """
    (SELECT SUM(COALESCE(c.endedAt, c.lastSeenAt) - c.startedAt) FROM segments c
     WHERE c.sessionId = s.sessionId AND NOT EXISTS (
         SELECT 1 FROM devices d
         WHERE d.type = c.outputType AND d.name = c.outputName AND d.isHeadphone = 0
     )) >= :minMillis
"""

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
     * Saídas marcadas como "não é fone" e sessões mais curtas que [minMillis] não contam.
     */
    @Query(
        """
        SELECT COALESCE(SUM(COALESCE(s.endedAt, s.lastSeenAt) - s.startedAt), 0)
        FROM segments s
        WHERE s.day = :day AND NOT EXISTS (
            SELECT 1 FROM devices d
            WHERE d.type = s.outputType AND d.name = s.outputName AND d.isHeadphone = 0
        ) AND $SESSION_REACHES_MINIMUM
        """,
    )
    fun listenedMillis(day: LocalDate, minMillis: Long): Flow<Long>

    /** Os trechos do dia, em ordem. */
    @Query("SELECT * FROM segments WHERE day = :day ORDER BY startedAt")
    fun segmentsOn(day: LocalDate): Flow<List<SegmentEntity>>

    /**
     * Os trechos do dia que contam (sem as saídas marcadas como "não é fone" e sem as sessões
     * mais curtas que [minMillis]), em ordem.
     */
    @Query(
        """
        SELECT s.* FROM segments s
        WHERE s.day = :day AND NOT EXISTS (
            SELECT 1 FROM devices d
            WHERE d.type = s.outputType AND d.name = s.outputName AND d.isHeadphone = 0
        ) AND $SESSION_REACHES_MINIMUM
        ORDER BY s.startedAt
        """,
    )
    fun countedSegmentsOn(day: LocalDate, minMillis: Long): Flow<List<SegmentEntity>>

    /**
     * Tempo ouvido e volume de cada dia com escuta entre [from] e [to], sem as saídas que não
     * contam e sem as sessões mais curtas que [minMillis].
     */
    @Query(
        """
        SELECT s.day AS day,
               SUM(COALESCE(s.endedAt, s.lastSeenAt) - s.startedAt) AS listenedMillis,
               SUM((COALESCE(s.endedAt, s.lastSeenAt) - s.startedAt) * 100.0 * s.volumeIndex / s.volumeMax)
                   AS volumePercentMillis
        FROM segments s
        WHERE s.day BETWEEN :from AND :to AND NOT EXISTS (
            SELECT 1 FROM devices d
            WHERE d.type = s.outputType AND d.name = s.outputName AND d.isHeadphone = 0
        ) AND $SESSION_REACHES_MINIMUM
        GROUP BY s.day
        """,
    )
    fun dailyTotals(from: LocalDate, to: LocalDate, minMillis: Long): Flow<List<DayTotalRow>>

    /** O primeiro dia com algum registro. Antes dele, o app ainda não estava medindo. */
    @Query("SELECT MIN(day) FROM segments")
    fun firstDay(): Flow<LocalDate?>

    @Query("DELETE FROM segments")
    suspend fun deleteAll()
}

/** Uma linha do [SegmentDao.dailyTotals]. O volume vem multiplicado pelo tempo, pra média ponderada. */
data class DayTotalRow(val day: LocalDate, val listenedMillis: Long, val volumePercentMillis: Double)
