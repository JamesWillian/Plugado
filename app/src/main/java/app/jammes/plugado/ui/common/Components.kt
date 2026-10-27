package app.jammes.plugado.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jammes.plugado.R
import app.jammes.plugado.ui.theme.PlugadoIcons
import app.jammes.plugado.ui.theme.PlugadoTheme
import java.time.Duration

/** O título grande de cada tela ("Hoje", "Ajustes"). */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineLarge,
        color = PlugadoTheme.colors.text,
    )
}

/** O título de uma parte da tela ("Sessões", "Por dia"). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, color = PlugadoTheme.colors.text)
}

/** O rótulo cinza de um grupo de ajustes ("Preferências"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall, color = PlugadoTheme.colors.muted)
}

/** Ícone dentro de um círculo cinza, no começo das linhas e dos cartões. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    tint: Color = PlugadoTheme.colors.text,
    background: Color = PlugadoTheme.colors.chip,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = tint)
    }
}

/**
 * O logo: o ícone do app em miniatura. O `ic_plugado` já traz o respiro do ícone em volta do fone,
 * então ocupa o círculo inteiro e mantém a proporção do ícone em qualquer tamanho.
 * Invertido, como o botão principal: preto no tema claro, branco no escuro.
 */
@Composable
fun PlugadoLogo(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val colors = PlugadoTheme.colors
    Icon(
        painterResource(R.drawable.ic_plugado),
        contentDescription = null,
        modifier = modifier.size(size).clip(CircleShape).background(colors.inverse),
        tint = colors.onInverse,
    )
}

/** Um número com a legenda embaixo, num quadro cinza. */
@Composable
fun StatTile(value: AnnotatedString, label: String, modifier: Modifier = Modifier) {
    val colors = PlugadoTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = colors.text, maxLines = 1)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = colors.muted,
        )
    }
}

/** Nota pequena com ícone, pra avisos honestos sobre limitações. */
@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier, icon: ImageVector = PlugadoIcons.Info) {
    val colors = PlugadoTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(top = 1.dp).size(16.dp), tint = colors.subtle)
        Text(text, style = MaterialTheme.typography.bodySmall, color = colors.subtle)
    }
}

/** O botão principal da tela: largo, invertido (branco no tema escuro). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val colors = PlugadoTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (danger) colors.danger else colors.inverse,
            contentColor = if (danger) colors.onDanger else colors.onInverse,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp, lineHeight = 22.sp))
    }
}

/** Botão menor, dentro de cartões. `emphasized` = invertido; senão, cinza. */
@Composable
fun CompactButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    val colors = PlugadoTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.height(if (trailingIcon != null) 48.dp else 44.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) colors.inverse else colors.chip,
            contentColor = if (emphasized) colors.onInverse else colors.text,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.padding(start = 8.dp).size(18.dp))
        }
    }
}

/** Botão discreto, sem fundo ("Agora não", "Cancelar"). */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = PlugadoTheme.colors.muted) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** Interruptor nas cores do app. Quem reage ao toque é a linha inteira (alvo maior). */
@Composable
fun PlugadoSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = PlugadoTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = null,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onInverse,
            checkedTrackColor = colors.inverse,
            checkedBorderColor = colors.inverse,
            uncheckedThumbColor = colors.subtle,
            uncheckedTrackColor = Color.Transparent,
            uncheckedBorderColor = colors.subtle,
        ),
    )
}

/** Painel que sobe de baixo, com a alça e as cores do app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlugadoSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = PlugadoTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = colors.sheet,
        contentColor = colors.text,
        scrimColor = colors.scrim,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp).size(width = 36.dp, height = 4.dp).clip(CircleShape).background(colors.handle))
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 32.dp), content = content)
    }
}

/** Uma opção de escolha única (tema, idioma). A linha inteira é o alvo do toque. */
@Composable
fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit, hint: String? = null) {
    val colors = PlugadoTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = colors.text)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.text, unselectedColor = colors.subtle),
        )
    }
}

/** "2 h 14 min" com as unidades menores que os números, como no número de destaque. */
fun durationWithUnits(duration: Duration, unitSize: TextUnit): AnnotatedString {
    val minutes = duration.toMinutes()
    val unit = SpanStyle(fontSize = unitSize, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
    return buildAnnotatedString {
        if (minutes >= 60) {
            append((minutes / 60).toString())
            withStyle(unit) { append(" h  ") }
        }
        append((minutes % 60).toString())
        withStyle(unit) { append(" min") }
    }
}

/** A versão pros quadros de número: "48 min" ou "1 h 05", com a unidade menor. */
fun compactDuration(duration: Duration): AnnotatedString {
    val minutes = duration.toMinutes()
    val unit = SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    return buildAnnotatedString {
        if (minutes >= 60) {
            append((minutes / 60).toString())
            withStyle(unit) { append(" h ") }
            append("%02d".format(minutes % 60))
        } else {
            append(minutes.toString())
            withStyle(unit) { append(" min") }
        }
    }
}

/** Linha divisória fina embaixo de uma linha de lista. */
fun Modifier.bottomHairline(color: Color): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
}
