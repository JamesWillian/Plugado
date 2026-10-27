package app.jammes.plugado.ui

import android.graphics.Color.TRANSPARENT
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.plugado.R
import app.jammes.plugado.data.prefs.ThemeChoice
import app.jammes.plugado.ui.devices.DevicesRoute
import app.jammes.plugado.ui.history.HistoryRoute
import app.jammes.plugado.ui.onboarding.OnboardingFlow
import app.jammes.plugado.ui.privacy.PrivacyScreen
import app.jammes.plugado.ui.settings.SettingsRoute
import app.jammes.plugado.ui.theme.PlugadoIcons
import app.jammes.plugado.ui.theme.PlugadoTheme
import app.jammes.plugado.ui.today.TodayRoute
import org.koin.compose.viewmodel.koinViewModel

enum class AppDestinations(@get:StringRes val label: Int, val icon: ImageVector) {
    TODAY(R.string.nav_today, PlugadoIcons.Today),
    HISTORY(R.string.nav_history, PlugadoIcons.History),
    DEVICES(R.string.nav_devices, PlugadoIcons.Headphones),
    SETTINGS(R.string.nav_settings, PlugadoIcons.Settings),
}

/** A raiz: aplica o tema escolhido e decide entre o primeiro uso e as abas. */
@Composable
fun PlugadoRoot(viewModel: AppViewModel = koinViewModel()) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    val dark = when (theme) {
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
        else -> isSystemInDarkTheme()
    }
    SystemBars(dark)
    PlugadoTheme(darkTheme = dark) {
        Box(Modifier.fillMaxSize().background(PlugadoTheme.colors.background)) {
            when (onboardingDone) {
                true -> PlugadoApp()
                false -> OnboardingFlow()
                null -> Unit // ainda lendo as preferências (questão de milissegundos)
            }
        }
    }
}

/** Os ícones das barras do sistema na cor certa pro tema do app, que pode ser diferente do celular. */
@Composable
private fun SystemBars(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { dark },
        )
        onDispose { }
    }
}

/** As 4 abas. A Privacidade abre por cima delas, a partir dos Ajustes. */
@Composable
fun PlugadoApp() {
    var current by rememberSaveable { mutableStateOf(AppDestinations.TODAY) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }

    if (showPrivacy) {
        PrivacyScreen(onBack = { showPrivacy = false })
        return
    }
    // O "voltar" numa aba que não é a Hoje leva pra Hoje antes de sair do app.
    BackHandler(enabled = current != AppDestinations.TODAY) { current = AppDestinations.TODAY }

    val colors = PlugadoTheme.colors
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.text,
            selectedTextColor = colors.text,
            indicatorColor = Color.Transparent,
            unselectedIconColor = colors.subtle,
            unselectedTextColor = colors.subtle,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = colors.text,
            selectedTextColor = colors.text,
            indicatorColor = Color.Transparent,
            unselectedIconColor = colors.subtle,
            unselectedTextColor = colors.subtle,
        ),
    )
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                val selected = destination == current
                item(
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = {
                        Text(
                            stringResource(destination.label),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                        )
                    },
                    selected = selected,
                    onClick = { current = destination },
                    colors = itemColors,
                )
            }
        },
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = colors.background,
            navigationRailContainerColor = colors.background,
        ),
        containerColor = colors.background,
        contentColor = colors.text,
    ) {
        Scaffold(containerColor = colors.background, contentColor = colors.text) { innerPadding ->
            Box(Modifier.fillMaxSize()) {
                when (current) {
                    AppDestinations.TODAY -> TodayRoute(innerPadding)
                    AppDestinations.HISTORY -> HistoryRoute(innerPadding)
                    AppDestinations.DEVICES -> DevicesRoute(innerPadding)
                    AppDestinations.SETTINGS -> SettingsRoute(innerPadding, onOpenPrivacy = { showPrivacy = true })
                }
                // A linha fina em cima da barra de abas, como no protótipo.
                HorizontalDivider(Modifier.align(Alignment.BottomCenter), thickness = 1.dp, color = colors.line)
            }
        }
    }
}
