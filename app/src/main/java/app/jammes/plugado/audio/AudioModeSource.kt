package app.jammes.plugado.audio

import android.media.AudioManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executor

/** Ligação e VoIP (`IN_CALL`, `IN_COMMUNICATION`). Esse listener é o motivo do minSdk 31. */
class AudioModeSource(
    private val am: AudioManager,
    private val executor: Executor,
) : TriggerSource {

    override val triggers: Flow<Unit> = callbackFlow {
        val listener = AudioManager.OnModeChangedListener { trySend(Unit) }
        am.addOnModeChangedListener(executor, listener)
        awaitClose { am.removeOnModeChangedListener(listener) }
    }
}
