package app.jammes.plugado.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/**
 * Preferências da interface. O idioma não fica aqui: quem guarda é o sistema (Android 13+)
 * ou a AppCompat (Android 12).
 */
class AppPreferences(private val store: DataStore<Preferences>) {

    val theme: Flow<ThemeChoice> = store.data.map { prefs ->
        prefs[THEME]?.let { name -> ThemeChoice.entries.firstOrNull { it.name == name } } ?: ThemeChoice.SYSTEM
    }

    /** Se o primeiro uso (boas-vindas e ajustes iniciais) já foi visto. */
    val onboardingDone: Flow<Boolean> = store.data.map { it[ONBOARDING_DONE] ?: false }

    /**
     * Sessões mais curtas que isso não aparecem nem contam no dia, no histórico e na notificação.
     * Elas continuam no banco: se o mínimo baixar, voltam a contar.
     */
    val minimumSession: Flow<Duration> = store.data.map { prefs ->
        prefs[MINIMUM_SESSION_SECONDS]?.let(Duration::ofSeconds) ?: DEFAULT_MINIMUM_SESSION
    }

    suspend fun setTheme(choice: ThemeChoice) {
        store.edit { it[THEME] = choice.name }
    }

    suspend fun setOnboardingDone() {
        store.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun setMinimumSession(value: Duration) {
        store.edit { it[MINIMUM_SESSION_SECONDS] = value.seconds }
    }

    companion object {
        val DEFAULT_MINIMUM_SESSION: Duration = Duration.ofMinutes(1)

        private val THEME = stringPreferencesKey("theme")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val MINIMUM_SESSION_SECONDS = longPreferencesKey("minimum_session_seconds")
    }
}
