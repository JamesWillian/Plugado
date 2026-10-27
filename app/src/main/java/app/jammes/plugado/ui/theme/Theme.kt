package app.jammes.plugado.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalPlugadoColors = staticCompositionLocalOf { DarkColors }

/** Sem cor dinâmica: o app é preto e branco de propósito. */
@Composable
fun PlugadoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    // O esquema do Material é só pros componentes prontos (Switch, painéis, Snackbar).
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.text, onPrimary = colors.background,
            background = colors.background, onBackground = colors.text,
            surface = colors.background, onSurface = colors.text,
            surfaceVariant = colors.surface, onSurfaceVariant = colors.muted,
            surfaceContainer = colors.background, surfaceContainerLow = colors.sheet,
            outline = colors.subtle, outlineVariant = colors.line,
            error = colors.danger, onError = colors.onDanger,
        )
    } else {
        lightColorScheme(
            primary = colors.text, onPrimary = colors.background,
            background = colors.background, onBackground = colors.text,
            surface = colors.background, onSurface = colors.text,
            surfaceVariant = colors.surface, onSurfaceVariant = colors.muted,
            surfaceContainer = colors.background, surfaceContainerLow = colors.sheet,
            outline = colors.subtle, outlineVariant = colors.line,
            error = colors.danger, onError = colors.onDanger,
        )
    }
    CompositionLocalProvider(LocalPlugadoColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}

/** Atalho pras cores do app: `PlugadoTheme.colors.muted`. */
object PlugadoTheme {
    val colors: PlugadoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPlugadoColors.current
}
