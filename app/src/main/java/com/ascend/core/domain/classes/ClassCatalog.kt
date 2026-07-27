package com.ascend.core.domain.classes

import com.ascend.core.model.ActivityTags
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.ClassPresentation

/**
 * The initial class definitions as **configurable seed data** — the single tunable
 * source of balancing defaults, kept out of the calculators. Seeded into the
 * `class_definition` table on first run; the engine reads definitions from the
 * repository and never branches on the class id. Three starter classes; further
 * classes (Assassin, Fighter, Tidewalker, Ranger, Guardian) can be added here (or
 * as DB rows) without touching calculation logic.
 */
object ClassCatalog {
    val BERSERKER =
        ClassDefinition(
            id = "berserker",
            name = "Berserker",
            classTitle = "Breaker of Limits",
            description = "Raw power expressed through heavy load and overload. Trains to move the immovable.",
            fitnessIdentity = "Powerful and muscular",
            favoredWorkoutCategories = listOf("Heavy resistance", "Compound strength", "Hypertrophy", "Loaded carries"),
            favoredTags = setOf(ActivityTags.HEAVY_STRENGTH, ActivityTags.HYPERTROPHY, ActivityTags.EXPLOSIVE),
            primaryAttributes = listOf(AttributeType.STRENGTH),
            secondaryAttributes = listOf(AttributeType.RECOVERY),
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
            description = "Mastery of the body as the instrument — control, balance, and bodyweight command.",
            fitnessIdentity = "Skilled with bodyweight movement",
            favoredWorkoutCategories = listOf("Calisthenics", "Bodyweight strength", "Mobility", "Balance", "Core control"),
            favoredTags = setOf(ActivityTags.BODYWEIGHT, ActivityTags.MUSCULAR_ENDURANCE, ActivityTags.MOBILITY, ActivityTags.BALANCE),
            primaryAttributes = listOf(AttributeType.DISCIPLINE, AttributeType.AGILITY),
            secondaryAttributes = listOf(AttributeType.STRENGTH),
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
            description = "Endurance and energy systems — sustained output, breath, and recovery.",
            fitnessIdentity = "High endurance and cardio efficiency",
            favoredWorkoutCategories =
                listOf("Running", "Cycling", "Rowing", "Swimming", "Zone training", "Breathwork", "Sustained cardio"),
            favoredTags =
                setOf(
                    ActivityTags.STEADY_STATE_CARDIO,
                    ActivityTags.HIGH_INTENSITY_CARDIO,
                    ActivityTags.BREATH_CONTROL,
                    ActivityTags.RECOVERY,
                    ActivityTags.AQUATIC,
                ),
            primaryAttributes = listOf(AttributeType.ENDURANCE),
            secondaryAttributes = listOf(AttributeType.RECOVERY, AttributeType.DISCIPLINE),
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
