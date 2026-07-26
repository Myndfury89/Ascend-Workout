package com.ascend.core.model

/** Domain view of a player's RPG progression (mapped from persistence). */
data class PlayerProgress(
    val userId: String,
    val level: Int,
    val currentLevelXp: Long,
    val xpToNextLevel: Long,
    val lifetimeXp: Long,
    val rank: Rank,
    val activeStreak: Int,
    val longestStreak: Int,
    val totalWorkouts: Int,
    val totalQuests: Int,
    val totalExpeditions: Int,
) {
    val progressFraction: Float
        get() = if (xpToNextLevel <= 0L) 1f else currentLevelXp.toFloat() / xpToNextLevel.toFloat()
}

/** Domain view of the five core attributes. */
data class PlayerStats(
    val userId: String,
    val strength: Long,
    val endurance: Long,
    val agility: Long,
    val discipline: Long,
    val recovery: Long,
) {
    fun value(attribute: AttributeType): Long =
        when (attribute) {
            AttributeType.STRENGTH -> strength
            AttributeType.ENDURANCE -> endurance
            AttributeType.AGILITY -> agility
            AttributeType.DISCIPLINE -> discipline
            AttributeType.RECOVERY -> recovery
        }

    val asMap: Map<AttributeType, Long>
        get() = AttributeType.entries.associateWith(::value)
}
