package com.ascend.core.data.motion

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ascend.core.designsystem.motion.MotionPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.motionPreferencesDataStore by preferencesDataStore(name = "motion_preferences")

/** DataStore-backed persistence of the in-app reduced-motion toggle (survives restarts). */
@Singleton
class DataStoreMotionPreferences
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : MotionPreferences {
        override fun observeInAppReducedMotion(): Flow<Boolean> =
            context.motionPreferencesDataStore.data
                .map { it[IN_APP_REDUCED_MOTION] ?: false }
                .distinctUntilChanged()

        override suspend fun setInAppReducedMotion(value: Boolean) {
            context.motionPreferencesDataStore.edit { it[IN_APP_REDUCED_MOTION] = value }
        }

        private companion object {
            val IN_APP_REDUCED_MOTION = booleanPreferencesKey("in_app_reduced_motion")
        }
    }
