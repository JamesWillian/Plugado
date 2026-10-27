package app.jammes.plugado.ui.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Android 13+ pede permissão pra mostrar notificações. */
fun Context.needsNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

fun Context.notificationsAllowed(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled()

/** "Sem restrições" na bateria: o sistema não otimiza (nem fecha) o app em segundo plano. */
fun Context.batteryUnrestricted(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

/** A página do app nos ajustes do celular, onde ficam a bateria e as permissões. */
fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Liga o monitoramento pedindo antes a permissão de notificação, se faltar. Liga mesmo se ela
 * for negada: o serviço roda igual, só a notificação não aparece.
 */
@Composable
fun rememberTurnOnMonitoring(turnOn: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentTurnOn by rememberUpdatedState(turnOn)
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        currentTurnOn()
    }
    return remember(context, askPermission) {
        {
            if (context.needsNotificationPermission()) {
                askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                currentTurnOn()
            }
        }
    }
}

/** Um valor relido toda vez que a tela volta pra frente (ex.: depois dos ajustes do celular). */
@Composable
fun <T> rememberOnResume(read: () -> T): T {
    var value by remember { mutableStateOf(read()) }
    LifecycleResumeEffect(Unit) {
        value = read()
        onPauseOrDispose { }
    }
    return value
}
