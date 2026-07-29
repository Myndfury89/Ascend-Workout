package com.ascend.core.domain.skill.di

import com.ascend.core.domain.skill.SkillConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Skills tuning config, provided as data so balancing lives in one place. */
@Module
@InstallIn(SingletonComponent::class)
object SkillModule {
    @Provides @Singleton
    fun skillConfig() = SkillConfig()
}
