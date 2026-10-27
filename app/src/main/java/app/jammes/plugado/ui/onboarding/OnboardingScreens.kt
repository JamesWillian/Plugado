package app.jammes.plugado.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jammes.plugado.R
import app.jammes.plugado.ui.common.CompactButton
import app.jammes.plugado.ui.common.IconBadge
import app.jammes.plugado.ui.common.PlugadoLogo
import app.jammes.plugado.ui.common.PrimaryButton
import app.jammes.plugado.ui.common.QuietButton
import app.jammes.plugado.ui.common.batteryUnrestricted
import app.jammes.plugado.ui.common.findActivity
import app.jammes.plugado.ui.common.needsNotificationPermission
import app.jammes.plugado.ui.common.notificationsAllowed
import app.jammes.plugado.ui.common.openAppSettings
import app.jammes.plugado.ui.common.openNotificationSettings
import app.jammes.plugado.ui.common.rememberOnResume
import app.jammes.plugado.ui.theme.PlugadoIcons
import app.jammes.plugado.ui.theme.PlugadoTheme
import org.koin.compose.viewmodel.koinViewModel

/** Primeiro uso: boas-vindas e os dois ajustes. Aparece uma vez só. */
@Composable
fun OnboardingFlow(viewModel: OnboardingViewModel = koinViewModel()) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    if (step == 0) {
        WelcomeScreen(onStart = { step = 1 })
    } else {
        BackHandler { step = 0 }
        SetupScreen(
            onBack = { step = 0 },
            onTurnOn = { viewModel.finish(turnOn = true) },
            onNotNow = { viewModel.finish(turnOn = false) },
        )
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val colors = PlugadoTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlugadoLogo()
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text,
                )
            }
            Text(
                stringResource(R.string.welcome_title),
                modifier = Modifier.padding(top = 48.dp),
                style = MaterialTheme.typography.displaySmall,
                color = colors.text,
            )
            Text(
                stringResource(R.string.welcome_body),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted,
            )
            Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Point(PlugadoIcons.Clock, stringResource(R.string.welcome_point_time))
                Point(PlugadoIcons.Volume, stringResource(R.string.welcome_point_volume))
                Point(PlugadoIcons.Lock, stringResource(R.string.welcome_point_private))
            }
        }
        PrimaryButton(stringResource(R.string.welcome_start), onClick = onStart, modifier = Modifier.padding(top = 24.dp))
        Text(
            stringResource(R.string.welcome_footer),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            style = MaterialTheme.typography.labelMedium,
            color = colors.subtle,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Point(icon: ImageVector, text: String) {
    val colors = PlugadoTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = colors.text)
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp), color = colors.text)
    }
}

@Composable
fun SetupScreen(onBack: () -> Unit, onTurnOn: () -> Unit, onNotNow: () -> Unit) {
    val colors = PlugadoTheme.colors
    val context = LocalContext.current
    val notificationsOk = rememberOnResume { context.notificationsAllowed() }
    val batteryOk = rememberOnResume { context.batteryUnrestricted() }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { askedOnce = true }

    fun allowNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && context.needsNotificationPermission()) {
            val activity = context.findActivity()
            val systemWillAsk = activity == null || !askedOnce ||
                activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            if (systemWillAsk) {
                askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        // Negada de vez, ou desligada à mão: só dá pra ligar pelos ajustes do celular.
        context.openNotificationSettings()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(PlugadoIcons.ChevronLeft, contentDescription = stringResource(R.string.action_back), tint = colors.text)
            }
            Text(
                stringResource(R.string.setup_title),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.text,
            )
            Text(
                stringResource(R.string.setup_body),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted,
            )
            Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SetupCard(
                    icon = PlugadoIcons.Bell,
                    title = stringResource(R.string.setup_notifications_title),
                    body = stringResource(R.string.setup_notifications_body),
                    done = notificationsOk,
                    doneLabel = stringResource(R.string.setup_notifications_done),
                    action = stringResource(R.string.setup_notifications_action),
                    onAction = ::allowNotifications,
                )
                SetupCard(
                    icon = PlugadoIcons.Battery,
                    title = stringResource(R.string.setup_battery_title),
                    body = stringResource(R.string.setup_battery_body),
                    done = batteryOk,
                    doneLabel = stringResource(R.string.setup_battery_done),
                    action = stringResource(R.string.setup_battery_action),
                    onAction = { context.openAppSettings() },
                )
            }
        }
        PrimaryButton(stringResource(R.string.setup_turn_on), onClick = onTurnOn, modifier = Modifier.padding(top = 16.dp))
        QuietButton(stringResource(R.string.setup_not_now), onClick = onNotNow, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SetupCard(
    icon: ImageVector,
    title: String,
    body: String,
    done: Boolean,
    doneLabel: String,
    action: String,
    onAction: () -> Unit,
) {
    val colors = PlugadoTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(icon)
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 24.sp), color = colors.text)
        }
        Text(body, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp), color = colors.muted)
        if (done) {
            Row(Modifier.height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(PlugadoIcons.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.text)
                Text(doneLabel, style = MaterialTheme.typography.labelLarge, color = colors.text)
            }
        } else {
            CompactButton(action, onClick = onAction, emphasized = false)
        }
    }
}
