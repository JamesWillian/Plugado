package app.jammes.plugado.ui.common

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

/** Os rótulos do tempo mínimo nos Ajustes ("Sem mínimo" vem do strings.xml e fica de fora). */
class FormatTest {

    @Test
    fun `menos de um minuto aparece em segundos`() {
        assertEquals("30 s", formatMinutesSeconds(Duration.ofSeconds(30)))
    }

    @Test
    fun `minuto redondo não mostra os segundos`() {
        assertEquals("2 min", formatMinutesSeconds(Duration.ofMinutes(2)))
    }

    // Exercício do James (veja o CLAUDE.md): minutos e segundos juntos, como "1 min 30 s".
}
