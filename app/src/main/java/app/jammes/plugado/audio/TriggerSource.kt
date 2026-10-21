package app.jammes.plugado.audio

import kotlinx.coroutines.flow.Flow

/**
 * Uma fonte só avisa que algo mudou. Quem lê o estado do áudio é o agregador:
 * assim a leitura é sempre completa e na hora, e não um pedaço de estado de cada callback.
 */
interface TriggerSource {
    val triggers: Flow<Unit>
}
