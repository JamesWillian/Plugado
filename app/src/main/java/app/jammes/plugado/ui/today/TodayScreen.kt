package app.jammes.plugado.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import app.jammes.plugado.ui.common.deviceName
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
import kotlin.math.roundToInt

@Composable
fun TodayRoute(contentPadding: PaddingValues, viewModel: TodayViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val turnOn = rememberTurnOnMonitoring { viewModel.setMonitoring(true) }
    state?.let { TodayScreen(it, contentPadding, onTurnOn = turnOn, onTurnOff = { viewModel.setMonitoring(false) }) }
}

/** [initiallyExpanded] é o id da sessão que já começa aberta (usado nas prévias). */
@Composable
fun TodayScreen(
    state: TodayState,
    contentPadding: PaddingValues,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    initiallyExpanded: String? = null,
) {
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
            Sessions(state, initiallyExpanded)
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
private fun Sessions(state: TodayState, initiallyExpanded: String?) {
    val time = rememberTimeFormatter()
    // Uma sessão aberta por vez. Guarda o id, e não a posição: uma sessão nova entra no topo da lista.
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column {
        SectionTitle(stringResource(R.string.stat_sessions), Modifier.padding(bottom = 4.dp))
        state.sessions.forEach { session ->
            key(session.id) {
                SessionItem(
                    session,
                    expanded = session.id == expanded,
                    onToggle = { expanded = if (session.id == expanded) null else session.id },
                    live = session.open && state.listening,
                    zone = state.zone,
                    time = time,
                )
            }
        }
    }
}

/** Uma sessão da lista. Com um toque, vira um cartão com o gráfico do volume e os detalhes. */
@Composable
private fun SessionItem(
    session: SessionSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
    live: Boolean,
    zone: ZoneId,
    time: DateTimeFormatter,
) {
    val colors = PlugadoTheme.colors
    val card by animateColorAsState(if (expanded) colors.surface else colors.surface.copy(alpha = 0f), label = "card")
    val gap by animateDpAsState(if (expanded) 8.dp else 0.dp, label = "gap")
    Column(
        Modifier
            .padding(vertical = gap)
            .bleed(CardInset)
            .clip(RoundedCornerShape(16.dp))
            .background(card),
    ) {
        SessionRow(session, expanded, onToggle, live, zone, time)
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            SessionDetails(session, live, zone, time, Modifier.padding(start = CardInset, end = CardInset, bottom = 16.dp))
        }
    }
}

@Composable
private fun SessionRow(
    session: SessionSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
    live: Boolean,
    zone: ZoneId,
    time: DateTimeFormatter,
) {
    val colors = PlugadoTheme.colors
    val end = if (live) stringResource(R.string.session_now) else time.format(session.end.atZone(zone))
    val hairline by animateColorAsState(if (expanded) colors.line.copy(alpha = 0f) else colors.line, label = "hairline")
    val badge by animateColorAsState(if (expanded) colors.line else colors.chip, label = "badge")
    val turn by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(if (expanded) R.string.session_collapse else R.string.session_expand),
                onClick = onToggle,
            )
            .padding(horizontal = CardInset)
            .heightIn(min = 72.dp)
            .bottomHairline(hairline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBadge(PlugadoIcons.Headphones, size = 44.dp, iconSize = 22.dp, background = badge)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.time_range, time.format(session.start.atZone(zone)), end),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularNumbers),
                color = colors.text,
            )
            Text(
                stringResource(R.string.session_average_volume, formatPercent(session.volumePercent, currentLocale())),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                formatDuration(session.listened),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularNumbers),
                color = colors.text,
            )
            Icon(PlugadoIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(20.dp).rotate(turn), tint = colors.subtle)
        }
    }
}

