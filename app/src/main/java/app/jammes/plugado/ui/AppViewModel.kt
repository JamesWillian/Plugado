package app.jammes.plugado.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.plugado.data.prefs.AppPreferences
import app.jammes.plugado.data.prefs.ThemeChoice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** O que a raiz do app precisa saber antes de desenhar qualquer tela. Null = ainda lendo. */
class AppViewModel(prefs: AppPreferences) : ViewModel() {
    val theme: StateFlow<ThemeChoice?> = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val onboardingDone: StateFlow<Boolean?> = prefs.onboardingDone.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
