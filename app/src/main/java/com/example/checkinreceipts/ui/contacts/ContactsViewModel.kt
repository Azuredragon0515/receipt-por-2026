package com.example.checkinreceipts.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.checkinreceipts.data.db.DatabaseProvider
import com.example.checkinreceipts.data.entity.ContactEntity
import com.example.checkinreceipts.data.repo.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactsUiState(
    val items: List<ContactEntity> = emptyList(),
    val loading: Boolean = false
)

class ContactsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: ContactsRepository by lazy {
        val db = DatabaseProvider.get(app)
        ContactsRepository(db.contactDao(), Dispatchers.IO)
    }
    private val loading = MutableStateFlow(false)

    val state: StateFlow<ContactsUiState> =
        repo.observe()
            .map { ContactsUiState(items = it, loading = loading.value) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactsUiState())

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar

    fun initialSync() = refresh()

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            val r = repo.refresh()
            loading.value = false
            if (r.isFailure) _snackbar.emit("Refresh failed")
        }
    }

    fun add(name: String, phone: String) {
        viewModelScope.launch {
            if (name.isBlank()) { _snackbar.emit("Name required"); return@launch }
            val r = repo.addLocalThenSync(name, phone)
            if (r.isFailure) {
                _snackbar.emit("Create failed. Tap retry.")
            } else {
                _snackbar.emit("Created")
            }
        }
    }

    fun delete(item: ContactEntity) {
        viewModelScope.launch {
            val r = repo.deleteOptimistic(item)
            if (r.isFailure) {
                _snackbar.emit("Delete failed. Tap undo.")
            } else {
                _snackbar.emit("Delete requested")
            }
        }
    }

    fun undoDelete(localId: Long) {
        viewModelScope.launch {
            val r = repo.undoDelete(localId)
            if (r.isFailure) _snackbar.emit("Undo failed")
        }
    }

    fun retryPending() {
        viewModelScope.launch {
            val r = repo.retryPending()
            if (r.isFailure) _snackbar.emit("Retry failed")
        }
    }
}