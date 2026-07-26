package com.ascend.feature.quests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ascend.core.domain.progression.SetSuggestion
import com.ascend.core.domain.progression.SetSuggestionEngine
import com.ascend.core.domain.progression.SetSuggestionInput
import com.ascend.core.domain.repository.CompleteQuestResult
import com.ascend.core.domain.repository.QuestRepository
import com.ascend.core.model.Quest
import com.ascend.navigation.ActiveQuest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompletionInfo(val xp: Long, val leveledUp: Boolean, val newLevel: Int)

data class ActiveQuestUiState(
    val isLoading: Boolean = true,
    val quest: Quest? = null,
    val suggestion: SetSuggestion? = null,
    val completion: CompletionInfo? = null,
)

@HiltViewModel
class QuestViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val questRepository: QuestRepository,
        private val setSuggestionEngine: SetSuggestionEngine,
    ) : ViewModel() {
        private val questId = savedStateHandle.toRoute<ActiveQuest>().questId
        private val completion = MutableStateFlow<CompletionInfo?>(null)

        val uiState: StateFlow<ActiveQuestUiState> =
            combine(questRepository.observeQuest(questId), completion) { quest, comp ->
                ActiveQuestUiState(
                    isLoading = false,
                    quest = quest,
                    suggestion = quest?.let(::suggestionFor),
                    completion = comp,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveQuestUiState())

        private fun suggestionFor(quest: Quest): SetSuggestion? {
            val objective = quest.primaryObjective ?: return null
            val remaining = objective.remaining.toInt()
            if (remaining <= 0) return null
            return setSuggestionEngine.suggest(
                SetSuggestionInput(
                    remaining = remaining,
                    preferredSetSize = objective.preferredSetSize ?: 10,
                    minimumSetSize = objective.minimumSetSize ?: 1,
                    maximumSetSize = objective.maximumSetSize,
                ),
            )
        }

        fun addProgress(value: Int) =
            viewModelScope.launch {
                val objective = uiState.value.quest?.primaryObjective ?: return@launch
                if (value > 0) questRepository.addProgress(objective.id, value.toDouble())
            }

        fun deleteEntry(entryId: String) =
            viewModelScope.launch {
                questRepository.deleteProgress(entryId)
            }

        fun complete() =
            viewModelScope.launch {
                val quest = uiState.value.quest ?: return@launch
                when (val result = questRepository.completeQuest(quest.id)) {
                    is CompleteQuestResult.Completed ->
                        completion.value = CompletionInfo(result.xpAwarded, result.leveledUp, result.newLevel)
                    else -> Unit
                }
            }

        fun dismissCompletion() {
            completion.value = null
        }
    }
