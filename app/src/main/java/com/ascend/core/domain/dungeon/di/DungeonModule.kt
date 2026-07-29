package com.ascend.core.domain.dungeon.di

import com.ascend.core.domain.dungeon.DungeonConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Dungeon balancing config, provided as data so tuning lives in one place. */
@Module
@InstallIn(SingletonComponent::class)
object DungeonModule {
    @Provides @Singleton
    fun dungeonConfig() = DungeonConfig()
}
