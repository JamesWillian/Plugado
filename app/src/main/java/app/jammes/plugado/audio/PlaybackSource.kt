package app.jammes.plugado.audio

import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Play, pause e troca de faixa de qualquer app.
 * A lista de players é ignorada: ela oscila e vem anonimizada (a saída de cada player vem null).
 */
class PlaybackSource(private val am: AudioManager) : TriggerSource {

    override val triggers: Flow<Unit> = callbackFlow {
        val callback = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>?) {
                trySend(Unit)
            }
        }
        am.registerAudioPlaybackCallback(callback, null) // null = looper principal
        awaitClose { am.unregisterAudioPlaybackCallback(callback) }
    }
}
