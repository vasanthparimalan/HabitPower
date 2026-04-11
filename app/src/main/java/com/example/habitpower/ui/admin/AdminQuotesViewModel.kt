package com.example.habitpower.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.Quote
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminQuotesViewModel(private val repository: HabitPowerRepository) : ViewModel() {
    val quotes: StateFlow<List<Quote>> = repository.allQuotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addQuote(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.insertQuote(Quote(text = text.trim()))
        }
    }

    fun deleteQuote(quote: Quote) {
        viewModelScope.launch {
            repository.deleteQuote(quote)
        }
    }
}
