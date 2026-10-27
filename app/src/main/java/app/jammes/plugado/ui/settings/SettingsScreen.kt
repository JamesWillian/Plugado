package app.jammes.plugado.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.plugado.R
import app.jammes.plugado.data.prefs.ThemeChoice
import app.jammes.plugado.ui.common.IconBadge
import app.jammes.plugado.ui.common.InfoNote
import app.jammes.plugado.ui.common.PlugadoSheet
import app.jammes.plugado.ui.common.PlugadoSwitch
import app.jammes.plugado.ui.common.PrimaryButton
import app.jammes.plugado.ui.common.QuietButton
import app.jammes.plugado.ui.common.RadioRow
import app.jammes.plugado.ui.common.ScreenTitle
import app.jammes.plugado.ui.common.SectionLabel
import app.jammes.plugado.ui.common.batteryUnrestricted
import app.jammes.plugado.ui.common.bottomHairline
import app.jammes.plugado.ui.common.openAppSettings
import app.jammes.plugado.ui.common.rememberOnResume
import app.jammes.plugado.ui.common.rememberTurnOnMonitoring
import app.jammes.plugado.ui.theme.PlugadoIcons
import app.jammes.plugado.ui.theme.PlugadoTheme
import org.koin.compose.viewmodel.koinViewModel

private enum class Sheet { THEME, LANGUAGE, DELETE }

@Composable
fun SettingsRoute(contentPadding: PaddingValues, onOpenPrivacy: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val monitoring by viewModel.monitoring.collectAsStateWithLifecycle()
    val turnOn = rememberTurnOnMonitoring { viewModel.setMonitoring(true) }
    val snackbar = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.delete_done)
    LaunchedEffect(viewModel) { viewModel.deleted.collect { snackbar.showSnackbar(deletedMessage) } }

    Box(Modifier.fillMaxSize()) {
        SettingsScreen(
            theme = theme,
            monitoring = monitoring,
            contentPadding = contentPadding,
            onTheme = viewModel::setTheme,
            onMonitoring = { on -> if (on) turnOn() else viewModel.setMonitoring(false) },
            onOpenPrivacy = onOpenPrivacy,
            onDeleteAll = viewModel::deleteAll,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp)) { data ->
            DoneSnackbar(data.visuals.message)
        }
    }
}

