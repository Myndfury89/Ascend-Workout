package com.ascend.feature.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.R
import com.ascend.core.designsystem.component.PrimaryActionButton
import com.ascend.core.designsystem.component.QuickProgressButton
import com.ascend.core.model.Quest
import com.ascend.core.model.QuestStatus

private val QUICK_ADD_VALUES = listOf(10, 25, 40, 50)

@Composable
fun ActiveQuestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ActiveQuestContent(
        state = state,
        onBack = onBack,
        onAdd = viewModel::addProgress,
        onDelete = viewModel::deleteEntry,
        onComplete = viewModel::complete,
        onDismissCompletion = {
            viewModel.dismissCompletion()
            onBack()
        },
        modifier = modifier,
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ActiveQuestContent(
    state: ActiveQuestUiState,
    onBack: () -> Unit,
    onAdd: (Int) -> Unit,
    onDelete: (String) -> Unit,
    onComplete: () -> Unit,
    onDismissCompletion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quest = state.quest
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Spacer(Modifier.height(0.dp))
            Text(
                text = quest?.title ?: stringResource(R.string.dest_quests),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        if (quest == null) {
            if (state.isLoading) {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
            }
            return@Column
        }

        val objective = quest.primaryObjective
        val current = quest.totalCurrent.toInt()
        val target = quest.totalTarget.toInt()
        val remaining = quest.remaining.toInt()
        val unit = objective?.unit.orEmpty()

        quest.description?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))
        Text("$current / $target $unit", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { quest.fraction }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "${stringResource(R.string.quest_remaining)}: $remaining $unit  •  " +
                "${stringResource(R.string.quest_reward)}: ${quest.baseRewardXp} XP",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Set suggestion
        Spacer(Modifier.height(8.dp))
        val suggestionText = state.suggestion?.primary?.sets
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?.let { "${stringResource(R.string.quest_suggested)}: $it" }
            ?: stringResource(R.string.quest_no_suggestion)
        Text(suggestionText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

        // Quick add
        Spacer(Modifier.height(16.dp))
        var showCustom by remember { mutableStateOf(false) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_ADD_VALUES.forEach { value ->
                QuickProgressButton(label = "+$value", onClick = { onAdd(value) })
            }
            QuickProgressButton(
                label = stringResource(R.string.quest_quick_add_custom),
                onClick = { showCustom = true },
            )
        }

        if (showCustom) {
            CustomAddDialog(
                onAdd = { onAdd(it); showCustom = false },
                onDismiss = { showCustom = false },
            )
        }

        // Logged sets
        if (objective != null && objective.entries.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.quest_set_history), style = MaterialTheme.typography.titleMedium)
            objective.entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Set ${index + 1}: +${entry.value.toInt()} $unit", style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { onDelete(entry.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.quest_delete_set))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        val alreadyDone = quest.status == QuestStatus.COMPLETED || quest.status == QuestStatus.OVER_COMPLETED
        PrimaryActionButton(
            text = stringResource(R.string.quest_complete),
            onClick = onComplete,
            enabled = !alreadyDone,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }

    state.completion?.let { completion ->
        AlertDialog(
            onDismissRequest = onDismissCompletion,
            confirmButton = {
                TextButton(onClick = onDismissCompletion) { Text(stringResource(R.string.quest_dismiss)) }
            },
            title = {
                Text(
                    if (completion.leveledUp) {
                        stringResource(R.string.quest_level_up_title)
                    } else {
                        stringResource(R.string.quest_completed_title)
                    },
                )
            },
            text = {
                val levelLine = if (completion.leveledUp) "  •  Level ${completion.newLevel}" else ""
                Text("+${completion.xp} XP$levelLine")
            },
        )
    }
}

@Composable
private fun CustomAddDialog(onAdd: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val parsed = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quest_add_custom_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter(Char::isDigit).take(4) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onAdd) },
                enabled = parsed != null && parsed > 0,
            ) { Text(stringResource(R.string.quest_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.quest_cancel)) }
        },
    )
}
