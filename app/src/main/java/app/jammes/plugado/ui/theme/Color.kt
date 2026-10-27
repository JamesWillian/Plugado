package app.jammes.plugado.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * As cores do app: preto e branco, cinzas pra hierarquia e um vermelho só pra ação destrutiva.
 * Os cinzas de texto passam no contraste AA (4,5:1) contra o fundo; as barras, em 3:1.
 */
@Immutable
data class PlugadoColors(
    val background: Color,
    val surface: Color,     // cartões
    val chip: Color,        // círculos dos ícones, pílulas, controle segmentado
    val track: Color,       // fundo da linha do dia
    val line: Color,        // divisórias e linhas de grade
    val text: Color,
    val muted: Color,       // texto secundário
    val subtle: Color,      // legendas, eixos, abas inativas
    val bar: Color,         // barras não selecionadas do gráfico
    val inverse: Color,     // botão principal
    val onInverse: Color,
    val sheet: Color,       // painel que sobe de baixo
    val handle: Color,
    val scrim: Color,
    val danger: Color,
    val onDanger: Color,
)

val DarkColors = PlugadoColors(
    background = Color(0xFF000000),
    surface = Color(0xFF141414),
    chip = Color(0xFF1F1F1F),
    track = Color(0xFF1A1A1A),
    line = Color(0xFF262626),
    text = Color(0xFFFFFFFF),
    muted = Color(0xFFA3A3A3),
    subtle = Color(0xFF8A8A8A),
    bar = Color(0xFF616161),
    inverse = Color(0xFFFFFFFF),
    onInverse = Color(0xFF000000),
    sheet = Color(0xFF161616),
    handle = Color(0xFF4A4A4A),
    scrim = Color(0xB8000000),
    danger = Color(0xFFFF6B6B),
    onDanger = Color(0xFF000000),
)

val LightColors = PlugadoColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5F5),
    chip = Color(0xFFEDEDED),
    track = Color(0xFFEFEFEF),
    line = Color(0xFFE6E6E6),
    text = Color(0xFF000000),
    muted = Color(0xFF5E5E5E),
    subtle = Color(0xFF6B6B6B),
    bar = Color(0xFF8C8C8C),
    inverse = Color(0xFF000000),
    onInverse = Color(0xFFFFFFFF),
    sheet = Color(0xFFFFFFFF),
    handle = Color(0xFFC7C7C7),
    scrim = Color(0x66000000),
    danger = Color(0xFFD92D20),
    onDanger = Color(0xFFFFFFFF),
)
