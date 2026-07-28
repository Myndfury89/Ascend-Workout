package com.ascend.feature.dashboard.prototype

/*
 * Fake, in-memory state for the REVISED Status motion prototype. Nothing here touches the
 * ProgressionEventQueue, repositories, or real reward logic — every value is simulated so
 * the composition and motion can be reviewed in isolation before any production wiring.
 */

/** The four sigil identities the prototype can render. */
enum class StatusClassVariant(
    val displayName: String,
    val classTitle: String,
    val proficiencyName: String,
) {
    NEUTRAL("Unbound", "The Unawakened", "Potential"),
    BERSERKER("Berserker", "Breaker of Limits", "Force"),
    MONK("Monk", "Master of the Vessel", "Body Mastery"),
    MAGICIAN("Magician", "Channeler of Vitality", "Energy Control"),
}

/** The reviewable prototype states from the spec's debug selector. */
enum class StatusPrototypeStateId(val label: String) {
    STANDARD("Standard status"),
    QUEST_PROGRESS("Quest progress update"),
    QUEST_COMPLETE("Quest completion"),
    PLAYER_XP("Player XP gain"),
    CLASS_XP("Class XP gain"),
    ATTRIBUTE_UP("Attribute increase"),
    PROFICIENCY_UP("Unique proficiency increase"),
    PLAYER_LEVEL_UP("Player level-up"),
    CLASS_LEVEL_UP("Class level-up"),
    PERSONAL_RECORD("Personal record"),
    RECOMMENDATION("Recommendation available"),
    PROGRESSION_COMPLETE("Progression completed"),
    RANK_PROMOTION("Rank promotion"),
    LOW_RANK("Low-rank seal"),
    MID_RANK("Mid-rank seal"),
    HIGH_RANK("High-rank seal"),
    REDUCED_MOTION("Reduced-motion mode"),
}

/** The transient overlay a state can raise over the panel (drives the event motion). */
enum class StatusOverlayKind {
    NONE,
    QUEST_PROGRESS,
    QUEST_COMPLETE,
    PLAYER_XP,
    CLASS_XP,
    ATTRIBUTE,
    PROFICIENCY,
    PLAYER_LEVEL_UP,
    CLASS_LEVEL_UP,
    PERSONAL_RECORD,
    RECOMMENDATION,
    PROGRESSION_COMPLETE,
    RANK_PROMOTION,
}

data class AttributeLine(
    val name: String,
    val value: Int,
    val emphasized: Boolean = false,
)

/** A temporary event overlay descriptor (label + optional detail + which attribute pulses). */
data class StatusEventOverlay(
    val kind: StatusOverlayKind,
    val title: String,
    val detail: String,
    val emphasizedAttribute: String? = null,
)

/**
 * A full, self-contained snapshot the prototype renders — identity, progression, attributes,
 * the adaptive-training summary, and the daily quest — plus the sigil state and any event
 * overlay. Assembled by [FakeStatusPrototype]; never derived from real data.
 */
data class StatusPrototypeData(
    val stateId: StatusPrototypeStateId,
    val variant: StatusClassVariant,
    // Identity
    val hunterName: String,
    val level: Int,
    val rankLabel: String,
    val trainingTier: Int,
    val title: String,
    val secondaryVariant: StatusClassVariant?,
    // Progression
    val playerXpInLevel: Int,
    val playerXpForLevel: Int,
    val classLevel: Int,
    val classXpInLevel: Int,
    val classXpForLevel: Int,
    val secondaryClassXpInLevel: Int?,
    val secondaryClassXpForLevel: Int?,
    val uniqueProficiency: Int,
    // Attributes
    val attributes: List<AttributeLine>,
    // Adaptive training
    val trainingFocus: String,
    val pendingRecommendation: String?,
    val recentProgressionEvent: String,
    val readinessState: String,
    val recentPersonalRecord: String?,
    // Daily quest
    val questName: String,
    val questProgress: Int,
    val questTarget: Int,
    val questIntervalState: String?,
    // Motion
    val overlay: StatusEventOverlay,
    val reducedMotion: Boolean,
    val majorUnlock: Boolean,
    // Ornate sigil
    val rankTier: RankTier,
    val activeMedallionIndex: Int,
    val showProficiencyMedallion: Boolean,
) {
    val questRemaining: Int get() = (questTarget - questProgress).coerceAtLeast(0)
    val playerXpFraction: Float get() = fraction(playerXpInLevel, playerXpForLevel)
    val classXpFraction: Float get() = fraction(classXpInLevel, classXpForLevel)
    val secondaryClassXpFraction: Float?
        get() =
            if (secondaryClassXpInLevel != null && secondaryClassXpForLevel != null) {
                fraction(secondaryClassXpInLevel, secondaryClassXpForLevel)
            } else {
                null
            }
    val questFraction: Float get() = fraction(questProgress, questTarget)

    private fun fraction(
        part: Int,
        whole: Int,
    ): Float = if (whole <= 0) 0f else (part.toFloat() / whole).coerceIn(0f, 1f)
}
