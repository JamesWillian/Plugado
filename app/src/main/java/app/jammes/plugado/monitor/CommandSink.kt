package app.jammes.plugado.monitor

import app.jammes.domain.tracker.TrackerCommand

/** Executa os comandos do tracker. Na fase 5 vira o repositório do Room. */
fun interface CommandSink {
    suspend fun execute(command: TrackerCommand)
}
