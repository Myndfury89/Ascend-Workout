package com.ascend.core.domain.classes

/**
 * Tunable knobs for how a class reshapes a reward — balancing defaults only, never
 * fixed constants. Shared by the class calculators.
 */
data class ClassRewardConfig(
    // Class XP base as a fraction of the (class‑neutral) Player XP for the activity.
    val classXpBaseFraction: Double = 0.6,
    // Class XP / unique‑proficiency rate for the secondary class.
    val secondaryAllocation: Double = 0.5,
    // How strongly favored activities feed the class's unique proficiency.
    val uniqueProficiencyRate: Double = 1.5,
)
