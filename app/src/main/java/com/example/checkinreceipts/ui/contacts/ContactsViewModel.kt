package com.example.checkinreceipts.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.checkinreceipts.data.db.DatabaseProvider
import com.example.checkinreceipts.data.entity.ContactEntity
import com.example.checkinreceipts.data.repo.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactsUiState(
    val items: List<ContactEntity> = emptyList(),
    val loading: Boolean = false
)

class ContactsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: ContactsRepository by lazy {
        val db = DatabaseProvider.get(app)
        ContactsRepository(db.contactDao(), Dispatchers.IO, null)
    }
    private val loading = MutableStateFlow(false)
    private val items = repo.observe()

    val state: StateFlow<ContactsUiState> =
        combine(items, loading) { list, isLoading -> ContactsUiState(items = list, loading = isLoading) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactsUiState())

    fun initialSync() = refresh()

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            repo.refresh()
            loading.value = false
        }
    }

    fun autoRetry() {
        viewModelScope.launch { repo.retryPending() }
    }

    fun add(name: String, phone: String) {
        viewModelScope.launch {
            repo.addLocalThenSync(name, phone)
            repo.retryPending()
        }
    }

    fun update(target: ContactEntity, newName: String, newPhone: String) {
        viewModelScope.launch {
            repo.updateLocalThenSync(target.id, newName, newPhone)
            repo.retryPending()
        }
    }

    fun delete(item: ContactEntity) {
        viewModelScope.launch { repo.markPendingDelete(item.id) }
    }

    fun undoDelete(localId: Long) {
        viewModelScope.launch { repo.undoDelete(localId) }
    }

    fun commitDelete(localId: Long) {
        viewModelScope.launch { repo.commitDelete(localId) }
    }
}