@Composable
fun SettingsScreen(
    theme: ThemeChoice,
    monitoring: Boolean,
    contentPadding: PaddingValues,
    onTheme: (ThemeChoice) -> Unit,
    onMonitoring: (Boolean) -> Unit,
    onOpenPrivacy: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    val colors = PlugadoTheme.colors
    val context = LocalContext.current
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    val language = remember { AppLanguage.current() }
    val batteryOk = rememberOnResume { context.batteryUnrestricted() }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
    ) {
        ScreenTitle(stringResource(R.string.settings_title))

        Group(stringResource(R.string.settings_preferences))
        ValueRow(PlugadoIcons.Contrast, stringResource(R.string.settings_theme), stringResource(theme.label)) { sheet = Sheet.THEME }
        ValueRow(PlugadoIcons.Globe, stringResource(R.string.settings_language), stringResource(language.shortLabel)) { sheet = Sheet.LANGUAGE }

        Group(stringResource(R.string.settings_monitoring))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .bottomHairline(colors.line)
                .toggleable(value = monitoring, role = Role.Switch, onValueChange = onMonitoring),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(PlugadoIcons.Headphones)
            TwoLines(
                stringResource(R.string.settings_monitor_row),
                stringResource(if (monitoring) R.string.settings_monitor_on else R.string.settings_monitor_off),
                Modifier.weight(1f),
            )
            PlugadoSwitch(checked = monitoring)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .bottomHairline(colors.line)
                .clickable { context.openAppSettings() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(PlugadoIcons.Battery)
            TwoLines(
                stringResource(R.string.settings_battery),
                stringResource(if (batteryOk) R.string.settings_battery_allowed else R.string.settings_battery_not_allowed),
                Modifier.weight(1f),
            )
            Icon(PlugadoIcons.External, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.subtle)
        }

        Group(stringResource(R.string.settings_privacy_data))
        ValueRow(PlugadoIcons.Shield, stringResource(R.string.settings_privacy_policy), value = null, onClick = onOpenPrivacy)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { sheet = Sheet.DELETE },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(PlugadoIcons.Trash, tint = colors.danger)
            Text(stringResource(R.string.settings_delete_all), style = MaterialTheme.typography.titleMedium, color = colors.danger)
        }

        Spacer(Modifier.height(24.dp))
        InfoNote(stringResource(R.string.settings_footer), icon = PlugadoIcons.Lock)
        Text(
            stringResource(R.string.settings_version, version),
            modifier = Modifier.padding(start = 24.dp, top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = colors.subtle,
        )
    }

    when (sheet) {
        Sheet.THEME -> PlugadoSheet(onDismiss = { sheet = null }) {
            SheetTitle(stringResource(R.string.settings_theme))
            Column(Modifier.selectableGroup()) {
                ThemeChoice.entries.forEach { choice ->
                    RadioRow(
                        label = stringResource(choice.label),
                        selected = choice == theme,
                        hint = if (choice == ThemeChoice.SYSTEM) stringResource(R.string.theme_system_hint) else null,
                        onClick = {
                            onTheme(choice)
                            sheet = null
                        },
                    )
                }
            }
        }
        Sheet.LANGUAGE -> PlugadoSheet(onDismiss = { sheet = null }) {
            SheetTitle(stringResource(R.string.settings_language))
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { option ->
                    RadioRow(
                        label = stringResource(option.label),
                        selected = option == language,
                        hint = if (option == AppLanguage.SYSTEM) AppLanguage.systemLanguageName(context) else null,
                        onClick = {
                            sheet = null
                            if (option != language) AppLanguage.apply(option)
                        },
                    )
                }
            }
        }
        Sheet.DELETE -> PlugadoSheet(onDismiss = { sheet = null }) {
            IconBadge(PlugadoIcons.Trash, size = 48.dp, iconSize = 24.dp, tint = colors.danger)
            Text(
                stringResource(R.string.delete_title),
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 28.sp),
                color = colors.text,
            )
            Text(
                stringResource(R.string.delete_body),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted,
            )
            PrimaryButton(stringResource(R.string.delete_confirm), danger = true, onClick = {
                onDeleteAll()
                sheet = null
            })
            QuietButton(stringResource(R.string.action_cancel), onClick = { sheet = null }, Modifier.padding(top = 8.dp), color = colors.text)
        }
        null -> Unit
    }
}

private val ThemeChoice.label: Int
    get() = when (this) {
        ThemeChoice.SYSTEM -> R.string.theme_system
        ThemeChoice.LIGHT -> R.string.theme_light
        ThemeChoice.DARK -> R.string.theme_dark
    }

/** Na linha, o "Padrão do sistema" pode ser mais curto (em espanhol, "Del sistema"). */
private val AppLanguage.shortLabel: Int
    get() = if (this == AppLanguage.SYSTEM) R.string.language_system_short else label

@Composable
private fun Group(title: String) {
    SectionLabel(title, Modifier.padding(top = 28.dp, bottom = 4.dp))
}

@Composable
private fun TwoLines(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = PlugadoTheme.colors.text)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = PlugadoTheme.colors.muted)
    }
}

/** Linha com ícone, título, o valor atual (opcional) e a seta. */
@Composable
private fun ValueRow(icon: ImageVector, title: String, value: String?, onClick: () -> Unit) {
    val colors = PlugadoTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).bottomHairline(colors.line).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBadge(icon)
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.text)
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = colors.muted, maxLines = 1)
        }
        Icon(PlugadoIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.subtle)
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(bottom = 4.dp),
        style = MaterialTheme.typography.titleLarge,
        color = PlugadoTheme.colors.text,
    )
}

@Composable
private fun DoneSnackbar(message: String) {
    val colors = PlugadoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.inverse)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(PlugadoIcons.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.onInverse)
        Text(message, style = MaterialTheme.typography.titleSmall, color = colors.onInverse)
    }
}

