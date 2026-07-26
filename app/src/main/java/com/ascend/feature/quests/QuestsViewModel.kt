package com.ascend.feature.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.core.domain.repository.QuestRepository
import com.ascend.core.domain.usecase.SeedDemoDataUseCase
import com.ascend.core.model.Quest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestsUiState(
    val isLoading: Boolean = true,
    val quests: List<Quest> = emptyList(),
)

@HiltViewModel
class QuestsViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val questRepository: QuestRepository,
    private val seedDemoData: SeedDemoDataUseCase,
) : ViewModel() {

    private val userId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<QuestsUiState> = userId
        .filterNotNull()
        .flatMapLatest { uid ->
            questRepository.observeQuestsForUser(uid).map { QuestsUiState(isLoading = false, quests = it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestsUiState())

    init {
        viewModelScope.launch {
            val uid = playerRepository.ensureLocalPlayer()
            seedDemoData(uid)
            userId.value = uid
        }
    }
}
