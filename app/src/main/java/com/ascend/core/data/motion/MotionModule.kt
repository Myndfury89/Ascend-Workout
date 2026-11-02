package com.ascend.core.data.motion

import com.ascend.core.designsystem.motion.MotionPreferences
import com.ascend.core.designsystem.motion.SystemMotionSettings
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MotionModule {
    @Binds
    abstract fun bindSystemMotionSettings(impl: AndroidSystemMotionSettings): SystemMotionSettings

    @Binds
    abstract fun bindMotionPreferences(impl: DataStoreMotionPreferences): MotionPreferences
}
