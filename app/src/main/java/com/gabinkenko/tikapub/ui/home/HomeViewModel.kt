package com.gabinkenko.tikapub.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.gabinkenko.tikapub.AppContainer
import com.gabinkenko.tikapub.data.db.PublishLogEntity
import com.gabinkenko.tikapub.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val isLinked: Boolean = false,
    val lastLog: PublishLogEntity? = null,
    val isPublishing: Boolean = false,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val isLinked = MutableStateFlow(container.tokenStore.isLinked)

    val uiState: StateFlow<HomeUiState> = combine(
        container.settingsRepository.settings,
        container.publishLogDao.observeRecent(1),
        isLinked,
        container.workScheduler.observeManualRun(),
    ) { settings, logs, linked, manualWorkInfos ->
        HomeUiState(
            settings = settings,
            isLinked = linked,
            lastLog = logs.firstOrNull(),
            isPublishing = manualWorkInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun refreshLinkState() {
        isLinked.value = container.tokenStore.isLinked
    }

    fun setAutoPublishEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setAutoPublishEnabled(enabled)
            container.workScheduler.reschedule(
                hour = uiState.value.settings.publishHour,
                minute = uiState.value.settings.publishMinute,
                enabled = enabled,
            )
        }
    }

    fun publishNow() {
        container.workScheduler.runNow()
    }
}
