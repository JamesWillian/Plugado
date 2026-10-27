package app.jammes.plugado.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.domain.model.Output
import app.jammes.domain.model.OutputType
import app.jammes.plugado.R
import app.jammes.plugado.data.DeviceTotal
import app.jammes.plugado.ui.common.IconBadge
import app.jammes.plugado.ui.common.InfoNote
import app.jammes.plugado.ui.common.PlugadoSwitch
import app.jammes.plugado.ui.common.ScreenTitle
import app.jammes.plugado.ui.common.SectionTitle
import app.jammes.plugado.ui.common.bottomHairline
import app.jammes.plugado.ui.common.formatDuration
import app.jammes.plugado.ui.common.outputKindLabel
import app.jammes.plugado.ui.theme.PlugadoIcons
import app.jammes.plugado.ui.theme.PlugadoTheme
import org.koin.compose.viewmodel.koinViewModel
import java.time.Duration

@Composable
fun DevicesRoute(contentPadding: PaddingValues, viewModel: DevicesViewModel = koinViewModel()) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    devices?.let { DevicesScreen(it, contentPadding, onCountsChange = viewModel::setCounts) }
}

@Composable
fun DevicesScreen(
    devices: List<DeviceTotal>,
    contentPadding: PaddingValues,
    onCountsChange: (DeviceTotal, Boolean) -> Unit,
) {
    val colors = PlugadoTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle(stringResource(R.string.devices_title))
            Text(
                stringResource(R.string.devices_intro),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                color = colors.muted,
            )
        }
        if (devices.isEmpty()) {
            EmptyCard()
        } else {
            Column { devices.forEach { DeviceRow(it, onCountsChange) } }
        }
        InfoNote(stringResource(R.string.devices_note))
    }
}

@Composable
private fun DeviceRow(device: DeviceTotal, onCountsChange: (DeviceTotal, Boolean) -> Unit) {
    val colors = PlugadoTheme.colors
    val kind = stringResource(outputKindLabel(device.output.type))
    val detail = if (device.counts) {
        stringResource(R.string.device_total, kind, formatDuration(device.listened))
    } else {
        stringResource(R.string.device_not_counted, kind)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .bottomHairline(colors.line)
            .toggleable(value = device.counts, role = Role.Switch, onValueChange = { onCountsChange(device, it) }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBadge(
            if (device.output.type == OutputType.WIRED || device.output.type == OutputType.USB) PlugadoIcons.Wired else PlugadoIcons.Bluetooth,
            size = 44.dp,
            iconSize = 22.dp,
            tint = if (device.counts) colors.text else colors.subtle,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                device.output.name.ifBlank { kind },
                style = MaterialTheme.typography.titleMedium,
                color = if (device.counts) colors.text else colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
        PlugadoSwitch(checked = device.counts)
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
        SectionTitle(stringResource(R.string.devices_empty_title))
        Text(
            stringResource(R.string.devices_empty_body),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = colors.muted,
        )
    }
}

@Preview(name = "Dispositivos · escuro", widthDp = 412, heightDp = 915, backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun DevicesPreview() {
    val devices = listOf(
        DeviceTotal(Output(OutputType.BLUETOOTH, "ZMI PurPods Pro"), counts = true, Duration.ofMinutes(2290)),
        DeviceTotal(Output(OutputType.WIRED, "Fone com fio"), counts = true, Duration.ofMinutes(260)),
        DeviceTotal(Output(OutputType.BLUETOOTH, "JBL Flip 5"), counts = false, Duration.ofMinutes(365)),
        DeviceTotal(Output(OutputType.BLUETOOTH, "CAR MULTIMEDIA"), counts = false, Duration.ofMinutes(700)),
    )
    PlugadoTheme(darkTheme = true) { DevicesScreen(devices, PaddingValues(top = 32.dp)) { _, _ -> } }
}
