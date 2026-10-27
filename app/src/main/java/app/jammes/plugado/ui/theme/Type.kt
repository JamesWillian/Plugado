package app.jammes.plugado.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.jammes.plugado.R

/** Inter (licença OFL, em assets/licenses). Os quatro pesos que o app usa. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * Algarismos de largura fixa, pra números que ficam em coluna (horários, durações na lista,
 * eixos). Números grandes e sozinhos ficam com os algarismos normais.
 */
const val TabularNumbers = "tnum"

private fun inter(weight: FontWeight, size: Int, lineHeight: Int, letterSpacing: TextUnit = 0.sp) =
    TextStyle(
        fontFamily = Inter,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing,
    )

// Nos tamanhos grandes o espaçamento fica negativo, como no protótipo.
val Typography = Typography(
    displayLarge = inter(FontWeight.Bold, 64, 68, (-2.5).sp),     // número de destaque da tela Hoje
    displayMedium = inter(FontWeight.Bold, 48, 52, (-1.8).sp),    // total do Histórico
    displaySmall = inter(FontWeight.Bold, 40, 44, (-1.4).sp),     // título das boas-vindas
    headlineLarge = inter(FontWeight.Bold, 32, 38, (-0.8).sp),    // título de cada tela
    headlineMedium = inter(FontWeight.SemiBold, 28, 34, (-0.5).sp),
    headlineSmall = inter(FontWeight.Bold, 24, 30, (-0.6).sp),    // valor dos quadros de número
    titleLarge = inter(FontWeight.Bold, 20, 26, (-0.4).sp),       // título de seção
    titleMedium = inter(FontWeight.SemiBold, 16, 22),             // título de linha
    titleSmall = inter(FontWeight.SemiBold, 14, 20),              // rótulo de seção
    bodyLarge = inter(FontWeight.Normal, 16, 24),
    bodyMedium = inter(FontWeight.Normal, 14, 20),
    bodySmall = inter(FontWeight.Normal, 12, 17),
    labelLarge = inter(FontWeight.SemiBold, 15, 20),              // botões
    labelMedium = inter(FontWeight.Medium, 13, 18),
    labelSmall = inter(FontWeight.Medium, 12, 16),                // abas, eixos
)
