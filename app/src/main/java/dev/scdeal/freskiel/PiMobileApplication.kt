package dev.scdeal.freskiel

import android.app.Application
import dev.scdeal.freskiel.di.AppGraph

class PiMobileApplication : Application() {
    val appGraph: AppGraph by lazy { AppGraph(this) }
}
