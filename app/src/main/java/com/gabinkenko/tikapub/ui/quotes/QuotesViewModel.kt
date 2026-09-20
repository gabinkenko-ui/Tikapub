package com.gabinkenko.tikapub.ui.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabinkenko.tikapub.AppContainer
import com.gabinkenko.tikapub.data.db.QuoteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuotesViewModel(private val container: AppContainer) : ViewModel() {

    val quotes: StateFlow<List<QuoteEntity>> = container.quoteDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addQuote(text: String, author: String?, category: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            container.quoteDao.insert(
                QuoteEntity(text = text.trim(), author = author?.trim()?.ifBlank { null }, category = category.ifBlank { "general" }),
            )
        }
    }

    fun setEnabled(quote: QuoteEntity, enabled: Boolean) {
        viewModelScope.launch { container.quoteDao.update(quote.copy(enabled = enabled)) }
    }

    fun delete(quote: QuoteEntity) {
        viewModelScope.launch { container.quoteDao.delete(quote.id) }
    }
}
