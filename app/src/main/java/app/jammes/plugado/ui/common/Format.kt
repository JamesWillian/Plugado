package app.jammes.plugado.ui.common

import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.text.FieldPosition
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** O idioma em que a tela está sendo mostrada (o do app, se o usuário escolheu um). */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** "2 h 14 min", "48 min". As abreviações são iguais em português, inglês e espanhol. */
fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes()
    return if (minutes >= 60) String.format(Locale.ROOT, "%d h %02d min", minutes / 60, minutes % 60) else "$minutes min"
}

/** A versão curta, pros rótulos do gráfico: "2 h 14", "48 min". */
fun formatDurationShort(duration: Duration): String {
    val minutes = duration.toMinutes()
    return if (minutes >= 60) String.format(Locale.ROOT, "%d h %02d", minutes / 60, minutes % 60) else "$minutes min"
}

/** "58%" no jeito de cada idioma ("58 %" em espanhol). */
fun formatPercent(percent: Int, locale: Locale): String =
    NumberFormat.getPercentInstance(locale).format(percent / 100.0)

/** Horário no formato que o usuário escolheu no celular (24 h ou AM/PM). */
@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
    val context = LocalContext.current
    val locale = currentLocale()
    return remember(locale) {
        DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale)
    }
}

/** "Quarta, 21 de outubro" / "Wednesday, October 21". */
fun formatLongDate(day: LocalDate, locale: Locale): String =
    bestPattern(locale, "EEEEdMMMM").format(day).capitalized(locale)

/** "21 de outubro" / "October 21". */
fun formatDayMonth(day: LocalDate, locale: Locale): String = bestPattern(locale, "dMMMM").format(day)

/** "Qua" / "Wed" / "Mié", sem o ponto da abreviação. */
fun formatWeekdayShort(day: LocalDate, locale: Locale): String =
    day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".").capitalized(locale)

/** "sábado" / "Saturday": o nome do dia como ele aparece no meio de uma frase. */
fun formatWeekday(day: LocalDate, locale: Locale): String = day.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

/** "15 – 21 de outubro" / "October 15 – 21", na ordem de cada idioma. */
fun formatDayRange(from: LocalDate, to: LocalDate, locale: Locale): String {
    val zone = ZoneId.systemDefault()
    val interval = DateInterval(
        from.atStartOfDay(zone).toInstant().toEpochMilli(),
        to.atStartOfDay(zone).toInstant().toEpochMilli(),
    )
    return DateIntervalFormat.getInstance("dMMMM", locale).format(interval, StringBuffer(), FieldPosition(0)).toString()
}

fun String.capitalized(locale: Locale): String = replaceFirstChar { it.titlecase(locale) }

private fun bestPattern(locale: Locale, skeleton: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
