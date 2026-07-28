package com.ascend.core.domain.training.di

import com.ascend.core.domain.training.BodyweightProgressionConfig
import com.ascend.core.domain.training.CardioProgressionConfig
import com.ascend.core.domain.training.ProgressionRewardConfig
import com.ascend.core.domain.training.ReadinessConfig
import com.ascend.core.domain.training.WeightIncrementConfig
import com.ascend.core.domain.training.ranking.RankingConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Adaptive‑training tuning configs, provided as data so balancing lives in one place. */
@Module
@InstallIn(SingletonComponent::class)
object TrainingModule {
    @Provides @Singleton
    fun readinessConfig() = ReadinessConfig()

    @Provides @Singleton
    fun weightIncrementConfig() = WeightIncrementConfig()

    @Provides @Singleton
    fun progressionRewardConfig() = ProgressionRewardConfig()

    @Provides @Singleton
    fun bodyweightProgressionConfig() = BodyweightProgressionConfig()

    @Provides @Singleton
    fun cardioProgressionConfig() = CardioProgressionConfig()

    @Provides @Singleton
    fun rankingConfig() = RankingConfig()
}
