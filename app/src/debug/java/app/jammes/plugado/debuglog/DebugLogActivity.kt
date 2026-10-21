package app.jammes.plugado.debuglog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.jammes.plugado.monitor.MonitorService
import app.jammes.plugado.ui.theme.PlugadoTheme
import org.koin.android.ext.android.inject

/** Tela de log do monitor (só no debug, com ícone próprio: "Plugado log"). Substitui o spike. */
class DebugLogActivity : ComponentActivity() {

    private val log: DebugLog by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlugadoTheme {
                DebugLogScreen(log)
            }
        }
    }
}

@Composable
private fun DebugLogScreen(log: DebugLog) {
    val context = LocalContext.current
    val entries by log.entries.collectAsState()
    val running by log.running.collectAsState()
    var scenario by remember { mutableStateOf("") }

    // Liga mesmo se a permissão for negada: o serviço roda, só a notificação não aparece.
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { MonitorService.start(context) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    when {
                        running -> MonitorService.stop(context)
                        needsNotificationPermission(context) ->
                            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> MonitorService.start(context)
                    }
                }) { Text(if (running) "Desligar" else "Ligar") }
                OutlinedButton(onClick = log::clear) { Text("Limpar") }
                OutlinedButton(onClick = { shareLog(context, entries) }) { Text("Compartilhar") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = scenario,
                    onValueChange = { scenario = it },
                    label = { Text("Cenário (ex: Spotify + fone BT)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    log.mark(scenario.ifBlank { "marca" })
                    scenario = ""
                }) { Text("Marcar") }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(entries) { entry ->
                    Text(
                        text = entry,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun needsNotificationPermission(context: Context) =
    Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

private fun shareLog(context: Context, entries: List<String>) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Plugado: log do monitor")
        putExtra(Intent.EXTRA_TEXT, entries.reversed().joinToString("\n")) // ordem cronológica
    }
    context.startActivity(Intent.createChooser(send, "Compartilhar log"))
}
