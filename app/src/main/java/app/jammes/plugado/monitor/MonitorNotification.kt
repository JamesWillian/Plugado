package app.jammes.plugado.monitor

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.jammes.plugado.R

/** A notificação obrigatória do foreground service, com o tempo de fone de hoje. */
object MonitorNotification {

    const val ID = 1
    private const val CHANNEL_ID = "monitor"

    /** Sem [minutesToday] (antes da primeira leitura do banco), mostra um texto genérico. */
    fun build(context: Context, minutesToday: Long? = null): Notification {
        createChannel(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) }
        val pause = PendingIntent.getService(
            context, 0, MonitorService.stopIntent(context), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_plugado)
            .setContentTitle(context.getString(R.string.monitor_notification_title))
            .setContentText(
                minutesToday?.let { todayText(context, it) }
                    ?: context.getString(R.string.monitor_notification_text),
            )
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.monitor_notification_pause), pause)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Sem isso, o Android 12+ pode esperar até 10 s pra mostrar a notificação.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /** Atualiza o "hoje". Sem a permissão de notificação, ela nem aparece, então não tem o que atualizar. */
    fun update(context: Context, minutesToday: Long) {
        val allowed = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(context).notify(ID, build(context, minutesToday))
    }

    private fun todayText(context: Context, minutes: Long): String =
        if (minutes < 60) {
            context.getString(R.string.monitor_today_minutes, minutes)
        } else {
            context.getString(R.string.monitor_today_hours, minutes / 60, minutes % 60)
        }

    private fun createChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.monitor_channel_name))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
