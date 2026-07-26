package com.ascend.core.data.mapper

import com.ascend.core.database.entity.PlayerProgressEntity
import com.ascend.core.database.entity.PlayerStatsEntity
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import com.ascend.core.model.Rank

fun PlayerProgressEntity.toDomain(levelCalculator: LevelCalculator): PlayerProgress =
    PlayerProgress(
        userId = userId,
        level = level,
        currentLevelXp = currentLevelXp,
        xpToNextLevel = levelCalculator.xpToReachNextLevel(level),
        lifetimeXp = lifetimeXp,
        rank = runCatching { Rank.valueOf(rank) }.getOrDefault(Rank.INITIATE),
        activeStreak = activeStreak,
        longestStreak = longestStreak,
        totalWorkouts = totalWorkouts,
        totalQuests = totalQuests,
        totalExpeditions = totalExpeditions,
    )

fun PlayerStatsEntity.toDomain(): PlayerStats =
    PlayerStats(
        userId = userId,
        strength = strength,
        endurance = endurance,
        agility = agility,
        discipline = discipline,
        recovery = recovery,
    )
