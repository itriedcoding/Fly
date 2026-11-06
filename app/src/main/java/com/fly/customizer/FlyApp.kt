package com.fly.customizer

import android.app.Application
import com.fly.customizer.core.Prefs
import com.fly.customizer.data.FlyRepository

/**
 * Process-wide holder for the shared preferences wrapper and the SQLite repository.
 */
class FlyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        repository = FlyRepository(this)
    }

    companion object {
        lateinit var instance: FlyApp
            private set

        lateinit var prefs: Prefs
            private set

        lateinit var repository: FlyRepository
            private set
    }
}
