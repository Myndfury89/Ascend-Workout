package com.ascend.core.domain.classes

import com.ascend.core.model.ActivityTags
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.ClassPresentation

/**
 * The initial class definitions as **configurable seed data** — the single tunable
 * source of balancing defaults, kept out of the calculators (which take a
 * [ClassDefinition] as input). Promote to a DB table later without touching the
 * engine. Three starter classes; the system is otherwise data‑driven.
 */
object ClassCatalog {
    val BERSERKER =
        ClassDefinition(
            id = "berserker",
            name = "Berserker",
            classTitle = "Breaker of Limits",
            favoredTags =
                setOf(
                    ActivityTags.HEAVY_STRENGTH,
                    ActivityTags.HYPERTROPHY,
                    ActivityTags.EXPLOSIVE,
                    ActivityTags.COMBAT,
                ),
            attributeMultipliers =
                mapOf(
                    AttributeType.STRENGTH to 1.50,
                    AttributeType.ENDURANCE to 0.75,
                    AttributeType.AGILITY to 0.70,
                    AttributeType.DISCIPLINE to 1.00,
                    AttributeType.RECOVERY to 1.10,
                ),
            uniqueProficiencyKey = "FORCE",
            uniqueProficiencyName = "Force",
            favoredClassXpMultiplier = 1.30,
            neutralClassXpMultiplier = 0.75,
            presentation =
                ClassPresentation(
                    statusThemeKey = "berserker",
                    frameVariantKey = "heavy",
                    accentTokenKey = "ember",
                    proficiencyIconKey = "force",
                    idleEffectKey = "forceful_pulse",
                    progressionEffectKey = "impact_slow",
                ),
        )

    val MONK =
        ClassDefinition(
            id = "monk",
            name = "Monk",
            classTitle = "Master of the Vessel",
            favoredTags =
                setOf(
                    ActivityTags.BODYWEIGHT,
                    ActivityTags.MUSCULAR_ENDURANCE,
                    ActivityTags.MOBILITY,
                    ActivityTags.BALANCE,
                ),
            attributeMultipliers =
                mapOf(
                    AttributeType.STRENGTH to 1.25,
                    AttributeType.ENDURANCE to 1.00,
                    AttributeType.AGILITY to 1.20,
                    AttributeType.DISCIPLINE to 1.35,
                    AttributeType.RECOVERY to 1.10,
                ),
            uniqueProficiencyKey = "BODY_MASTERY",
            uniqueProficiencyName = "Body Mastery",
            favoredClassXpMultiplier = 1.30,
            neutralClassXpMultiplier = 0.75,
            presentation =
                ClassPresentation(
                    statusThemeKey = "monk",
                    frameVariantKey = "balanced",
                    accentTokenKey = "aqua",
                    proficiencyIconKey = "body_mastery",
                    idleEffectKey = "circular_calm",
                    progressionEffectKey = "sequential_controlled",
                ),
        )

    val MAGICIAN =
        ClassDefinition(
            id = "magician",
            name = "Magician",
            classTitle = "Channeler of Vitality",
            favoredTags =
                setOf(
                    ActivityTags.STEADY_STATE_CARDIO,
                    ActivityTags.HIGH_INTENSITY_CARDIO,
                    ActivityTags.AQUATIC,
                    ActivityTags.BREATH_CONTROL,
                ),
            attributeMultipliers =
                mapOf(
                    AttributeType.STRENGTH to 0.75,
                    AttributeType.ENDURANCE to 1.50,
                    AttributeType.AGILITY to 1.00,
                    AttributeType.DISCIPLINE to 1.15,
                    AttributeType.RECOVERY to 1.25,
                ),
            uniqueProficiencyKey = "ENERGY_CONTROL",
            uniqueProficiencyName = "Energy Control",
            favoredClassXpMultiplier = 1.30,
            neutralClassXpMultiplier = 0.75,
            presentation =
                ClassPresentation(
                    statusThemeKey = "magician",
                    frameVariantKey = "flowing",
                    accentTokenKey = "recovery",
                    proficiencyIconKey = "energy_control",
                    idleEffectKey = "energy_shimmer",
                    progressionEffectKey = "soft_continuous",
                ),
        )

    val ALL: List<ClassDefinition> = listOf(BERSERKER, MONK, MAGICIAN)

    fun byId(id: String?): ClassDefinition? = ALL.firstOrNull { it.id == id }
}
