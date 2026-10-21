package app.jammes.plugado.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Volta a monitorar depois de reiniciar o celular (`BOOT_COMPLETED`) ou de atualizar o app
 * (`MY_PACKAGE_REPLACED`), se estava ligado. Os dois estão na lista de exceções que deixam
 * ligar um foreground service a partir do segundo plano.
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val switch: MonitorSwitch by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        // Ler o DataStore é assíncrono; o goAsync segura o broadcast até terminar.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                switch.restoreIfEnabled()
            } finally {
                pending.finish()
            }
        }
    }
}
