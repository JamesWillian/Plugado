package app.jammes.plugado.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Os ícones do protótipo, com os mesmos traços (24 × 24, linha de 1,75). São desenhados em preto;
 * o `Icon` pinta na cor do `tint`.
 */
object PlugadoIcons {
    val Today = icon(
        "today", rect(3.5f, 5f, 17f, 15.5f, 3f), "M3.5 10h17", "M8 3v4", "M16 3v4",
        filled = listOf(circle(12f, 15.25f, 1.75f)),
    )
    val History = icon("history", "M5 20v-7", "M12 20V5", "M19 20v-10", strokeWidth = 2f)
    /** O fone do ícone do app (`ic_plugado`) com o traço dos ícones da interface, centrado na grade. */
    val Headphones = icon(
        "headphones",
        "M4 13.5H6.5a1.5 1.5 0 0 1 1.5 1.5V19a1.5 1.5 0 0 1 -1.5 1.5H6a2 2 0 0 1 -2 -2V11.5" +
            "A8 8 0 0 1 20 11.5V18.5a2 2 0 0 1 -2 2h-0.5a1.5 1.5 0 0 1 -1.5 -1.5V15a1.5 1.5 0 0 1 1.5 -1.5H20",
    )
    val Settings = icon(
        "settings", "M4 7h9", "M19 7h1", circle(16f, 7f, 2.5f), "M4 17h1", "M11 17h9", circle(8f, 17f, 2.5f),
    )
    val Bluetooth = icon("bluetooth", "M7 7l10 10-5 5V2l5 5L7 17")
    val Wired = icon("wired", "M9 7V3", "M15 7V3", rect(7f, 7f, 10f, 7f, 2f), "M12 14v8")
    val Info = icon("info", circle(12f, 12f, 9f), "M12 11v5", "M12 7.5v0.5")
    val ArrowUp = icon("arrow_up", "M12 19V5", "M6 11l6-6 6 6", strokeWidth = 2f)
    val ArrowDown = icon("arrow_down", "M12 5v14", "M6 13l6 6 6-6", strokeWidth = 2f)
    val ChevronRight = icon("chevron_right", "M9 5l7 7-7 7", strokeWidth = 2f)
    val ChevronLeft = icon("chevron_left", "M15 5l-7 7 7 7", strokeWidth = 2f)
    val External = icon(
        "external", "M14 4h6v6", "M20 4l-9 9", "M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5",
    )
    val Contrast = icon("contrast", circle(12f, 12f, 8.5f), filled = listOf("M12 3.5a8.5 8.5 0 0 1 0 17z"))
    val Globe = icon(
        "globe", circle(12f, 12f, 9f), "M3 12h18", "M12 3a14 14 0 0 1 0 18", "M12 3a14 14 0 0 0 0 18",
    )
    val Battery = icon(
        "battery", rect(3f, 7f, 16f, 10f, 2.5f), "M21.5 10.5v3", "M7 10.5v3", "M10.5 10.5v3", "M14 10.5v3",
    )
    val Shield = icon("shield", "M12 3l7 3v5c0 4.5-3 8.5-7 10-4-1.5-7-5.5-7-10V6z")
    val Trash = icon("trash", "M4 7h16", "M9 7V4.5h6V7", "M6.5 7l1 13h9l1-13", "M10 11v5", "M14 11v5")
    val Lock = icon("lock", rect(5f, 11f, 14f, 10f, 2f), "M8 11V8a4 4 0 0 1 8 0v3")
    val Clock = icon("clock", circle(12f, 12f, 9f), "M12 7v5l3 2")
    val Volume = icon("volume", "M4 9v6h4l5 4V5L8 9z", "M16.5 8.5a5 5 0 0 1 0 7", "M19 6a8.5 8.5 0 0 1 0 12")
    val Bell = icon("bell", "M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20.5a2 2 0 0 0 4 0")
    val Check = icon("check", "M5 12.5l4.5 4.5L19 7.5", strokeWidth = 2.25f)
    val MicOff = icon(
        "mic_off", "M3 3l18 18", "M9 9v3a3 3 0 0 0 5.1 2.1", "M15 9.3V5a3 3 0 0 0-5.9-0.7",
        "M19 11a7 7 0 0 1-0.8 3.2", "M5 11a7 7 0 0 0 11.3 5.5", "M12 18v3",
    )
    val Phone = icon("phone", rect(6f, 2.5f, 12f, 19f, 2.5f), "M11 18.5h2")
}

private fun icon(
    name: String,
    vararg strokes: String,
    filled: List<String> = emptyList(),
    strokeWidth: Float = 1.75f,
): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
    strokes.forEach { d ->
        addPath(
            pathData = addPathNodes(d),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    filled.forEach { d -> addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black)) }
}.build()

/** Um círculo como caminho de SVG (o `<circle>` do protótipo). */
private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

/** Um retângulo de cantos arredondados como caminho de SVG (o `<rect rx>` do protótipo). */
private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
        "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"
