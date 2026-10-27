package app.jammes.plugado.ui.privacy

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import app.jammes.plugado.R
import app.jammes.plugado.ui.common.CompactButton
import app.jammes.plugado.ui.common.IconBadge
import app.jammes.plugado.ui.common.ScreenTitle
import app.jammes.plugado.ui.theme.PlugadoIcons
import app.jammes.plugado.ui.theme.PlugadoTheme

/**
 * Endereço da política completa, que a Play Store exige. Ainda não existe: enquanto for null,
 * o botão "Ler a política completa" não aparece.
 */
val PRIVACY_POLICY_URL: String? = null

/** O resumo da privacidade, aberto pelos Ajustes. Esconde a barra de abas. */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val colors = PlugadoTheme.colors
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(PlugadoIcons.ChevronLeft, contentDescription = stringResource(R.string.action_back), tint = colors.text)
        }
        ScreenTitle(stringResource(R.string.privacy_title), Modifier.padding(top = 16.dp))
        Text(
            stringResource(R.string.privacy_intro),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.muted,
        )
        Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Point(PlugadoIcons.Headphones, R.string.privacy_records_title, R.string.privacy_records_body)
            Point(PlugadoIcons.MicOff, R.string.privacy_not_title, R.string.privacy_not_body)
            Point(PlugadoIcons.Phone, R.string.privacy_where_title, R.string.privacy_where_body)
            Point(PlugadoIcons.Settings, R.string.privacy_control_title, R.string.privacy_control_body)
        }
        PRIVACY_POLICY_URL?.let { url ->
            CompactButton(
                stringResource(R.string.privacy_full_policy),
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
                modifier = Modifier.padding(top = 32.dp),
                emphasized = false,
                trailingIcon = PlugadoIcons.External,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.privacy_updated), style = MaterialTheme.typography.labelMedium, color = colors.subtle)
    }
}

@Composable
private fun Point(icon: ImageVector, title: Int, body: Int) {
    val colors = PlugadoTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBadge(icon)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
            )
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = colors.muted,
            )
        }
    }
}
