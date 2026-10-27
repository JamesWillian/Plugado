package app.jammes.plugado.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.plugado.data.ListeningRepository
import app.jammes.plugado.data.prefs.AppPreferences
import app.jammes.plugado.data.prefs.MonitorSettings
import app.jammes.plugado.data.prefs.ThemeChoice
import app.jammes.plugado.monitor.MonitorSwitch
import java.time.Duration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefs: AppPreferences,
    settings: MonitorSettings,
    private val switch: MonitorSwitch,
    private val repository: ListeningRepository,
) : ViewModel() {

    val theme: StateFlow<ThemeChoice> =
        prefs.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeChoice.SYSTEM)

    val monitoring: StateFlow<Boolean> =
        settings.enabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val minimumSession: StateFlow<Duration> = prefs.minimumSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT_MINIMUM_SESSION)

    private val deletedEvents = Channel<Unit>(Channel.CONFLATED)

    /** Avisa a tela quando os dados terminaram de ser apagados (pra mostrar a confirmação). */
    val deleted: Flow<Unit> = deletedEvents.receiveAsFlow()

    fun setTheme(choice: ThemeChoice) {
        viewModelScope.launch { prefs.setTheme(choice) }
    }

    fun setMonitoring(on: Boolean) {
        viewModelScope.launch { if (on) switch.turnOn() else switch.turnOff() }
    }

    fun setMinimumSession(value: Duration) {
        viewModelScope.launch { prefs.setMinimumSession(value) }
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
            deletedEvents.send(Unit)
        }
    }
}
