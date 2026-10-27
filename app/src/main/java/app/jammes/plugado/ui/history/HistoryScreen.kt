package app.jammes.plugado.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.domain.stats.DayTotal
import app.jammes.plugado.R
import app.jammes.plugado.ui.common.IconBadge
import app.jammes.plugado.ui.common.ScreenTitle
import app.jammes.plugado.ui.common.SectionTitle
import app.jammes.plugado.ui.common.StatTile
import app.jammes.plugado.ui.common.bottomHairline
import app.jammes.plugado.ui.common.capitalized
import app.jammes.plugado.ui.common.compactDuration
import app.jammes.plugado.ui.common.currentLocale
import app.jammes.plugado.ui.common.durationWithUnits
import app.jammes.plugado.ui.common.formatDayMonth
import app.jammes.plugado.ui.common.formatDayRange
import app.jammes.plugado.ui.common.formatDuration
import app.jammes.plugado.ui.common.formatDurationShort
import app.jammes.plugado.ui.common.formatPercent
import app.jammes.plugado.ui.common.formatWeekday
import app.jammes.plugado.ui.common.formatWeekdayShort
import app.jammes.plugado.ui.theme.PlugadoIcons
import app.jammes.plugado.ui.theme.PlugadoTheme
import app.jammes.plugado.ui.theme.TabularNumbers
import org.koin.compose.viewmodel.koinViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun HistoryRoute(contentPadding: PaddingValues, viewModel: HistoryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let { HistoryScreen(it, contentPadding, onPeriod = viewModel::setPeriod, onSelect = viewModel::select) }
}

@Composable
fun HistoryScreen(
    state: HistoryState,
    contentPadding: PaddingValues,
    onPeriod: (Int) -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = PlugadoTheme.colors
    val locale = currentLocale()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenTitle(stringResource(R.string.history_title))
        PeriodSelector(state.periodDays, onPeriod)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val spoken = formatDuration(state.total)
            Text(
                durationWithUnits(state.total, unitSize = 22.sp),
                modifier = Modifier.semantics { contentDescription = spoken },
                style = MaterialTheme.typography.displayMedium,
                color = colors.text,
            )
            val range = formatDayRange(state.from, state.today, locale)
            Text(
                state.averagePerDay?.let { stringResource(R.string.history_subtitle, range, formatDuration(it)) } ?: range,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
        DailyBars(state, onSelect)
        if (state.hasData) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val tile = Modifier.weight(1f).fillMaxHeight()
                StatTile(
                    AnnotatedString(state.averageVolumePercent?.let { formatPercent(it, locale) } ?: "–"),
                    stringResource(R.string.stat_average_volume),
                    tile,
                )
                state.longestDay?.let { longest ->
                    StatTile(
                        compactDuration(longest.listened),
                        stringResource(R.string.stat_longest_day, formatWeekday(longest.day, locale)),
                        tile,
                    )
                }
            }
            ByDay(state)
        } else {
            EmptyCard()
        }
    }
}

/** "7 dias | 30 dias", em pílula. */
@Composable
private fun PeriodSelector(periodDays: Int, onPeriod: (Int) -> Unit) {
    val colors = PlugadoTheme.colors
    val label = stringResource(R.string.history_period)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.chip)
            .padding(4.dp)
            .selectableGroup()
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(7 to R.string.history_7_days, 30 to R.string.history_30_days).forEach { (days, text) ->
            val selected = periodDays == days
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.inverse else Color.Transparent)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onPeriod(days) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(text),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (selected) colors.onInverse else colors.text,
                )
            }
        }
    }
}

/**
 * Uma barra por dia. O dia selecionado fica branco e mostra o valor; os outros, cinza.
 * Tocar numa barra seleciona o dia. A lista "Por dia" mostra os mesmos números em texto.
 */
