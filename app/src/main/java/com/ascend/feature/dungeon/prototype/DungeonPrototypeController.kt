package com.ascend.feature.dungeon.prototype

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ascend.core.domain.dungeon.ContributionEligibilityEvaluator
import com.ascend.core.domain.dungeon.DungeonCatalog
import com.ascend.core.domain.dungeon.DungeonCombatEngine
import com.ascend.core.domain.dungeon.DungeonRewardCalculator
import com.ascend.core.domain.dungeon.PartyScalingCalculator
import com.ascend.core.domain.dungeon.PartyScalingInput
import com.ascend.core.domain.dungeon.WorkoutActionInput
import com.ascend.core.domain.dungeon.WorkoutContributionMapper
import com.ascend.core.model.DungeonCombatState
import com.ascend.core.model.DungeonEncounterState
import com.ascend.core.model.DungeonParty
import com.ascend.core.model.DungeonPartyMember
import com.ascend.core.model.DungeonReward
import com.ascend.core.model.MemberActivityState
import com.ascend.core.model.PartyScaling
import com.ascend.core.model.WorkoutActionType

/** A simulated party member the reviewer configures (never a real person). */
data class PartyMemberDraft(
    val id: String,
    val handle: String,
    val classId: String?,
    val skillIds: Set<String>,
    val activityState: MemberActivityState,
    val workoutActive: Boolean,
    val ready: Boolean,
    val earnedContribution: Long,
    val lastActivityAt: Long,
)

/** One line in the contribution timeline. */
data class TimelineEntry(
    val handle: String,
    val text: String,
    val isContribution: Boolean,
)

enum class DungeonPrototypePhase { LOBBY, READY_CHECK, IN_ENCOUNTER, VICTORY, DEFEAT }

/**
 * Drives the fake party Dungeon prototype: a simulated lobby (fake nearby presences), an explicit
 * ready check + join, and a deterministic encounter powered by the real Dungeon engines fed by
 * simulated workout-event triggers. No Bluetooth, no real users, no networking. All debug-only.
 */
