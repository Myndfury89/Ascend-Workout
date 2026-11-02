package com.ascend.core.data.motion

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.ascend.core.designsystem.motion.DEFAULT_ANIMATOR_DURATION_SCALE
import com.ascend.core.designsystem.motion.SystemMotionSettings
import com.ascend.core.designsystem.motion.isReducedMotionScale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Watches [Settings.Global.ANIMATOR_DURATION_SCALE] so the app reacts live when it changes. */
@Singleton
class AndroidSystemMotionSettings
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : SystemMotionSettings {
        override fun observeSystemReducedMotion(): Flow<Boolean> =
            callbackFlow {
                val resolver = context.contentResolver

                fun current(): Boolean =
                    isReducedMotionScale(
                        Settings.Global.getFloat(
                            resolver,
                            Settings.Global.ANIMATOR_DURATION_SCALE,
                            DEFAULT_ANIMATOR_DURATION_SCALE,
                        ),
                    )
                val observer =
                    object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            trySend(current())
                        }
                    }
                resolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                    false,
                    observer,
                )
                trySend(current())
                awaitClose { resolver.unregisterContentObserver(observer) }
            }.distinctUntilChanged()
    }
