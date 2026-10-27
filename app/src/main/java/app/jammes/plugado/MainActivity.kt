package app.jammes.plugado

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.jammes.plugado.monitor.MonitorSwitch
import app.jammes.plugado.ui.PlugadoRoot
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/** AppCompatActivity, e não ComponentActivity, por causa da troca de idioma no Android 12. */
class MainActivity : AppCompatActivity() {

    private val switch: MonitorSwitch by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Se o monitoramento estava ligado mas o processo foi fechado (pelo fabricante ou pelo
        // gerenciador de tarefas), ele volta quando o app abre.
        if (savedInstanceState == null) lifecycleScope.launch { switch.restoreIfEnabled() }
        setContent { PlugadoRoot() }
    }
}
