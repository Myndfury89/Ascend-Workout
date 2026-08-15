package com.ascend

import android.app.Application
import com.ascend.core.data.community.AuthLinkCoordinator
import com.ascend.core.data.repository.BuildProfileRefresher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AscendApplication : Application() {
    @Inject
    lateinit var buildProfileRefresher: BuildProfileRefresher

    @Inject
    lateinit var authLinkCoordinator: AuthLinkCoordinator

    override fun onCreate() {
        super.onCreate()
        // Guarded so a boot without Hilt injection (e.g. some test harnesses) can't crash onCreate.
        // Keep the read-only Build snapshot fresh as new verified activity is written.
        if (::buildProfileRefresher.isInitialized) {
            buildProfileRefresher.start()
        }
        // Restore/track the remote identity link from the live auth session (never touches Room).
        if (::authLinkCoordinator.isInitialized) {
            authLinkCoordinator.start()
        }
    }
}