@Composable
private fun DailyBars(state: HistoryState, onSelect: (LocalDate) -> Unit) {
    val colors = PlugadoTheme.colors
    val locale = currentLocale()
    val maxMinutes = state.days.maxOf { it.listened.toMinutes() }
    val topHours = maxOf(1L, ceil(maxMinutes / 60.0).toLong())
    val step = if (topHours <= 4) 1L else ceil(topHours / 3.0).toLong()
    val top = (topHours + step - 1) / step * step // o topo do eixo, múltiplo do passo
    val plot = 180.dp
    val gutter = 36.dp
    val barWidth = if (state.days.size <= 7) 24.dp else 6.dp
    val todayLabel = stringResource(R.string.day_today)
    val description = stringResource(R.string.history_chart_a11y)

    Column(Modifier.semantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(plot + 20.dp)) {
            Canvas(Modifier.fillMaxSize().padding(end = gutter)) {
                val perHour = plot.toPx() / top
                for (hour in 0..top step step) {
                    val y = size.height - hour * perHour
                    drawLine(colors.line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
            }
            for (hour in step..top step step) {
                Text(
                    "$hour h",
                    modifier = Modifier.align(Alignment.BottomEnd).offset(y = -(plot * (hour.toFloat() / top)) + 8.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TabularNumbers),
                    color = colors.subtle,
                )
            }
            Row(Modifier.fillMaxSize().padding(end = gutter).selectableGroup()) {
                state.days.forEach { day ->
                    val selected = day.day == state.selected
                    val name = if (day.day == state.today) todayLabel else formatDayMonth(day.day, locale)
                    val spoken = stringResource(R.string.history_day_a11y, name, formatDuration(day.listened))
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(day.day) })
                            .semantics { contentDescription = spoken },
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (selected) {
                            Text(
                                formatDurationShort(day.listened),
                                modifier = Modifier.wrapContentWidth(unbounded = true),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = TabularNumbers,
                                ),
                                color = colors.text,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        Box(
                            Modifier
                                .width(barWidth)
                                .height(plot * (day.listened.toMinutes().toFloat() / (top * 60)))
                                .background(
                                    if (selected) colors.text else colors.bar,
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                ),
                        )
                    }
                }
            }
        }
        // Nomes dos dias. Com 30 barras, só um a cada 7 (contando de hoje pra trás).
        Row(Modifier.fillMaxWidth().padding(end = gutter)) {
            state.days.forEach { day ->
                val showLabel = state.days.size <= 7 || ChronoUnit.DAYS.between(day.day, state.today) % 7 == 0L
                val selected = day.day == state.selected
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (showLabel) {
                        Text(
                            when {
                                day.day == state.today -> todayLabel
                                state.days.size <= 7 -> formatWeekdayShort(day.day, locale)
                                else -> day.day.dayOfMonth.toString()
                            },
                            modifier = Modifier.wrapContentWidth(unbounded = true),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = if (selected) colors.text else colors.subtle,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

/** Os mesmos números do gráfico em lista, do dia mais recente pro mais antigo. */
@Composable
private fun ByDay(state: HistoryState) {
    val colors = PlugadoTheme.colors
    val locale = currentLocale()
    val today = stringResource(R.string.day_today)
    val yesterday = stringResource(R.string.day_yesterday)
    Column {
        SectionTitle(stringResource(R.string.history_by_day), Modifier.padding(bottom = 4.dp))
        state.days.filter { !it.listened.isZero }.asReversed().forEach { day ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp).bottomHairline(colors.line),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        when (day.day) {
                            state.today -> today
                            state.today.minusDays(1) -> yesterday
                            else -> formatWeekday(day.day, locale).capitalized(locale)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.text,
                    )
                    Text(
                        stringResource(
                            R.string.history_row_detail,
                            formatDayMonth(day.day, locale),
                            formatPercent(day.volumePercent?.roundToInt() ?: 0, locale),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                Text(
                    formatDuration(day.listened),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularNumbers),
                    color = colors.text,
                )
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    val colors = PlugadoTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(PlugadoIcons.History, size = 48.dp, iconSize = 24.dp)
        SectionTitle(stringResource(R.string.history_empty_title))
        Text(
            stringResource(R.string.history_empty_body),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = colors.muted,
        )
    }
}

// Prévia com a semana do protótipo: 12 h 26 min, média de 1 h 47.
private fun previewState(): HistoryState {
    val today = LocalDate.of(2026, 10, 21)
    val minutes = listOf(75, 110, 150, 130, 85, 62, 134)
    val volumes = listOf(57.0, 62.0, 66.0, 60.0, 59.0, 63.0, 58.0)
    val totals = minutes.indices.map { i ->
        DayTotal(today.minusDays(6L - i), Duration.ofMinutes(minutes[i].toLong()), volumes[i])
    }
    return buildHistoryState(today, 7, totals, firstDay = LocalDate.of(2026, 9, 1), selected = null)
}

@Preview(name = "Histórico · escuro", widthDp = 412, heightDp = 915, backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun HistoryDarkPreview() {
    PlugadoTheme(darkTheme = true) { HistoryScreen(previewState(), PaddingValues(top = 32.dp), {}, {}) }
}

@Preview(name = "Histórico · claro", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun HistoryLightPreview() {
    PlugadoTheme(darkTheme = false) { HistoryScreen(previewState(), PaddingValues(top = 32.dp), {}, {}) }
}
