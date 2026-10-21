package app.jammes.plugado.monitor

import app.jammes.domain.model.AudioSnapshot
import app.jammes.domain.tracker.TrackerCommand

/** Quem quer acompanhar o monitor por dentro. Hoje é o log do build de debug. */
interface MonitorListener {
    fun onStarted() {}
    fun onSnapshot(snapshot: AudioSnapshot) {}
    fun onCommands(commands: List<TrackerCommand>) {}
    fun onStopped() {}

    /** Ninguém acompanhando (build de release). */
    object None : MonitorListener
}
