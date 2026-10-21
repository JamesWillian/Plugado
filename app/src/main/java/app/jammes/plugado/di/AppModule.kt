package app.jammes.plugado.di

import android.media.AudioManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.jammes.domain.tracker.SegmentTracker
import app.jammes.plugado.audio.AudioModeSource
import app.jammes.plugado.audio.AudioStateAggregator
import app.jammes.plugado.audio.OutputDeviceSource
import app.jammes.plugado.audio.PlaybackSource
import app.jammes.plugado.audio.SystemAudioStateReader
import app.jammes.plugado.audio.VolumeSource
import app.jammes.plugado.data.ListeningRepository
import app.jammes.plugado.data.db.AppDatabase
import app.jammes.plugado.data.prefs.MonitorSettings
import app.jammes.plugado.monitor.CommandSink
import app.jammes.plugado.monitor.ListeningMonitor
import app.jammes.plugado.monitor.MonitorSwitch
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
            notHeadphones = get<ListeningRepository>().notHeadphones,
        )
    }

    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, "plugado.db").build() }
    single { ListeningRepository(get()) }
    single<CommandSink> { get<ListeningRepository>() }

    single {
        MonitorSettings(
            PreferenceDataStoreFactory.create { androidContext().preferencesDataStoreFile("monitor") },
        )
    }
    single { MonitorSwitch(androidContext(), get()) }

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
