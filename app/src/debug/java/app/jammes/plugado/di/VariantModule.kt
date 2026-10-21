package app.jammes.plugado.di

import app.jammes.plugado.debuglog.DebugLog
import app.jammes.plugado.monitor.MonitorListener
import org.koin.dsl.module

/** No debug, o log em memória acompanha o monitor. */
val variantModule = module {
    single { DebugLog() }
    single<MonitorListener> { get<DebugLog>() }
}