/** O que aparece com a sessão aberta: o gráfico do volume, o pico e o fone. */
@Composable
private fun SessionDetails(session: SessionSummary, live: Boolean, zone: ZoneId, time: DateTimeFormatter, modifier: Modifier = Modifier) {
    val colors = PlugadoTheme.colors
    val locale = currentLocale()
    val atPeak = if (session.timeAtPeak < Duration.ofMinutes(1)) {
        stringResource(R.string.duration_under_a_minute)
    } else {
        formatDuration(session.timeAtPeak)
    }
    Column(modifier) {
        VolumeChart(session, live, Modifier.padding(top = 4.dp).fillMaxWidth().height(ChartTop + ChartPlot + ChartBottom))
        TimeAxis(session, live, zone, time, Modifier.padding(top = 6.dp, end = ChartAxisWidth).fillMaxWidth())
        Box(Modifier.padding(top = 18.dp).fillMaxWidth().height(1.dp).background(colors.line))
        Row(Modifier.padding(top = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            SessionStat(formatPercent(session.peakPercent, locale), stringResource(R.string.session_peak))
            SessionStat(atPeak, stringResource(R.string.session_time_at_peak))
            // O nome do fone fica com o espaço que sobra, numa linha só: a fonte diminui até caber
            SessionStat(deviceName(session.output), stringResource(R.string.session_device), Modifier.weight(1f), autoSize = DeviceNameSize)
        }
    }
}

/**
 * O volume da sessão em degraus: o volume do celular muda aos pulos (um degrau por aperto), então a
 * linha anda em escada. A escala vai sempre de 0 a 100%, pra mesma altura ser o mesmo volume em
 * qualquer sessão. O ponto marca o pico; na sessão ao vivo, outro ponto marca o agora.
 */
@Composable
private fun VolumeChart(session: SessionSummary, live: Boolean, modifier: Modifier = Modifier) {
    val colors = PlugadoTheme.colors
    val locale = currentLocale()
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = colors.subtle, fontFeatureSettings = TabularNumbers)
    val peakStyle = axisStyle.copy(color = colors.text, fontWeight = FontWeight.SemiBold)
    val gridLabels = remember(locale) { listOf(100, 50, 0).associateWith { formatPercent(it, locale) } }
    val peakLabel = formatPercent(session.peakPercent, locale)
    val lowest = session.steps.minOf { it.percent.roundToInt() }
    val description = if (lowest == session.peakPercent) {
        stringResource(R.string.session_chart_steady_a11y, peakLabel)
    } else {
        stringResource(R.string.session_chart_a11y, formatPercent(lowest, locale), peakLabel)
    }
    Canvas(modifier.semantics { contentDescription = description }) {
        val plotWidth = size.width - ChartAxisWidth.toPx()
        val top = ChartTop.toPx()
        val plotHeight = ChartPlot.toPx()
        val total = Duration.between(session.start, session.end).toMillis().coerceAtLeast(1).toFloat()
        fun x(at: Instant) = Duration.between(session.start, at).toMillis() / total * plotWidth
        fun y(percent: Double) = top + plotHeight * (1f - (percent / 100).toFloat().coerceIn(0f, 1f))

        gridLabels.forEach { (percent, label) ->
            val lineY = y(percent.toDouble())
            drawLine(colors.line, Offset(0f, lineY), Offset(plotWidth, lineY), strokeWidth = 1.dp.toPx())
            val text = measurer.measure(label, axisStyle)
            drawText(text, topLeft = Offset(size.width - text.size.width, lineY - text.size.height / 2f))
        }

        val line = Path()
        var previousY = 0f
        session.steps.forEachIndexed { i, step ->
            val stepY = y(step.percent)
            if (i == 0) {
                line.moveTo(x(step.start), stepY)
            } else {
                line.lineTo(x(step.start), previousY) // uma pausa curta entre dois trechos segue no volume anterior
                line.lineTo(x(step.start), stepY)
            }
            line.lineTo(x(step.end), stepY)
            previousY = stepY
        }
        drawPath(line, colors.text, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // O pico fica no meio do trecho mais longo nesse volume, com o valor em cima
        val peak = session.steps.filter { it.percent.roundToInt() == session.peakPercent }.maxBy { it.duration }
        val peakCenter = Offset((x(peak.start) + x(peak.end)) / 2f, y(peak.percent))
        drawCircle(colors.surface, radius = 6.5.dp.toPx(), center = peakCenter)
        drawCircle(colors.text, radius = 4.5.dp.toPx(), center = peakCenter)
        val value = measurer.measure(peakLabel, peakStyle)
        val above = peakCenter.y - 11.dp.toPx() - value.firstBaseline
        drawText(
            value,
            topLeft = Offset(
                (peakCenter.x - value.size.width / 2f).coerceIn(0f, plotWidth - value.size.width),
                if (above >= 0f) above else peakCenter.y + 8.dp.toPx(), // perto de 100% não cabe em cima
            ),
        )

        if (live) {
            val last = session.steps.last()
            val now = Offset(x(last.end), y(last.percent))
            drawCircle(colors.text.copy(alpha = 0.22f), radius = 7.5.dp.toPx(), center = now)
            drawCircle(colors.text, radius = 4.dp.toPx(), center = now)
        }
    }
}

/** O começo, o meio e o fim da sessão, embaixo do gráfico. */
@Composable
private fun TimeAxis(session: SessionSummary, live: Boolean, zone: ZoneId, time: DateTimeFormatter, modifier: Modifier = Modifier) {
    val colors = PlugadoTheme.colors
    val style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TabularNumbers)
    val start = time.format(session.start.atZone(zone))
    val middle = time.format(session.start.plus(Duration.between(session.start, session.end).dividedBy(2)).atZone(zone))
    val end = if (live) stringResource(R.string.session_now) else time.format(session.end.atZone(zone))
    Box(modifier) {
        Text(start, Modifier.align(Alignment.CenterStart), style = style, color = colors.subtle)
        // Numa sessão de poucos minutos, o meio repetiria o começo ou o fim
        if (middle != start && middle != end) {
            Text(middle, Modifier.align(Alignment.Center), style = style, color = colors.subtle)
        }
        Text(end, Modifier.align(Alignment.CenterEnd), style = style, color = colors.subtle)
    }
}