@Stable
class DungeonPrototypeController(
    val provider: NearbyAscendedProvider,
) {
    private val mapper = WorkoutContributionMapper()
    private val combatEngine = DungeonCombatEngine()
    private val eligibility = ContributionEligibilityEvaluator()
    private val scalingCalc = PartyScalingCalculator()
    private val rewardCalc = DungeonRewardCalculator()

    var phase by mutableStateOf(DungeonPrototypePhase.LOBBY)
        private set
    var selectedDungeonId by mutableStateOf(DungeonCatalog.DUNGEONS.first().id)
    var solo by mutableStateOf(false)
    var weightUnit by mutableStateOf(com.ascend.core.common.WeightUnit.KILOGRAMS)

    /** A formatted reference load for the strength contribution, in the selected unit. */
    val strengthReferenceLabel: String
        get() = "Strength set ≈ ${com.ascend.core.common.WeightUnits.format(STRENGTH_REFERENCE_KG, weightUnit)}"
    var nowMillis by mutableLongStateOf(0L)
        private set

    val party = mutableStateListOf<PartyMemberDraft>()
    val timeline = mutableStateListOf<TimelineEntry>()

    var combat by mutableStateOf<DungeonCombatState?>(null)
        private set
    var scaling by mutableStateOf<PartyScaling?>(null)
        private set
    var reward by mutableStateOf<DungeonReward?>(null)
        private set

    private var encounterStart by mutableLongStateOf(0L)
    private var memberSeq by mutableIntStateOf(0)
    private var rewardAwarded = false

    private val dungeon get() = DungeonCatalog.dungeonById(selectedDungeonId) ?: DungeonCatalog.DUNGEONS.first()

    // ---- lobby / party management ----

    fun join(presence: FakeNearbyPresence) {
        if (party.size >= MAX_PARTY || party.any { it.handle == presence.handle }) return
        party.add(
            PartyMemberDraft(
                id = "member-${memberSeq++}", handle = presence.handle, classId = presence.classId,
                skillIds = emptySet(), activityState = MemberActivityState.ACTIVE, workoutActive = true,
                ready = false, earnedContribution = 0, lastActivityAt = nowMillis,
            ),
        )
    }

    fun leave(memberId: String) {
        party.removeAll { it.id == memberId }
    }

    fun toggleReady(memberId: String) = update(memberId) { it.copy(ready = !it.ready) }

    fun setActivityState(
        memberId: String,
        state: MemberActivityState,
    ) = update(memberId) { it.copy(activityState = state, workoutActive = state == MemberActivityState.ACTIVE) }

    fun setSkills(
        memberId: String,
        skills: Set<String>,
    ) = update(memberId) { it.copy(skillIds = skills) }

    fun safetyStop(memberId: String) = setActivityState(memberId, MemberActivityState.SAFETY_PAUSED)

    val allReady: Boolean get() = party.isNotEmpty() && party.all { it.ready }

    fun beginReadyCheck() {
        if (party.isNotEmpty()) phase = DungeonPrototypePhase.READY_CHECK
    }

    // ---- encounter ----

    fun startEncounter() {
        val members = if (solo) party.take(1) else party
        if (members.isEmpty()) return
        val enemy = dungeon.enemies.first()
        val scaled =
            scalingCalc.scale(
                PartyScalingInput(
                    baseEnemyHealth = enemy.baseHealth, encounterTier = dungeon.tier,
                    memberCount = members.size, activeMemberCount = members.count { it.activityState.canContribute },
                    averageTrainingTier = 3, distinctClasses = members.mapNotNull { it.classId }.distinct().size.coerceAtLeast(1),
                    distinctSkills = members.flatMap { it.skillIds }.distinct().size, recentActivityFraction = 1.0, solo = solo,
                ),
            )
        scaling = scaled
        combat = combatEngine.initialState(scaled.scaledEnemyHealth)
        timeline.clear()
        reward = null
        rewardAwarded = false
        encounterStart = nowMillis
        phase = DungeonPrototypePhase.IN_ENCOUNTER
    }

    fun trigger(
        memberId: String,
        action: WorkoutActionType,
    ) {
        val state = combat ?: return
        if (phase != DungeonPrototypePhase.IN_ENCOUNTER) return
        val draft = party.firstOrNull { it.id == memberId } ?: return
        val elig = eligibility.evaluate(draft.toMember(), workoutActive = draft.workoutActive, now = nowMillis)
        if (!elig.canContribute) {
            timeline.add(0, TimelineEntry(draft.handle, elig.message, isContribution = false))
            return
        }
        val contribution =
            mapper.map(
                WorkoutActionInput(draft.id, action, metricFor(action), "SIM", "sim-$nowMillis-${action.name}", draft.skillIds),
            )
        val enemy = dungeon.enemies.first()
        val next = combatEngine.apply(state, enemy, contribution)
        combat = next
        update(memberId) { it.copy(earnedContribution = it.earnedContribution + contribution.magnitude, lastActivityAt = nowMillis) }
        val dealt = state.enemyHealth - next.enemyHealth
        timeline.add(0, TimelineEntry(draft.handle, "${action.pretty()} → ${contribution.type} (-$dealt)", isContribution = true))
        if (next.state == DungeonEncounterState.VICTORY) finishVictory()
    }

    fun advanceTimer(seconds: Long) {
        nowMillis += seconds * 1000
        if (phase == DungeonPrototypePhase.IN_ENCOUNTER) {
            val budgetMs = (scaling?.recommendedDurationSeconds ?: DEFAULT_BUDGET_SECONDS) * 1000 * FAIL_BUDGET_FACTOR
            if (nowMillis - encounterStart > budgetMs && combat?.defeated != true) {
                combat = combat?.copy(state = DungeonEncounterState.DEFEAT)
                phase = DungeonPrototypePhase.DEFEAT
            }
        }
    }

    fun reset() {
        phase = DungeonPrototypePhase.LOBBY
        combat = null
        scaling = null
        reward = null
        rewardAwarded = false
        timeline.clear()
    }

    private fun finishVictory() {
        val partyModel = DungeonParty(party.map { it.toMember() }, solo)
        reward = rewardCalc.award("enc-$encounterStart", dungeon, partyModel, scaling!!, alreadyAwarded = rewardAwarded)
        rewardAwarded = true
        phase = DungeonPrototypePhase.VICTORY
    }

    private inline fun update(
        memberId: String,
        transform: (PartyMemberDraft) -> PartyMemberDraft,
    ) {
        val i = party.indexOfFirst { it.id == memberId }
        if (i >= 0) party[i] = transform(party[i])
    }

    private fun PartyMemberDraft.toMember(): DungeonPartyMember =
        DungeonPartyMember(id, handle, trainingTier = 3, classId, skillIds, activityState, earnedContribution, lastActivityAt)

    private fun metricFor(action: WorkoutActionType): Long =
        when (action) {
            WorkoutActionType.STRENGTH_SET -> STRENGTH_METRIC
            WorkoutActionType.CARDIO_DURATION -> CARDIO_METRIC
            WorkoutActionType.PERSONAL_RECORD -> PR_METRIC
            WorkoutActionType.QUEST_INTERVAL -> QUEST_METRIC
            WorkoutActionType.MOBILITY_OR_AGILITY -> MOBILITY_METRIC
            WorkoutActionType.RECOVERY -> 0
        }

    private fun WorkoutActionType.pretty(): String = name.lowercase().replace('_', ' ')

    companion object {
        const val MAX_PARTY = 4
        private const val STRENGTH_METRIC = 200L
        private const val CARDIO_METRIC = 360L
        private const val PR_METRIC = 260L
        private const val QUEST_METRIC = 140L
        private const val MOBILITY_METRIC = 160L
        private const val DEFAULT_BUDGET_SECONDS = 600L
        private const val FAIL_BUDGET_FACTOR = 3L
        private const val STRENGTH_REFERENCE_KG = 60.0
    }
}

@Composable
fun rememberDungeonPrototypeController(provider: NearbyAscendedProvider): DungeonPrototypeController =
    remember { DungeonPrototypeController(provider) }
