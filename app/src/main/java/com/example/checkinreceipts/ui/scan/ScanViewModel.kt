package com.example.checkinreceipts.ui.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.checkinreceipts.core.util.extractDate
import com.example.checkinreceipts.core.util.extractHeader
import com.example.checkinreceipts.core.util.extractTotal
import com.example.checkinreceipts.data.repo.RecordRepository
import com.example.checkinreceipts.device.ocr.recognizeTextFromUri
import com.example.checkinreceipts.domain.model.Record
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ScanUiState(
    val header: String? = null,
    val dateText: String? = null,
    val totalText: String? = null,
    val note: String? = null,
    val rawText: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RecordRepository.getInstance(app)
    private val _ui = MutableStateFlow(ScanUiState())
    val ui: StateFlow<ScanUiState> = _ui

    fun updateHeader(v: String) { _ui.value = _ui.value.copy(header = v) }
    fun updateDate(v: String) { _ui.value = _ui.value.copy(dateText = v) }
    fun updateTotal(v: String) { _ui.value = _ui.value.copy(totalText = v) }
    fun updateNote(v: String) { _ui.value = _ui.value.copy(note = v) }

    fun ocrFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(error = null)
                val text = recognizeTextFromUri(getApplication(), uri)
                val full = text.text ?: ""
                _ui.value = _ui.value.copy(
                    rawText = full,
                    header = extractHeader(full) ?: _ui.value.header,
                    dateText = extractDate(full) ?: _ui.value.dateText,
                    totalText = extractTotal(full) ?: _ui.value.totalText
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = e.message ?: "OCR failed")
            }
        }
    }

    fun save(onSaved: () -> Unit) {
        val s = _ui.value
        viewModelScope.launch {
            try {
                _ui.value = s.copy(isSaving = true, error = null)
                repo.add(
                    Record(
                        header = s.header,
                        dateText = s.dateText,
                        totalText = s.totalText,
                        note = s.note
                    )
                )
                _ui.value = ScanUiState()
                onSaved()
            } catch (e: Exception) {
                _ui.value = s.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}