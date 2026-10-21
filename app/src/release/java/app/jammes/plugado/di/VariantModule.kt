package app.jammes.plugado.di

import app.jammes.plugado.monitor.MonitorListener
import org.koin.dsl.module

/** No release, ninguém acompanha o monitor por dentro. */
val variantModule = module {
    single<MonitorListener> { MonitorListener.None }
}
