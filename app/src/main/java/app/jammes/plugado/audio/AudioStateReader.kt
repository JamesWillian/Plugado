package app.jammes.plugado.audio

import android.media.AudioManager
import app.jammes.domain.model.AudioSnapshot
import app.jammes.domain.model.Output
import app.jammes.domain.model.Volume
import java.time.Instant

/** Lê o estado atual do áudio. Interface pra que o agregador possa ser testado com um leitor falso. */
fun interface AudioStateReader {
    fun read(at: Instant, notHeadphones: Set<Output>): AudioSnapshot
}

/** O leitor de verdade: só pergunta pro AudioManager. As decisões ficam no [toSnapshot]. */
class SystemAudioStateReader(private val am: AudioManager) : AudioStateReader {

    private val volumes = VolumeLookup { stream, deviceType ->
        val index = am.getStreamVolume(stream)
        Volume(
            index = index,
            max = am.getStreamMaxVolume(stream),
            // Lança exceção se o aparelho não aceitar o tipo de saída. Sem dB, fica NaN.
            db = runCatching { am.getStreamVolumeDb(stream, index, deviceType) }
                .getOrDefault(Float.NaN),
        )
    }

    override fun read(at: Instant, notHeadphones: Set<Output>): AudioSnapshot =
        AudioReading(
            musicActive = am.isMusicActive,
            mode = am.mode,
            outputs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .map { OutputDevice(it.type, it.productName.toString()) },
        ).toSnapshot(at, notHeadphones, volumes)
}
