package app.jammes.plugado.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.plugado.data.prefs.AppPreferences
import app.jammes.plugado.monitor.MonitorSwitch
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val prefs: AppPreferences,
    private val switch: MonitorSwitch,
) : ViewModel() {

    /** Fim do primeiro uso. Se [turnOn], liga o monitoramento antes. */
    fun finish(turnOn: Boolean) {
        viewModelScope.launch {
            if (turnOn) switch.turnOn()
            prefs.setOnboardingDone()
        }
    }
}
