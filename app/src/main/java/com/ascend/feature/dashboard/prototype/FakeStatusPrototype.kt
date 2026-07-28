package com.ascend.feature.dashboard.prototype

/**
 * Builds the fake [StatusPrototypeData] for any (state, class, reduced-motion) combination.
 * This is the *only* source of truth for the prototype — deliberately hand-authored so the
 * revised composition and its motion can be reviewed without any real progression data.
 */
object FakeStatusPrototype {
    private fun baseAttributes(variant: StatusClassVariant): List<AttributeLine> =
        when (variant) {
            StatusClassVariant.BERSERKER ->
                listOf(a("Strength", 96), a("Endurance", 54), a("Agility", 48), a("Discipline", 61), a("Recovery", 58))
            StatusClassVariant.MONK ->
                listOf(a("Strength", 71), a("Endurance", 63), a("Agility", 82), a("Discipline", 94), a("Recovery", 66))
            StatusClassVariant.MAGICIAN ->
                listOf(a("Strength", 49), a("Endurance", 97), a("Agility", 60), a("Discipline", 74), a("Recovery", 88))
            StatusClassVariant.NEUTRAL ->
                listOf(a("Strength", 42), a("Endurance", 40), a("Agility", 38), a("Discipline", 45), a("Recovery", 41))
        }

    private fun a(
        name: String,
        value: Int,
    ) = AttributeLine(name, value)

    /** The baseline snapshot before any event; other states are edits of this. */
    private fun baseline(
        variant: StatusClassVariant,
        reducedMotion: Boolean,
        stateId: StatusPrototypeStateId,
        overlay: StatusEventOverlay = StatusEventOverlay(StatusOverlayKind.NONE, "", ""),
    ): StatusPrototypeData =
        StatusPrototypeData(
            stateId = stateId,
            variant = variant,
            hunterName = "Kaiden Voss",
            level = 27,
            rankLabel = "Ascendant III",
            trainingTier = 4,
            title = variant.classTitle,
            secondaryVariant = if (variant == StatusClassVariant.MAGICIAN) StatusClassVariant.MONK else null,
            playerXpInLevel = 4148,
            playerXpForLevel = 6200,
            classLevel = 12,
            classXpInLevel = 720,
            classXpForLevel = 1500,
            secondaryClassXpInLevel = if (variant == StatusClassVariant.MAGICIAN) 340 else null,
            secondaryClassXpForLevel = if (variant == StatusClassVariant.MAGICIAN) 1200 else null,
            uniqueProficiency = 58,
            attributes = baseAttributes(variant),
            trainingFocus = focusFor(variant),
            pendingRecommendation = null,
            recentProgressionEvent = "Bench press → 62.5 kg confirmed",
            readinessState = "Ready for load progression",
            recentPersonalRecord = null,
            questName = "Hundredfold Discipline",
            questProgress = 140,
            questTarget = 200,
            questIntervalState = "Interval 3 of 4 · 40 to go",
            overlay = overlay,
            reducedMotion = reducedMotion,
            majorUnlock = false,
        )

    private fun focusFor(variant: StatusClassVariant): String =
        when (variant) {
            StatusClassVariant.BERSERKER -> "Heavy pressing — double progression"
            StatusClassVariant.MONK -> "Push-up variation ladder"
            StatusClassVariant.MAGICIAN -> "Zone-2 duration builds"
            StatusClassVariant.NEUTRAL -> "Foundational conditioning"
        }

