package com.ascend.feature.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.ascend.core.model.Difficulty
import com.ascend.core.model.Exercise
import com.ascend.core.model.WorkoutSet

@Composable
fun LogWorkoutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogWorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LogWorkoutContent(
        state = state,
        onBack = onBack,
        onTitleChange = viewModel::setTitle,
        onDifficultyChange = viewModel::setDifficulty,
        onAddSet = viewModel::addSet,
        onDeleteSet = viewModel::deleteSet,
        onFinish = viewModel::finish,
        onDismissCompletion = {
            viewModel.dismissCompletion()
            onBack()
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun LogWorkoutContent(
    state: LogWorkoutUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onAddSet: (Exercise, Int, Double?) -> Unit,
    onDeleteSet: (String) -> Unit,
    onFinish: () -> Unit,
    onDismissCompletion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Text(stringResource(R.string.workout_log_action), style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.workout_title_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.workout_difficulty_label), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { level ->
                FilterChip(
                    selected = state.difficulty == level,
                    onClick = { onDifficultyChange(level) },
                    label = { Text(level.displayName) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        AddSetSection(exercises = state.exercises, onAddSet = onAddSet)

        if (state.sets.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.workout_logged_sets), style = MaterialTheme.typography.titleMedium)
            state.sets.forEachIndexed { index, set ->
                LoggedSetRow(index = index, set = set, onDelete = { onDeleteSet(set.id) })
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryActionButton(
            text = stringResource(R.string.workout_finish),
            onClick = onFinish,
            enabled = state.canFinish,
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
                        stringResource(R.string.workout_complete_title)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSetSection(
    exercises: List<Exercise>,
    onAddSet: (Exercise, Int, Double?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember(exercises) { mutableStateOf(exercises.firstOrNull()) }
    var valueText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }

    Text(stringResource(R.string.workout_add_set), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: stringResource(R.string.workout_pick_exercise),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.workout_exercise_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = { Text("${exercise.name}  ·  ${exercise.category}") },
                    onClick = {
                        selected = exercise
                        expanded = false
                    },
                )
            }
        }
    }

    val exercise = selected
    val weighted = exercise?.isWeighted == true
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = valueText,
            onValueChange = { valueText = it.filter(Char::isDigit).take(5) },
            label = { Text(exercise?.defaultUnit ?: stringResource(R.string.workout_value_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(140.dp),
        )
        if (weighted) {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                label = { Text(stringResource(R.string.workout_weight_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp),
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    val parsedValue = valueText.toIntOrNull()
    QuickProgressButton(
        label = stringResource(R.string.workout_add_set),
        enabled = exercise != null && parsedValue != null && parsedValue > 0,
        onClick = {
            val ex = exercise ?: return@QuickProgressButton
            val v = parsedValue ?: return@QuickProgressButton
            onAddSet(ex, v, weightText.toDoubleOrNull())
            valueText = ""
            weightText = ""
        },
    )
}

@Composable
private fun LoggedSetRow(
    index: Int,
    set: WorkoutSet,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val weightSuffix = set.weight?.let { " @ ${it.toInt()} kg" }.orEmpty()
        Text(
            "Set ${index + 1}: ${set.exerciseName} — ${set.volume.toInt()} ${set.unit}$weightSuffix",
            style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.workout_delete_set))
        }
    }
}
