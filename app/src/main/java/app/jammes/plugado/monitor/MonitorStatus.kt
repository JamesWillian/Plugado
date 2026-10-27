package app.jammes.plugado.monitor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Se tem alguém ouvindo agora, pra UI ("Ouvindo agora"). Vive na memória do processo: se ele
 * morrer, volta a false até o próximo snapshot.
 */
class MonitorStatus {
    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    internal fun setListening(value: Boolean) {
        _listening.value = value
    }
}
