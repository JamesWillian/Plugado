package app.jammes.plugado.monitor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

/**
 * Foreground service "burro": fica em primeiro plano e hospeda o [ListeningMonitor].
 * Sem ele, o Android congela o processo em segundo plano e os eventos chegam represados.
 */
class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoring: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !goForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (monitoring == null) {
            val monitor: ListeningMonitor = get()
            monitoring = scope.launch { monitor.run() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel() // o run() fecha a sessão aberta antes de terminar
        super.onDestroy()
    }

    private fun goForeground(): Boolean = try {
        // O tipo specialUse só existe a partir da API 34. Antes disso, o tipo não é exigido.
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, MonitorNotification.ID, MonitorNotification.build(this), type)
        true
    } catch (e: IllegalStateException) {
        // ForegroundServiceStartNotAllowedException: o sistema não deixou ligar em segundo plano
        // (por exemplo, numa reinicialização pelo START_STICKY).
        Log.w(TAG, "startForeground recusado", e)
        false
    }

    companion object {
        private const val TAG = "Plugado"
        private const val ACTION_STOP = "app.jammes.plugado.action.STOP_MONITOR"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }

        /** Usado pela ação "Pausar" da notificação. */
        fun stopIntent(context: Context): Intent =
            Intent(context, MonitorService::class.java).setAction(ACTION_STOP)
    }
}
