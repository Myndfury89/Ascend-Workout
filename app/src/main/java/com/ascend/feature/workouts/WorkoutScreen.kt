package com.ascend.feature.workouts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ascend.R
import com.ascend.core.designsystem.component.CenteredPlaceholder

@Composable
fun WorkoutScreen(modifier: Modifier = Modifier) {
    CenteredPlaceholder(
        title = stringResource(R.string.dest_workout),
        message = stringResource(R.string.workout_placeholder),
        icon = Icons.Filled.FitnessCenter,
        modifier = modifier,
    )
}
