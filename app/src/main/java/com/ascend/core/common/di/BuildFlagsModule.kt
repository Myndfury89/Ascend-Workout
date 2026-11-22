package com.ascend.core.common.di

import com.ascend.BuildConfig
import com.ascend.core.common.BuildFlags
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BuildFlagsModule {
    /** Release builds compile [BuildConfig.DEBUG] to false, so the debug awakening override cannot ship. */
    @Provides
    @Singleton
    fun provideBuildFlags(): BuildFlags = BuildFlags(isDebug = BuildConfig.DEBUG)
}
