package com.ascend.feature.dungeon.prototype

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.core.domain.dungeon.DungeonCatalog
import com.ascend.core.model.MemberActivityState
import com.ascend.core.model.WorkoutActionType

private val ACTIONS =
    listOf(
        WorkoutActionType.STRENGTH_SET to "Strength",
        WorkoutActionType.CARDIO_DURATION to "Cardio",
        WorkoutActionType.PERSONAL_RECORD to "PR",
        WorkoutActionType.QUEST_INTERVAL to "Quest",
        WorkoutActionType.MOBILITY_OR_AGILITY to "Mobility",
        WorkoutActionType.RECOVERY to "Recovery",
    )

@Composable
internal fun EncounterSection(controller: DungeonPrototypeController) {
    val combat = controller.combat ?: return
    val enemy = DungeonCatalog.dungeonById(controller.selectedDungeonId)?.enemies?.first()

    SectionCard(enemy?.name ?: "Enemy") {
        Text(
            "Phase ${combat.phaseIndex + 1} · ${controller.scaling?.difficulty ?: ""}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = { combat.enemyHealthFraction }, modifier = Modifier.fillMaxWidth().height(12.dp))
        Text("${combat.enemyHealth} / ${combat.maxEnemyHealth}", fontSize = 12.sp)
        if (combat.partyShield > 0 || combat.partyStamina > 0) {
            Text(
                "Shield ${combat.partyShield} · Stamina ${combat.partyStamina}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    SectionCard("Party contribution") {
        controller.party.forEach { m -> MemberRow(controller, m.id) }
    }

    SectionCard("Contribution timeline") {
        if (controller.timeline.isEmpty()) {
            Text("Trigger a workout action to contribute.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        controller.timeline.take(TIMELINE_LIMIT).forEach { entry ->
            Text(
                "${entry.handle}: ${entry.text}",
                fontSize = 11.sp,
                color = if (entry.isContribution) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberRow(
    controller: DungeonPrototypeController,
    memberId: String,
) {
    val member = controller.party.firstOrNull { it.id == memberId } ?: return
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(member.handle, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("earned ${member.earnedContribution}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(stateLabel(member.activityState), fontSize = 11.sp, color = stateColor(member.activityState))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ACTIONS.forEach { (action, label) ->
                OutlinedButton(onClick = {
                    controller.trigger(memberId, action)
                }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                    Text(label, fontSize = 10.sp)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { controller.setActivityState(memberId, MemberActivityState.ACTIVE) },
            ) { Text("Active", fontSize = 10.sp) }
            OutlinedButton(
                onClick = { controller.setActivityState(memberId, MemberActivityState.PAUSED) },
            ) { Text("Pause", fontSize = 10.sp) }
            OutlinedButton(onClick = {
                controller.setActivityState(memberId, MemberActivityState.DISCONNECTED)
            }) { Text("Disconnect", fontSize = 10.sp) }
            OutlinedButton(onClick = { controller.safetyStop(memberId) }) { Text("Safety stop", fontSize = 10.sp) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DebugFooter(controller: DungeonPrototypeController) {
    SectionCard("Debug") {
        Text("Weight unit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        com.ascend.core.designsystem.component.WeightUnitSelector(
            selected = controller.weightUnit,
            onSelect = { controller.weightUnit = it },
        )
        Text(controller.strengthReferenceLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text("Simulated clock: ${controller.nowMillis / 1000}s", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { controller.advanceTimer(30) }) { Text("+30s", fontSize = 11.sp) }
            OutlinedButton(onClick = { controller.advanceTimer(120) }) { Text("+2m", fontSize = 11.sp) }
            OutlinedButton(onClick = { controller.reset() }) { Text("Reset encounter", fontSize = 11.sp) }
        }
    }
}

@Composable
private fun stateColor(state: MemberActivityState) =
    when (state) {
        MemberActivityState.ACTIVE -> MaterialTheme.colorScheme.primary
        MemberActivityState.SAFETY_PAUSED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun stateLabel(state: MemberActivityState): String =
    when (state) {
        MemberActivityState.ACTIVE -> "Contributing"
        MemberActivityState.IDLE_WARNING -> "Idle warning"
        MemberActivityState.INACTIVE -> "Inactive"
        MemberActivityState.PAUSED -> "Paused"
        MemberActivityState.DISCONNECTED -> "Reconnecting"
        MemberActivityState.SAFETY_PAUSED -> "Contribution paused"
    }

private const val TIMELINE_LIMIT = 10
