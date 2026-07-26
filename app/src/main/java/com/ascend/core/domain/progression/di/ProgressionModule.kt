package com.ascend.core.domain.progression.di

import com.ascend.core.domain.progression.AttributeProgressCalculator
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.ProgressionEventFactory
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.domain.progression.SetSuggestionEngine
import com.ascend.core.domain.progression.StreakCalculator
import com.ascend.core.domain.progression.XpCalculator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Calculators are pure and stateless; provided as singletons with default config. */
@Module
@InstallIn(SingletonComponent::class)
object ProgressionModule {
    @Provides @Singleton
    fun levelCalculator() = LevelCalculator()

    @Provides @Singleton
    fun rankCalculator() = RankCalculator()

    @Provides @Singleton
    fun xpCalculator() = XpCalculator()

    @Provides @Singleton
    fun attributeProgressCalculator() = AttributeProgressCalculator()

    @Provides @Singleton
    fun streakCalculator() = StreakCalculator()

    @Provides @Singleton
    fun setSuggestionEngine() = SetSuggestionEngine()

    @Provides @Singleton
    fun progressionEventFactory() = ProgressionEventFactory()
}
