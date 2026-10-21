package app.jammes.plugado.data.db

import androidx.room.Entity
import app.jammes.domain.model.OutputType

/**
 * Uma saída que já tocou alguma vez. Entra como fone, e o usuário pode marcar como
 * "não é fone" (caixa de som, som do carro): aí ela para de contar, inclusive no passado.
 */
@Entity(tableName = "devices", primaryKeys = ["type", "name"])
data class DeviceEntity(
    val type: OutputType,
    val name: String,
    val isHeadphone: Boolean = true,
)
