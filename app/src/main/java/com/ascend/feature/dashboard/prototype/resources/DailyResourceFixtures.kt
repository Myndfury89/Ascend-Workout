package com.ascend.feature.dashboard.prototype.resources

/*
 * Deterministic fake inputs for every device-availability scenario. These feed the REAL resolvers
 * (HP / MP-evidence / training-target), so the HUD demonstrates actual resolution — dedup, bands,
 * rest-day, and unavailable-vs-zero — without any repository or Health Connect dependency.
 */

enum class DailyResourceScenario(val label: String) {
    FULL("Full"),
    PARTIAL("Partial"),
    PHONE_ONLY("Phone-only"),
    SPARSE("Sparse"),
    REST_DAY("Rest day"),
    NO_ACTIVITY("No activity"),
}

data class ScenarioInputs(
    val movement: MovementInput,
    val evidence: List<TrainingDurationEvidence>,
    val targetInput: TrainingTargetInput,
    val xp: XpState,
)

object DailyResourceFixtures {
    private const val MIN = 60_000L // one minute in ms

    private val XP = XpState(level = 27, currentLevelXp = 4_148, xpForNextLevel = 6_200)

    private fun evidence(
        source: TrainingDurationSource,
        minutes: Int,
        startMin: Long,
        stableSessionId: String? = null,
        externalRecordId: String? = null,
        sourceApplication: String? = null,
        classification: WorkoutKind = WorkoutKind.UNKNOWN,
    ) = TrainingDurationEvidence(
        source = source,
        durationMinutes = minutes,
        startTime = startMin * MIN,
        endTime = (startMin + minutes) * MIN,
        stableSessionId = stableSessionId,
        externalRecordId = externalRecordId,
        sourceApplication = sourceApplication,
        classification = classification,
    )

    fun inputs(scenario: DailyResourceScenario): ScenarioInputs =
        when (scenario) {
            DailyResourceScenario.FULL ->
                ScenarioInputs(
                    movement = MovementInput(6_842, MovementSource.STEP_RECORDS, 8_000),
                    // Same 45-min session reported by both Ascend and Health Connect → counted once.
                    evidence =
                        listOf(
                            evidence(
                                TrainingDurationSource.ASCEND_WORKOUT,
                                45,
                                540,
                                stableSessionId = "sess-1",
                                classification = WorkoutKind.STRENGTH,
                            ),
                            evidence(
                                TrainingDurationSource.HEALTH_CONNECT,
                                45,
                                541,
                                stableSessionId = "sess-1",
                                classification = WorkoutKind.STRENGTH,
                            ),
                        ),
                    targetInput = TrainingTargetInput(ReadinessLevel.READY, 45, prescribedRest = false),
                    xp = XP,
                )
            DailyResourceScenario.PARTIAL ->
                ScenarioInputs(
                    movement = MovementInput(6_800, MovementSource.STEP_RECORDS, 8_000),
                    evidence = listOf(evidence(TrainingDurationSource.ASCEND_WORKOUT, 38, 540, classification = WorkoutKind.STRENGTH)),
                    targetInput = TrainingTargetInput(ReadinessLevel.READY, 45, prescribedRest = false),
                    xp = XP,
                )
            DailyResourceScenario.PHONE_ONLY ->
                ScenarioInputs(
                    movement = MovementInput(5_000, MovementSource.STEP_RECORDS, 8_000),
                    // Distinct sessions from phone-only sources — both count.
                    evidence =
                        listOf(
                            evidence(
                                TrainingDurationSource.ASCEND_CARDIO,
                                30,
                                420,
                                stableSessionId = "cardio-1",
                                classification = WorkoutKind.CARDIO,
                            ),
                            evidence(
                                TrainingDurationSource.QUEST_EVIDENCE,
                                15,
                                1_000,
                                stableSessionId = "quest-1",
                                classification = WorkoutKind.MIXED,
                            ),
                        ),
                    targetInput = TrainingTargetInput(ReadinessLevel.READY, 45, prescribedRest = false),
                    xp = XP,
                )
            DailyResourceScenario.SPARSE ->
                ScenarioInputs(
                    // No movement source at all → HP unavailable; a real workout → MP still valid.
                    movement = MovementInput(null, MovementSource.NONE, 8_000),
                    evidence = listOf(evidence(TrainingDurationSource.ASCEND_WORKOUT, 45, 540, classification = WorkoutKind.STRENGTH)),
                    targetInput = TrainingTargetInput(ReadinessLevel.READY, 45, prescribedRest = false),
                    xp = XP,
                )
            DailyResourceScenario.REST_DAY ->
                ScenarioInputs(
                    movement = MovementInput(4_000, MovementSource.STEP_RECORDS, 8_000),
                    evidence = emptyList(),
                    targetInput = TrainingTargetInput(ReadinessLevel.RECOVERY, 45, prescribedRest = true),
                    xp = XP,
                )
            DailyResourceScenario.NO_ACTIVITY ->
                ScenarioInputs(
                    // Sources present, but today's totals are genuinely zero → 0%, not "unavailable".
                    movement = MovementInput(0, MovementSource.STEP_RECORDS, 8_000),
                    evidence = emptyList(),
                    targetInput = TrainingTargetInput(ReadinessLevel.READY, 45, prescribedRest = false),
                    xp = XP,
                )
        }
}
