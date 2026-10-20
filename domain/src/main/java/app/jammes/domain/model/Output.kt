package app.jammes.domain.model

data class Output(
    val type: OutputType,      // BLUETOOTH, WIRED, USB, BLE, HEARING_AID
    val name: String,          // "ZMI PurPods Pro": junta A2DP + SCO num aparelho só
)