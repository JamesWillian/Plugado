package app.jammes.plugado.monitor

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.jammes.plugado.R

/** A notificação obrigatória do foreground service. O "hoje: X min" entra na fase 5, com o banco. */
object MonitorNotification {

    const val ID = 1
    private const val CHANNEL_ID = "monitor"

    fun build(context: Context): Notification {
        createChannel(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) }
        val pause = PendingIntent.getService(
            context, 0, MonitorService.stopIntent(context), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentTitle(context.getString(R.string.monitor_notification_title))
            .setContentText(context.getString(R.string.monitor_notification_text))
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.monitor_notification_pause), pause)
            .setOngoing(true)
            // Sem isso, o Android 12+ pode esperar até 10 s pra mostrar a notificação.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.monitor_channel_name))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
