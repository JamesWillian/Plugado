package app.jammes.plugado.audio

import android.media.AudioDeviceInfo
import android.media.AudioManager
import app.jammes.domain.model.AudioKind
import app.jammes.domain.model.AudioSnapshot
import app.jammes.domain.model.Output
import app.jammes.domain.model.OutputType
import app.jammes.domain.model.Volume
import java.time.Instant

// Regras puras: só usam constantes do Android (que o compilador copia pro código),
// nunca chamam o sistema. Por isso rodam em teste na JVM, sem aparelho.

/** O que o AudioManager informou, sem decisão nenhuma. */
data class AudioReading(
    val musicActive: Boolean,
    val mode: Int,                  // AudioManager.MODE_*
    val outputs: List<OutputDevice>,
)

/** Uma saída conectada, do jeito que o Android informa. */
data class OutputDevice(
    val type: Int,                  // AudioDeviceInfo.TYPE_*
    val name: String,               // productName
)

/** Lê o volume de um stream numa saída. Vem de fora pra que as regras fiquem puras. */
fun interface VolumeLookup {
    fun volume(stream: Int, deviceType: Int): Volume
}

private val CALL_MODES = setOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION)

/** Saídas que contam como fone. O A2DP e o SCO do mesmo fone viram um `Output` só. */
private val HEADPHONE_TYPES = mapOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP to OutputType.BLUETOOTH,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO to OutputType.BLUETOOTH,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES to OutputType.WIRED,
    AudioDeviceInfo.TYPE_WIRED_HEADSET to OutputType.WIRED,
    AudioDeviceInfo.TYPE_USB_HEADSET to OutputType.USB,
    AudioDeviceInfo.TYPE_BLE_HEADSET to OutputType.BLE,
    AudioDeviceInfo.TYPE_HEARING_AID to OutputType.HEARING_AID,
)

/**
 * Por onde a mídia sai, em ordem de preferência. O SCO fica de fora: com só o SCO conectado,
 * a mídia ainda sai no alto-falante. Saídas internas (incluindo o `TYPE_FM`) nunca entram.
 */
private val MEDIA_OUTPUTS = listOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_HEARING_AID,
)

/** Por onde a chamada sai. Suposição: chamadas ainda não foram testadas no aparelho. */
private val CALL_OUTPUTS = listOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
)

/**
 * Monta o snapshot a partir da leitura crua.
 * [notHeadphones] são as saídas que o usuário marcou como "não é fone" (caixa de som, carro).
 */
fun AudioReading.toSnapshot(
    at: Instant,
    notHeadphones: Set<Output>,
    volumes: VolumeLookup,
): AudioSnapshot {
    val inCall = mode in CALL_MODES
    val device = firstConnected(if (inCall) CALL_OUTPUTS else MEDIA_OUTPUTS)
    val stream = if (inCall) AudioManager.STREAM_VOICE_CALL else AudioManager.STREAM_MUSIC
    // O dB é o da saída onde o som toca de fato (A2DP pra mídia, não SCO). Sem fone, alto-falante.
    val volume = volumes.volume(stream, device?.type ?: AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)

    return AudioSnapshot(
        at = at,
        playing = musicActive || inCall,
        kind = if (inCall) AudioKind.CALL else AudioKind.MEDIA,
        output = device?.toOutput()?.takeUnless { it in notHeadphones },
        volume = volume,
    )
}

private fun AudioReading.firstConnected(preference: List<Int>): OutputDevice? =
    preference.firstNotNullOfOrNull { type -> outputs.firstOrNull { it.type == type } }

private fun OutputDevice.toOutput() = Output(HEADPHONE_TYPES.getValue(type), name)
