package com.gabinkenko.tikapub.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabinkenko.tikapub.AppContainer
import com.gabinkenko.tikapub.data.db.PublishLogEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(container: AppContainer) : ViewModel() {
    val logs: StateFlow<List<PublishLogEntity>> = container.publishLogDao.observeRecent(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
