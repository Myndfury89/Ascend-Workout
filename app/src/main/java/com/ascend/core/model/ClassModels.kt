package com.ascend.core.model

/**
 * Data‑driven activity tags. Affinity is computed from tag overlap, never from
 * class‑specific conditionals, so new classes/activities need no engine changes.
 * Tags are plain strings end‑to‑end (stored, compared) — this list is the initial
 * vocabulary, not a closed enum.
 */
object ActivityTags {
    const val HEAVY_STRENGTH = "HEAVY_STRENGTH"
    const val HYPERTROPHY = "HYPERTROPHY"
    const val BODYWEIGHT = "BODYWEIGHT"
    const val MUSCULAR_ENDURANCE = "MUSCULAR_ENDURANCE"
    const val EXPLOSIVE = "EXPLOSIVE"
    const val STEADY_STATE_CARDIO = "STEADY_STATE_CARDIO"
    const val HIGH_INTENSITY_CARDIO = "HIGH_INTENSITY_CARDIO"
    const val MOBILITY = "MOBILITY"
    const val BALANCE = "BALANCE"
    const val BREATH_CONTROL = "BREATH_CONTROL"
    const val RECOVERY = "RECOVERY"
    const val COMBAT = "COMBAT"
    const val AQUATIC = "AQUATIC"

    val INITIAL: Set<String> =
        setOf(
            HEAVY_STRENGTH, HYPERTROPHY, BODYWEIGHT, MUSCULAR_ENDURANCE, EXPLOSIVE,
            STEADY_STATE_CARDIO, HIGH_INTENSITY_CARDIO, MOBILITY, BALANCE, BREATH_CONTROL,
            RECOVERY, COMBAT, AQUATIC,
        )
}

/**
 * Presentation metadata for a class — variations of Ascend's own design system, not
 * separate skins. Consumed later by the animated Status menu; carried here so the
 * class definition is the single source for a class's identity.
 */
data class ClassPresentation(
    val statusThemeKey: String,
    val frameVariantKey: String,
    val accentTokenKey: String,
    val proficiencyIconKey: String,
    val idleEffectKey: String,
    val progressionEffectKey: String,
)

/**
 * A playable class. All numbers are **balancing defaults** carried as data (see
 * [com.ascend.core.domain.classes.ClassCatalog]) — the engine reads a definition, it
 * never branches on [id]. [attributeMultipliers] scale an activity's *existing* base
 * attribute distribution; absent attributes default to 1.0 (neutral).
 */
data class ClassDefinition(
    val id: String,
    val name: String,
    val classTitle: String,
    val favoredTags: Set<String>,
    val attributeMultipliers: Map<AttributeType, Double>,
    val uniqueProficiencyKey: String,
    val uniqueProficiencyName: String,
    val favoredClassXpMultiplier: Double,
    val neutralClassXpMultiplier: Double,
    val presentation: ClassPresentation,
) {
    fun attributeMultiplier(attribute: AttributeType): Double = attributeMultipliers[attribute] ?: 1.0
}

/** Which class slot an earning is attributed to. */
enum class ClassSlot(val allocationKey: String) {
    PRIMARY("primary"),
    SECONDARY("secondary"),
}

/** A player's chosen classes (primary required once chosen; secondary optional). */
data class PlayerClassSelection(
    val userId: String,
    val primaryClassId: String?,
    val secondaryClassId: String?,
) {
    val hasClass: Boolean get() = primaryClassId != null
}

/** One class's contribution to a completion reward. */
data class ClassRewardLine(
    val classId: String,
    val className: String,
    val slot: ClassSlot,
    val allocation: Double,
    val affinity: Double,
    val classXp: Long,
    val newClassLevel: Int,
    val leveledUp: Boolean,
    val uniqueProficiencyKey: String,
    val uniqueProficiencyName: String,
    val uniqueProficiencyGain: Long,
)

/**
 * The fully inspectable reward for a completion. Player XP is always class‑neutral;
 * everything else is class‑shaped. Surfaced to the reward screen so a player can see
 * exactly why each gain happened.
 */
data class RewardBreakdown(
    val basePlayerXp: Long,
    val playerLeveledUp: Boolean,
    val newPlayerLevel: Int,
    val baseAttributeDistribution: Map<AttributeType, Long>,
    val awardedAttributeProficiency: Map<AttributeType, Long>,
    val primaryClass: ClassRewardLine?,
    val secondaryClass: ClassRewardLine?,
) {
    val hasClassContribution: Boolean get() = primaryClass != null
}
