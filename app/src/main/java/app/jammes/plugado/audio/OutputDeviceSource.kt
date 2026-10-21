package app.jammes.plugado.audio

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Fone conectado ou desconectado. Na troca de aparelho, o volume novo já aparece na leitura
 * feita depois desse callback (o observer de volume nem sempre dispara nessa hora).
 */
class OutputDeviceSource(private val am: AudioManager) : TriggerSource {

    override val triggers: Flow<Unit> = callbackFlow {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) {
                trySend(Unit)
            }

            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) {
                trySend(Unit)
            }
        }
        am.registerAudioDeviceCallback(callback, null) // null = looper principal
        awaitClose { am.unregisterAudioDeviceCallback(callback) }
    }
}
