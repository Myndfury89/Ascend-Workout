package com.ascend.core.domain.skill

import com.ascend.core.model.ActivityTags
import com.ascend.core.model.SkillDefinition
import com.ascend.core.model.SkillEffectDefinition
import com.ascend.core.model.SkillEffectType
import com.ascend.core.model.SkillPrerequisite
import com.ascend.core.model.SkillPrerequisiteType

/**
 * The initial Skills as **configurable seed data** — the single tunable source, kept out of the
 * engines. Every initial Skill is unlocked by **real activity** and carries **no hard class
 * requirement** (`classIds` null on the definition and every prerequisite); class affinity only
 * shapes Skill‑XP rate, effectiveness, and recommendation priority. A user with any class, or no
 * class, may unlock any initial Skill once the real prerequisites are met.
 */
object SkillCatalog {
    const val PERCEPTION = "skill-perception"
    const val STRENGTH_BOOST = "skill-strength-boost"
    const val BREATH_CONTROL = "skill-breath-control"
    const val BODY_AWARENESS = "skill-body-awareness"

    private val perception =
        SkillDefinition(
            id = PERCEPTION,
            name = "Perception",
            description = "Sharpened awareness from sustained aerobic focus — reveals what others miss.",
            category = "Awareness",
            prerequisites =
                listOf(
                    SkillPrerequisite(
                        type = SkillPrerequisiteType.CARDIO_CONTINUOUS_SECONDS,
                        threshold = 15 * 60.0,
                        requiresCompletion = true,
                        disqualifiedBySafetyEvent = true,
                        description = "Complete one continuous cardio session of at least 15 minutes",
                    ),
                ),
            effects =
                listOf(
                    SkillEffectDefinition(SkillEffectType.REVEAL_ENEMY_WEAKNESS, 1.0, description = "Reveal an enemy weakness"),
                    SkillEffectDefinition(SkillEffectType.REVEAL_PARTY_COMPOSITION, 1.0, description = "Suggest a party composition"),
                    SkillEffectDefinition(SkillEffectType.REVEAL_ENCOUNTER_DIFFICULTY, 1.0, description = "Reveal encounter difficulty"),
                    SkillEffectDefinition(SkillEffectType.REVEAL_DUNGEON_INFO, 1.0, 0.1, "Reveal extra Dungeon information"),
                ),
            classAffinityTags = setOf(ActivityTags.STEADY_STATE_CARDIO, ActivityTags.HIGH_INTENSITY_CARDIO),
        )

    private val strengthBoost =
        SkillDefinition(
            id = STRENGTH_BOOST,
            name = "Strength Boost",
            description = "Force forged through verified strength gains.",
            category = "Power",
            prerequisites =
                listOf(
                    SkillPrerequisite(
                        type = SkillPrerequisiteType.STRENGTH_PERSONAL_RECORD,
                        threshold = 1.0,
                        group = 1,
                        description = "Set a verified strength personal record",
                    ),
                    SkillPrerequisite(
                        type = SkillPrerequisiteType.LOAD_PROGRESSION_PROVEN,
                        threshold = 1.0,
                        group = 1,
                        description = "Prove a load-progression recommendation",
                    ),
                ),
            effects =
                listOf(
                    SkillEffectDefinition(SkillEffectType.STRENGTH_CONTRIBUTION_MULTIPLIER, 1.15, 0.03, "Strength contribution multiplier"),
                    SkillEffectDefinition(SkillEffectType.ARMOR_BREAK, 1.0, description = "Armor-break ability"),
                    SkillEffectDefinition(SkillEffectType.HEAVY_ACTION, 1.0, description = "Heavy-action unlock"),
                ),
            classAffinityTags = setOf(ActivityTags.HEAVY_STRENGTH, ActivityTags.HYPERTROPHY),
        )

    private val breathControl =
        SkillDefinition(
            id = BREATH_CONTROL,
            name = "Breath Control",
            description = "Command of the breath through sustained, controlled output.",
            category = "Endurance",
            prerequisites =
                listOf(
                    SkillPrerequisite(
                        type = SkillPrerequisiteType.CARDIO_CONTINUOUS_SECONDS,
                        threshold = 20 * 60.0,
                        requiresCompletion = true,
                        disqualifiedBySafetyEvent = true,
                        description = "Complete a sustained 20-minute cardio, rowing, or swimming session",
                    ),
                ),
            effects =
                listOf(
                    SkillEffectDefinition(SkillEffectType.STAMINA_COST_REDUCTION, 0.15, 0.02, "Reduced simulated stamina cost"),
                    SkillEffectDefinition(SkillEffectType.SUSTAINED_CONTRIBUTION, 1.1, 0.02, "Improved sustained contribution"),
                    SkillEffectDefinition(SkillEffectType.ENDURANCE_ABILITY, 1.0, description = "Endurance-based ability"),
                ),
            classAffinityTags = setOf(ActivityTags.STEADY_STATE_CARDIO, ActivityTags.BREATH_CONTROL, ActivityTags.AQUATIC),
        )

    private val bodyAwareness =
        SkillDefinition(
            id = BODY_AWARENESS,
            name = "Body Awareness",
            description = "Mastery of the body as an instrument — technique, control, and balance.",
            category = "Technique",
            prerequisites =
                listOf(
                    SkillPrerequisite(
                        SkillPrerequisiteType.BODYWEIGHT_VARIATION_ADVANCED,
                        1.0,
                        group = 1,
                        description = "Advance a bodyweight variation",
                    ),
                    SkillPrerequisite(SkillPrerequisiteType.TEMPO_PROGRESSION, 1.0, group = 1, description = "Progress tempo control"),
                    SkillPrerequisite(SkillPrerequisiteType.ASSISTANCE_REDUCED, 1.0, group = 1, description = "Reduce assistance"),
                    SkillPrerequisite(
                        SkillPrerequisiteType.MOBILITY_OR_BALANCE_MILESTONE,
                        1.0,
                        group = 1,
                        description = "Reach a mobility or balance milestone",
                    ),
                ),
            effects =
                listOf(
                    SkillEffectDefinition(SkillEffectType.TECHNIQUE_ACTION, 1.0, description = "Technique action"),
                    SkillEffectDefinition(SkillEffectType.COUNTER_OR_EVASION, 1.0, 0.05, "Counter or evasion action"),
                    SkillEffectDefinition(SkillEffectType.BODYWEIGHT_COMBO, 1.0, description = "Bodyweight-combo unlock"),
                ),
            classAffinityTags = setOf(ActivityTags.BODYWEIGHT, ActivityTags.MOBILITY, ActivityTags.BALANCE),
        )

    val ALL: List<SkillDefinition> = listOf(perception, strengthBoost, breathControl, bodyAwareness)

    fun byId(id: String?): SkillDefinition? = ALL.firstOrNull { it.id == id }
}
