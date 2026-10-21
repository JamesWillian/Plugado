package app.jammes.plugado.debuglog

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * O log só existe no build de debug. No release, a aba Today mostra este aviso até a tela de
 * hoje ficar pronta; aí a chamada no PlugadoApp troca e este arquivo pode ser apagado.
 */
@Composable
fun DebugLogScreen(modifier: Modifier = Modifier) {
    Text("A tela de hoje ainda não está pronta.", modifier.padding(16.dp))
}
