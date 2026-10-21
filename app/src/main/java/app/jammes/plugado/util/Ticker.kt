package app.jammes.plugado.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/** Emite a cada [period], pra sempre. Quem coleta decide quando parar. */
fun ticker(period: Duration): Flow<Unit> = flow {
    while (true) {
        delay(period)
        emit(Unit)
    }
}
