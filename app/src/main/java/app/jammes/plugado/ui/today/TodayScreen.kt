package app.jammes.plugado.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.domain.model.Output
import app.jammes.domain.model.OutputType
import app.jammes.domain.model.Volume
import app.jammes.domain.stats.SessionSummary
import app.jammes.domain.stats.Span
import app.jammes.plugado.R
import app.jammes.plugado.ui.common.IconBadge
import app.jammes.plugado.ui.common.InfoNote
import app.jammes.plugado.ui.common.CompactButton
import app.jammes.plugado.ui.common.ScreenTitle
import app.jammes.plugado.ui.common.SectionTitle
import app.jammes.plugado.ui.common.StatTile
import app.jammes.plugado.ui.common.bottomHairline
import app.jammes.plugado.ui.common.compactDuration
import app.jammes.plugado.ui.common.currentLocale
import app.jammes.plugado.ui.common.durationWithUnits
import app.jammes.plugado.ui.common.formatDuration
import app.jammes.plugado.ui.common.formatLongDate
import app.jammes.plugado.ui.common.formatPercent
import app.jammes.plugado.ui.common.rememberTimeFormatter
import app.jammes.plugado.ui.common.rememberTurnOnMonitoring
import app.jammes.plugado.ui.theme.PlugadoIcons
import app.jammes.plugado.ui.theme.PlugadoTheme
import app.jammes.plugado.ui.theme.TabularNumbers
import org.koin.compose.viewmodel.koinViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun TodayRoute(contentPadding: PaddingValues, viewModel: TodayViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val turnOn = rememberTurnOnMonitoring { viewModel.setMonitoring(true) }
    state?.let { TodayScreen(it, contentPadding, onTurnOn = turnOn, onTurnOff = { viewModel.setMonitoring(false) }) }
}

@Composable
fun TodayScreen(state: TodayState, contentPadding: PaddingValues, onTurnOn: () -> Unit, onTurnOff: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Header(state, onTurnOn, onTurnOff)
        if (!state.enabled) PausedBanner(onTurnOn)
        Hero(state)
        Timeline(state)
        if (state.hasData) {
            Stats(state)
            Sessions(state)
        } else {
            EmptyCard()
        }
    }
}

@Composable
private fun Header(state: TodayState, onTurnOn: () -> Unit, onTurnOff: () -> Unit) {
    val colors = PlugadoTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ScreenTitle(stringResource(R.string.today_title))
            Text(formatLongDate(state.day, currentLocale()), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
        StatusPill(state, onTurnOn, onTurnOff)
    }
}

/** Mostra o estado do monitoramento e liga/desliga com um toque. */
@Composable
private fun StatusPill(state: TodayState, onTurnOn: () -> Unit, onTurnOff: () -> Unit) {
    val colors = PlugadoTheme.colors
    val label = stringResource(
        when {
            !state.enabled -> R.string.today_status_paused
            state.listening -> R.string.today_status_listening
            else -> R.string.today_status_monitoring
        },
    )
    val action = stringResource(if (state.enabled) R.string.today_status_a11y_on else R.string.today_status_a11y_paused)
    Surface(
        onClick = { if (state.enabled) onTurnOff() else onTurnOn() },
        modifier = Modifier.semantics { stateDescription = action },
        shape = CircleShape,
        color = colors.chip,
        contentColor = colors.text,
    ) {
        Row(
            Modifier.height(44.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val dot = Modifier.size(8.dp).clip(CircleShape)
            Box(if (state.enabled) dot.background(colors.text) else dot.border(1.5.dp, colors.text, CircleShape))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun PausedBanner(onTurnOn: () -> Unit) {
    val colors = PlugadoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.today_paused_title), style = MaterialTheme.typography.titleMedium, color = colors.text)
            Text(stringResource(R.string.today_paused_body), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
        CompactButton(stringResource(R.string.today_resume), onClick = onTurnOn)
    }
}

@Composable
private fun Hero(state: TodayState) {
    val colors = PlugadoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val spoken = formatDuration(state.total)
        Text(
            durationWithUnits(state.total, unitSize = 28.sp),
            modifier = Modifier.semantics { contentDescription = spoken },
            style = MaterialTheme.typography.displayLarge,
            color = colors.text,
        )
        Text(stringResource(R.string.today_hero_caption), style = MaterialTheme.typography.bodyLarge, color = colors.muted)
        state.versusAverage?.let { Comparison(it) }
    }
}

@Composable
private fun Comparison(versusAverage: Duration) {
    val colors = PlugadoTheme.colors
    val minutes = versusAverage.toMinutes()
    val difference = formatDuration(Duration.ofMinutes(abs(minutes)))
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            minutes > 0 -> Icon(PlugadoIcons.ArrowUp, null, Modifier.size(16.dp), tint = colors.muted)
            minutes < 0 -> Icon(PlugadoIcons.ArrowDown, null, Modifier.size(16.dp), tint = colors.muted)
        }
        Text(
            when {
                minutes > 0 -> stringResource(R.string.today_above_average, difference)
                minutes < 0 -> stringResource(R.string.today_below_average, difference)
                else -> stringResource(R.string.today_at_average)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
    }
}

/** As 24 horas do dia, com cada trecho ouvido marcado e uma linha em "agora". */
@Composable
private fun Timeline(state: TodayState) {
    val colors = PlugadoTheme.colors
    val time = rememberTimeFormatter()
    val description = stringResource(R.string.today_timeline_a11y)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.today_timeline_title), style = MaterialTheme.typography.titleMedium, color = colors.text)
            Text(
                stringResource(R.string.today_timeline_now, time.format(state.now.atZone(state.zone))),
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TabularNumbers),
                color = colors.muted,
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.track)
                .semantics { contentDescription = description },
        ) {
            val inset = 7.dp.toPx()
            val minWidth = 2.dp.toPx()
            state.spans.forEach { span ->
                val start = state.fractionOfDay(span.start) * size.width
                val end = state.fractionOfDay(span.end) * size.width
                drawRoundRect(
                    color = colors.text,
                    topLeft = Offset(start, inset),
                    size = Size(maxOf(end - start, minWidth), size.height - 2 * inset),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
            val now = state.fractionOfDay(state.now) * size.width
            drawRect(colors.subtle, topLeft = Offset(now - 1.dp.toPx(), 0f), size = Size(2.dp.toPx(), size.height))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0h", "6h", "12h", "18h", "24h").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TabularNumbers), color = colors.subtle)
            }
        }
    }
}

