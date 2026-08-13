package com.ascend

import android.app.Application
import com.ascend.core.data.repository.BuildProfileRefresher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AscendApplication : Application() {
    @Inject
    lateinit var buildProfileRefresher: BuildProfileRefresher

    override fun onCreate() {
        super.onCreate()
        // Keep the read-only Build snapshot fresh as new verified activity is written. Guarded so a
        // boot without Hilt injection (e.g. some test harnesses) can't crash onCreate.
        if (::buildProfileRefresher.isInitialized) {
            buildProfileRefresher.start()
        }
    }
}