    fun dataFor(
        stateId: StatusPrototypeStateId,
        variant: StatusClassVariant,
        reducedMotion: Boolean,
    ): StatusPrototypeData {
        val effectiveReduced = reducedMotion || stateId == StatusPrototypeStateId.REDUCED_MOTION
        val base = baseline(variant, effectiveReduced, stateId)
        return when (stateId) {
            StatusPrototypeStateId.STANDARD, StatusPrototypeStateId.REDUCED_MOTION -> base
            StatusPrototypeStateId.QUEST_PROGRESS ->
                base.copy(
                    questProgress = 165,
                    questIntervalState = "Interval 4 of 4 · 35 to go",
                    overlay = StatusEventOverlay(StatusOverlayKind.QUEST_PROGRESS, "+25 reps", "Hundredfold Discipline"),
                )
            StatusPrototypeStateId.QUEST_COMPLETE ->
                base.copy(
                    questProgress = 200,
                    questIntervalState = "Completed · all intervals cleared",
                    overlay = StatusEventOverlay(StatusOverlayKind.QUEST_COMPLETE, "Quest complete", "Hundredfold Discipline"),
                )
            StatusPrototypeStateId.PLAYER_XP ->
                base.copy(
                    playerXpInLevel = 4748,
                    overlay = StatusEventOverlay(StatusOverlayKind.PLAYER_XP, "+600 XP", "Session banked"),
                )
            StatusPrototypeStateId.CLASS_XP ->
                base.copy(
                    classXpInLevel = 1040,
                    overlay = StatusEventOverlay(StatusOverlayKind.CLASS_XP, "+320 ${variant.displayName} XP", "Affinity matched"),
                )
            StatusPrototypeStateId.ATTRIBUTE_UP ->
                base.copy(
                    attributes = base.attributes.mapIndexed { i, l -> if (i == 0) l.copy(value = l.value + 3, emphasized = true) else l },
                    overlay =
                        StatusEventOverlay(
                            StatusOverlayKind.ATTRIBUTE,
                            "+3 ${base.attributes[0].name}",
                            "Volume threshold",
                            base.attributes[0].name,
                        ),
                )
            StatusPrototypeStateId.PROFICIENCY_UP ->
                base.copy(
                    uniqueProficiency = base.uniqueProficiency + 5,
                    overlay = StatusEventOverlay(StatusOverlayKind.PROFICIENCY, "+5 ${variant.proficiencyName}", "Class-favoured work"),
                )
            StatusPrototypeStateId.PLAYER_LEVEL_UP ->
                base.copy(
                    level = 28,
                    playerXpInLevel = 260,
                    overlay = StatusEventOverlay(StatusOverlayKind.PLAYER_LEVEL_UP, "Level 28", "Threshold crossed"),
                )
            StatusPrototypeStateId.CLASS_LEVEL_UP ->
                base.copy(
                    classLevel = 13,
                    classXpInLevel = 120,
                    overlay =
                        StatusEventOverlay(
                            StatusOverlayKind.CLASS_LEVEL_UP,
                            "${variant.displayName} Lv 13",
                            "Specialisation deepens",
                        ),
                )
            StatusPrototypeStateId.PERSONAL_RECORD ->
                base.copy(
                    recentPersonalRecord = "Bench press 65 kg × 5",
                    overlay = StatusEventOverlay(StatusOverlayKind.PERSONAL_RECORD, "Personal record", "Bench press 65 kg × 5"),
                )
            StatusPrototypeStateId.RECOMMENDATION ->
                base.copy(
                    pendingRecommendation = "Increase load to 65 kg (double progression met)",
                    overlay = StatusEventOverlay(StatusOverlayKind.RECOMMENDATION, "New recommendation", "Increase load to 65 kg"),
                )
            StatusPrototypeStateId.PROGRESSION_COMPLETE ->
                base.copy(
                    recentProgressionEvent = "Load increase proven → reward granted",
                    overlay = StatusEventOverlay(StatusOverlayKind.PROGRESSION_COMPLETE, "Progression completed", "Load increase proven"),
                )
            StatusPrototypeStateId.RANK_PROMOTION ->
                base.copy(
                    rankLabel = "Paragon I",
                    majorUnlock = true,
                    overlay = StatusEventOverlay(StatusOverlayKind.RANK_PROMOTION, "Rank promotion", "Paragon I"),
                )
        }
    }
}
