package com.example.checkinreceipts.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.checkinreceipts.data.repo.RecordRepository
import com.example.checkinreceipts.domain.model.Record
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordsListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RecordRepository.getInstance(app)
    private val query = MutableStateFlow("")
    val q: StateFlow<String> = query
    val records: StateFlow<List<Record>> =
        query.flatMapLatest { _ -> repo.observeAll() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun setQuery(s: String) { query.value = s }

    fun seedDemoData() = viewModelScope.launch {
        repo.addAll(
            Record(header = "Coffee Shop", dateText = "2025-01-07", totalText = "42.00", note = "Latte x2"),
            Record(header = "SuperMart", dateText = "07/01/2025", totalText = "168.90", note = "Snacks"),
            Record(header = "Metro", dateText = "2025/01/06", totalText = "23.50"),
            Record(header = "Cinema", dateText = "2025-01-05", totalText = "98.00", note = "Movie night")
        )
    }
}