@Composable
private fun Stats(state: TodayState) {
    val locale = currentLocale()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val tile = Modifier.weight(1f).fillMaxHeight()
            StatTile(
                AnnotatedString(state.averageVolumePercent?.let { formatPercent(it, locale) } ?: "–"),
                stringResource(R.string.stat_average_volume),
                tile,
            )
            StatTile(AnnotatedString(state.sessions.size.toString()), stringResource(R.string.stat_sessions), tile)
            StatTile(compactDuration(state.longest ?: Duration.ZERO), stringResource(R.string.stat_longest), tile)
        }
        InfoNote(stringResource(R.string.today_volume_note))
    }
}

@Composable
private fun Sessions(state: TodayState) {
    val time = rememberTimeFormatter()
    Column {
        SectionTitle(stringResource(R.string.stat_sessions), Modifier.padding(bottom = 4.dp))
        state.sessions.forEach { SessionRow(it, live = it.open && state.listening, zone = state.zone, time = time) }
    }
}

@Composable
private fun SessionRow(session: SessionSummary, live: Boolean, zone: ZoneId, time: DateTimeFormatter) {
    val colors = PlugadoTheme.colors
    val end = if (live) stringResource(R.string.session_now) else time.format(session.end.atZone(zone))
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).bottomHairline(colors.line),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBadge(PlugadoIcons.Headphones, size = 44.dp, iconSize = 22.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.time_range, time.format(session.start.atZone(zone)), end),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularNumbers),
                color = colors.text,
            )
            Text(
                stringResource(R.string.session_detail, session.output.name, formatPercent(session.volumePercent, currentLocale())),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            formatDuration(session.listened),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularNumbers),
            color = colors.text,
        )
    }
}

@Composable
private fun EmptyCard() {
    val colors = PlugadoTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(PlugadoIcons.Headphones, size = 48.dp, iconSize = 24.dp)
        SectionTitle(stringResource(R.string.today_empty_title))
        Text(
            stringResource(R.string.today_empty_body),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = colors.muted,
        )
    }
}

// Prévias com os dados do protótipo: 2 h 14 min em 5 sessões.
private fun previewState(enabled: Boolean = true, empty: Boolean = false): TodayState {
    val zone = ZoneId.of("America/Sao_Paulo")
    val day = LocalDate.of(2026, 10, 21)
    val fone = Output(OutputType.BLUETOOTH, "ZMI PurPods Pro")
    fun at(h: Int, m: Int): Instant = day.atTime(h, m).atZone(zone).toInstant()
    val spans = if (empty) emptyList() else listOf(
        Span("1", at(7, 42), at(8, 16), fone, Volume(15, 30, -30f)),
        Span("2", at(12, 5), at(12, 23), fone, Volume(16, 30, -28f)),
        Span("3", at(15, 10), at(15, 27), Output(OutputType.WIRED, "Fone com fio"), Volume(18, 30, -26f)),
        Span("4", at(18, 40), at(19, 28), fone, Volume(19, 30, -25f)),
        Span("5", at(21, 23), at(21, 40), fone, Volume(18, 30, -26f), open = true),
    )
    return buildTodayState(day, zone, at(21, 40), spans, emptyList(), null, enabled, listening = enabled && !empty)
}

@Preview(name = "Hoje · escuro", widthDp = 412, heightDp = 915, backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun TodayDarkPreview() {
    PlugadoTheme(darkTheme = true) { TodayScreen(previewState(), PaddingValues(top = 32.dp), {}, {}) }
}

@Preview(name = "Hoje · claro, pausado", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun TodayLightPausedPreview() {
    PlugadoTheme(darkTheme = false) { TodayScreen(previewState(enabled = false), PaddingValues(top = 32.dp), {}, {}) }
}

@Preview(name = "Hoje · nada ouvido", widthDp = 412, heightDp = 915, backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun TodayEmptyPreview() {
    PlugadoTheme(darkTheme = true) { TodayScreen(previewState(empty = true), PaddingValues(top = 32.dp), {}, {}) }
}
