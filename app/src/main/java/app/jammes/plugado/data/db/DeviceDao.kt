package app.jammes.plugado.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.jammes.domain.model.OutputType
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    /** Se a saída já existe, mantém como está (inclusive a marcação do usuário). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(device: DeviceEntity)

    @Query("UPDATE devices SET isHeadphone = :isHeadphone WHERE type = :type AND name = :name")
    suspend fun setHeadphone(type: OutputType, name: String, isHeadphone: Boolean)

    @Query("SELECT * FROM devices WHERE isHeadphone = 0")
    fun notHeadphones(): Flow<List<DeviceEntity>>

    /**
     * Cada saída com o tempo total dela, da mais usada pra menos usada. A ordem não depende de
     * contar ou não: assim a linha não pula de lugar quando o usuário mexe no interruptor.
     * Sessões mais curtas que [minMillis] ficam de fora. Pelo mesmo motivo, aqui a sessão soma
     * todas as saídas: somando só as que contam, o interruptor mudaria quais sessões entram.
     */
    @Query(
        """
        SELECT d.type AS type, d.name AS name, d.isHeadphone AS isHeadphone,
               COALESCE((
                   SELECT SUM(COALESCE(s.endedAt, s.lastSeenAt) - s.startedAt) FROM segments s
                   WHERE s.outputType = d.type AND s.outputName = d.name AND (
                       SELECT SUM(COALESCE(c.endedAt, c.lastSeenAt) - c.startedAt) FROM segments c
                       WHERE c.sessionId = s.sessionId
                   ) >= :minMillis
               ), 0) AS listenedMillis
        FROM devices d
        ORDER BY listenedMillis DESC, d.name
        """,
    )
    fun devicesWithTotals(minMillis: Long): Flow<List<DeviceTotalRow>>

    @Query("DELETE FROM devices")
    suspend fun deleteAll()
}

/** Uma linha do [DeviceDao.devicesWithTotals]. */
data class DeviceTotalRow(val type: OutputType, val name: String, val isHeadphone: Boolean, val listenedMillis: Long)
