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
}
