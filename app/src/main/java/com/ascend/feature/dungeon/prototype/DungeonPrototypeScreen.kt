package com.ascend.feature.dungeon.prototype

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.core.domain.dungeon.DungeonCatalog

/**
 * The fake, debug-only party Dungeon prototype UI. Reviewable in a debug build; entirely simulated
 * (no Bluetooth, no real users, no networking). Lets a reviewer discover fake nearby Ascended,
 * explicitly join a party, ready up, and run a deterministic encounter with simulated
 * workout-event triggers.
 */
@Composable
fun DungeonPrototypeScreen(
    modifier: Modifier = Modifier,
    controller: DungeonPrototypeController =
        rememberDungeonPrototypeController(remember { FakeNearbyAscendedProvider() }),
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text("Fake Party Dungeon", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Simulated only — no Bluetooth, no real nearby users, no networking.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        when (controller.phase) {
            DungeonPrototypePhase.LOBBY -> LobbySection(controller)
            DungeonPrototypePhase.READY_CHECK -> ReadySection(controller)
            DungeonPrototypePhase.IN_ENCOUNTER -> EncounterSection(controller)
            DungeonPrototypePhase.VICTORY, DungeonPrototypePhase.DEFEAT -> ResultSection(controller)
        }

        Spacer(Modifier.height(16.dp))
        DebugFooter(controller)
        Spacer(Modifier.height(28.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LobbySection(controller: DungeonPrototypeController) {
    val nearby by controller.provider.nearby.collectAsState()
    SectionCard("Dungeon") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DungeonCatalog.DUNGEONS.forEach { d ->
                FilterChip(
                    selected = d.id == controller.selectedDungeonId,
                    onClick = { controller.selectedDungeonId = d.id },
                    label = { Text("${d.name} · T${d.tier}", fontSize = 11.sp) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = controller.solo, onCheckedChange = { controller.solo = it })
            Text("  Solo mode", fontSize = 13.sp)
        }
    }
    SectionCard("Nearby Ascended (simulated)") {
        nearby.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.handle, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Signal: ${p.signal.label}${p.classId?.let { " · $it" } ?: ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = { controller.join(p) }) { Text("Join", fontSize = 12.sp) }
            }
        }
        OutlinedButton(onClick = { controller.provider.rotate() }) { Text("Rotate nearby", fontSize = 12.sp) }
    }
    PartyPreview(controller)
    Button(onClick = { controller.beginReadyCheck() }, enabled = controller.party.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
        Text("Begin ready check")
    }
}

@Composable
private fun PartyPreview(controller: DungeonPrototypeController) {
    SectionCard("Party (${controller.party.size}/${DungeonPrototypeController.MAX_PARTY})") {
        if (controller.party.isEmpty()) {
            Text("No members yet — join from the list above.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        controller.party.forEach { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(m.handle + (m.classId?.let { " · $it" } ?: ""), fontSize = 12.sp, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { controller.leave(m.id) }) { Text("Leave", fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun ReadySection(controller: DungeonPrototypeController) {
    SectionCard("Ready check") {
        controller.party.forEach { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(m.handle, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    if (m.ready) "Ready" else "Not ready",
                    fontSize = 12.sp,
                    color = if (m.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(0.dp))
                Switch(checked = m.ready, onCheckedChange = { controller.toggleReady(m.id) })
            }
        }
    }
    Button(onClick = { controller.startEncounter() }, enabled = controller.allReady, modifier = Modifier.fillMaxWidth()) {
        Text(if (controller.allReady) "Start encounter" else "Waiting for ready")
    }
    OutlinedButton(onClick = { controller.reset() }, modifier = Modifier.fillMaxWidth()) { Text("Back to lobby") }
}

@Composable
private fun ResultSection(controller: DungeonPrototypeController) {
    val victory = controller.phase == DungeonPrototypePhase.VICTORY
    SectionCard(if (victory) "Victory" else "Encounter failed") {
        Text(
            if (victory) "The party prevailed." else "The encounter ended before the enemy fell.",
            fontSize = 14.sp,
            color = if (victory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        controller.reward?.let { r ->
            Spacer(Modifier.height(8.dp))
            Text("Reward XP: ${r.playerXp}", fontSize = 13.sp)
            r.perMemberXp.forEach { (id, xp) ->
                val handle = controller.party.firstOrNull { it.id == id }?.handle ?: id
                Text("  $handle: $xp", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Button(onClick = { controller.reset() }, modifier = Modifier.fillMaxWidth()) { Text("Return to lobby") }
}

@Composable
internal fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title.uppercase(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}
