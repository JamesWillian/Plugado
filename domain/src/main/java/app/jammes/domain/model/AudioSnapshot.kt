package app.jammes.domain.model

import java.time.Instant

data class AudioSnapshot(
    val at: Instant,
    val playing: Boolean,      // isMusicActive, ou em chamada
    val kind: AudioKind,       // MEDIA ou CALL
    val output: Output?,       // null = sem fone válido pro tipo de áudio
    val volume: Volume,
)  {
    /**
     * Só conta como escuta se tem som, saindo por um fone, com volume acima de zero.
     * No zero não se ouve nada (e o dB vem -Infinity), então funciona como uma pausa.
     */
    val isListening: Boolean get() = playing && output != null && volume.index > 0
}