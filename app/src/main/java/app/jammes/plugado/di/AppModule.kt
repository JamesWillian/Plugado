package app.jammes.plugado.di

import android.media.AudioManager
import app.jammes.domain.tracker.SegmentTracker
import app.jammes.plugado.audio.AudioModeSource
import app.jammes.plugado.audio.AudioStateAggregator
import app.jammes.plugado.audio.OutputDeviceSource
import app.jammes.plugado.audio.PlaybackSource
import app.jammes.plugado.audio.SystemAudioStateReader
import app.jammes.plugado.audio.VolumeSource
import app.jammes.plugado.monitor.CommandSink
import app.jammes.plugado.monitor.ListeningMonitor
import kotlinx.coroutines.flow.merge
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {

    single<AudioManager> { androidContext().getSystemService(AudioManager::class.java) }

    factory {
        val am: AudioManager = get()
        val context = androidContext()
        val sources = listOf(
            PlaybackSource(am),
            AudioModeSource(am, context.mainExecutor),
            OutputDeviceSource(am),
            VolumeSource(context.contentResolver),
        )
        AudioStateAggregator(
            triggers = sources.map { it.triggers }.merge(),
            reader = SystemAudioStateReader(am),
        )
    }

    // Por enquanto os comandos só aparecem no log de debug. Na fase 5 o Room entra aqui.
    single<CommandSink> { CommandSink { } }

    // Factory: cada vez que o serviço liga, um tracker novo, sem estado da vez anterior.
    factory {
        ListeningMonitor(
            snapshots = get<AudioStateAggregator>().snapshots,
            tracker = SegmentTracker(),
            sink = get(),
            listener = get(),
        )
    }
}
