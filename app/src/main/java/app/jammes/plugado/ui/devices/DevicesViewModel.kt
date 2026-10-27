package app.jammes.plugado.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.plugado.data.DeviceTotal
import app.jammes.plugado.data.ListeningRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DevicesViewModel(private val repository: ListeningRepository) : ViewModel() {

    val devices: StateFlow<List<DeviceTotal>?> =
        repository.devicesWithTotals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setCounts(device: DeviceTotal, counts: Boolean) {
        viewModelScope.launch { repository.setCounts(device.output, counts) }
    }
}