@Composable
private fun SessionStat(value: String, label: String, modifier: Modifier = Modifier, autoSize: TextAutoSize? = null) {
    val colors = PlugadoTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // A base do texto fica sempre no mesmo lugar, e a altura também: com a fonte menor, o nome do
        // fone continua alinhado com os números do lado, e a legenda dele com as outras
        Text(
            value,
            Modifier.paddingFromBaseline(top = 21.sp, bottom = 5.sp),
            color = colors.text,
            autoSize = autoSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TabularNumbers),
        )
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = colors.muted)
    }
}

private val CardInset = 12.dp       // quanto o cartão da sessão aberta passa da margem da lista
private val ChartTop = 12.dp        // espaço pro valor do pico em cima da linha de 100%
private val ChartPlot = 136.dp
private val ChartBottom = 8.dp      // metade do rótulo de 0%, que fica centrado na linha
private val ChartAxisWidth = 40.dp  // os rótulos de % à direita

// O nome do fone vai de 20 sp (o tamanho dos números ao lado) até 12 sp. Se nem assim couber, corta com "…".
private val DeviceNameSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 20.sp, stepSize = 0.5.sp)

/**
 * Alarga o elemento [inset] pros dois lados sem mudar o espaço que ele ocupa: o cartão da sessão
 * aberta passa um pouco da margem, e o conteúdo dele continua alinhado com as outras linhas.
 */
private fun Modifier.bleed(inset: Dp): Modifier = layout { measurable, constraints ->
    val extra = (inset * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    layout((placeable.width - extra).coerceAtLeast(0), placeable.height) { placeable.place(-inset.roundToPx(), 0) }
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

// Prévias com os dados do protótipo: 2 h 14 min em 5 sessões, com o volume mudando em algumas.
private fun previewState(enabled: Boolean = true, empty: Boolean = false): TodayState {
    val zone = ZoneId.of("America/Sao_Paulo")
    val day = LocalDate.of(2026, 10, 21)
    val fone = Output(OutputType.BLUETOOTH, "ZMI PurPods Pro")
    fun at(h: Int, m: Int): Instant = day.atTime(h, m).atZone(zone).toInstant()
    fun volume(index: Int) = Volume(index, 30, -30f)
    val spans = if (empty) emptyList() else listOf(
        Span("manha", at(7, 42), at(7, 59), fone, volume(14)),
        Span("manha", at(7, 59), at(8, 16), fone, volume(16)),
        Span("almoco", at(12, 5), at(12, 14), fone, volume(15)),
        Span("almoco", at(12, 14), at(12, 23), fone, volume(17)),
        Span("tarde", at(15, 10), at(15, 27), Output(OutputType.WIRED, "Fone com fio"), volume(18)),
        Span("noite", at(18, 40), at(18, 47), fone, volume(17)),
        Span("noite", at(18, 47), at(18, 59), fone, volume(19)),
        Span("noite", at(18, 59), at(19, 8), fone, volume(22)),
        Span("noite", at(19, 8), at(19, 20), fone, volume(19)),
        Span("noite", at(19, 20), at(19, 28), fone, volume(17)),
        Span("agora", at(21, 23), at(21, 31), fone, volume(18)),
        Span("agora", at(21, 31), at(21, 35), fone, volume(20)),
        Span("agora", at(21, 35), at(21, 40), fone, volume(16), open = true),
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

@Preview(name = "Hoje · sessão aberta", widthDp = 412, heightDp = 1500, backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun TodaySessionOpenPreview() {
    PlugadoTheme(darkTheme = true) { TodayScreen(previewState(), PaddingValues(top = 32.dp), {}, {}, initiallyExpanded = "noite") }
}

@Preview(name = "Hoje · nada ouvido", widthDp = 412, heightDp = 915, backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun TodayEmptyPreview() {
    PlugadoTheme(darkTheme = true) { TodayScreen(previewState(empty = true), PaddingValues(top = 32.dp), {}, {}) }
}
