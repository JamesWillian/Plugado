package app.jammes.plugado.audio

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Volume mudou. O observer junta os degraus de um aperto num aviso só.
 * O `VOLUME_CHANGED_ACTION` chega antes, mas não é documentado, então fica de fora.
 * Dispara também pra outras configurações do sistema; leituras repetidas são descartadas no agregador.
 */
class VolumeSource(private val resolver: ContentResolver) : TriggerSource {

    override val triggers: Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }
}
