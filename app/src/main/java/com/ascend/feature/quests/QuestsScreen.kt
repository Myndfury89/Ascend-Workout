package com.ascend.feature.quests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.R
import com.ascend.core.designsystem.component.CenteredPlaceholder
import com.ascend.core.model.Quest

@Composable
fun QuestsScreen(
    onQuestClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuestsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (!state.isLoading && state.quests.isEmpty()) {
        CenteredPlaceholder(
            title = stringResource(R.string.dest_quests),
            message = stringResource(R.string.quests_empty),
            icon = Icons.Filled.CheckCircle,
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        items(state.quests, key = { it.id }) { quest ->
            Spacer(Modifier.height(12.dp))
            QuestListCard(quest = quest, onClick = { onQuestClick(quest.id) })
        }
    }
}

@Composable
private fun QuestListCard(quest: Quest, onClick: () -> Unit) {
    val current = quest.totalCurrent.toInt()
    val target = quest.totalTarget.toInt()
    val unit = quest.primaryObjective?.unit.orEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(quest.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { quest.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    // Progress is announced by the text below; avoid duplicate reads.
                    .clearAndSetSemantics { },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$current / $target $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
