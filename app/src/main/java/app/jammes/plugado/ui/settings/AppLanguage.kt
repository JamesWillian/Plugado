package app.jammes.plugado.ui.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import app.jammes.plugado.R
import app.jammes.plugado.ui.common.capitalized

/**
 * Os idiomas do app. Quem guarda a escolha é o sistema (Android 13+, que também mostra a opção
 * nos ajustes do celular) ou a AppCompat (Android 12). Trocar recria a tela no idioma novo.
 */
enum class AppLanguage(val tag: String?, @get:StringRes val label: Int) {
    SYSTEM(null, R.string.language_system),
    PORTUGUESE("pt-BR", R.string.language_pt),
    ENGLISH("en", R.string.language_en),
    SPANISH("es", R.string.language_es);

    companion object {
        fun current(): AppLanguage {
            val chosen = AppCompatDelegate.getApplicationLocales()
            if (chosen.isEmpty) return SYSTEM
            return when (chosen[0]?.language) {
                "pt" -> PORTUGUESE
                "en" -> ENGLISH
                "es" -> SPANISH
                else -> SYSTEM
            }
        }

        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(
                language.tag?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList(),
            )
        }

        /** O idioma do celular escrito nele mesmo ("Português (Brasil)"), pra explicar o "Padrão do sistema". */
        fun systemLanguageName(context: Context): String? =
            LocaleManagerCompat.getSystemLocales(context)[0]?.let { it.getDisplayName(it).capitalized(it) }
    }
}
