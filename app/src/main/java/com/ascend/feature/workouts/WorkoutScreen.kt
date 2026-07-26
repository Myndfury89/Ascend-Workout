package com.ascend.feature.workouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.R
import com.ascend.core.designsystem.component.CenteredPlaceholder
import com.ascend.core.model.Workout
import com.ascend.core.model.WorkoutStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun WorkoutScreen(
    onLogWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        if (!state.isLoading && state.workouts.isEmpty()) {
            CenteredPlaceholder(
                title = stringResource(R.string.dest_workout),
                message = stringResource(R.string.workout_empty),
                icon = Icons.Filled.FitnessCenter,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            ) {
                items(state.workouts, key = { it.id }) { workout ->
                    Spacer(Modifier.height(12.dp))
                    WorkoutListCard(workout)
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onLogWorkout,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.workout_log_action)) },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
        )
    }
}

@Composable
private fun WorkoutListCard(workout: Workout) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(workout.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(workout.performedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            val summary =
                "${workout.setCount} sets  •  ${workout.totalVolume.toInt()} total volume" +
                    if (workout.status == WorkoutStatus.IN_PROGRESS) "  •  in progress" else ""
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (workout.status == WorkoutStatus.COMPLETED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}
