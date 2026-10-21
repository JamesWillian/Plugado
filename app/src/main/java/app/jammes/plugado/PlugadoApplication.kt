package app.jammes.plugado

import android.app.Application
import app.jammes.plugado.di.appModule
import app.jammes.plugado.di.variantModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PlugadoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PlugadoApplication)
            // variantModule existe duas vezes: em src/debug e em src/release.
            modules(appModule, variantModule)
        }
    }
}
