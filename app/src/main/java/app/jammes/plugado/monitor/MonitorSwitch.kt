package app.jammes.plugado.monitor

import android.content.Context
import app.jammes.plugado.data.prefs.MonitorSettings
import kotlinx.coroutines.flow.first

/** O interruptor do monitoramento: liga ou desliga o serviço e lembra a escolha. É o que a UI chama. */
class MonitorSwitch(
    private val context: Context,
    private val settings: MonitorSettings,
) {
    suspend fun turnOn() {
        settings.setEnabled(true)
        MonitorService.start(context)
    }

    suspend fun turnOff() {
        settings.setEnabled(false)
        MonitorService.stop(context)
    }

    /** Depois de reiniciar o celular ou atualizar o app: volta se estava ligado. */
    suspend fun restoreIfEnabled() {
        if (settings.enabled.first()) MonitorService.start(context)
    }
}
