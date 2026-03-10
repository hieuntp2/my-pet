package com.aipet.brain.app

import android.app.Application
import com.aipet.brain.app.debug.AppCrashReporter

class PetBrainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCrashReporter.install(this)
    }
}
