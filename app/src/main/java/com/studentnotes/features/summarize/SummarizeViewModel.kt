package com.studentnotes.features.summarize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studentnotes.data.local.Note
import com.studentnotes.data.local.NoteDao
import com.studentnotes.inference.InferenceClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SummarizeUiState(
    val title: String = "",
    val noteText: String = "",
    val summary: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class SummarizeViewModel(
    private val noteDao: NoteDao,
    private val inferenceClient: InferenceClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummarizeUiState())
    val uiState: StateFlow<SummarizeUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, error = null)
    }

    fun updateNoteText(text: String) {
        _uiState.value = _uiState.value.copy(noteText = text, error = null)
    }

    fun summarize() {
        val noteText = _uiState.value.noteText.trim()
        if (noteText.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter some text to summarize")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val summary = inferenceClient.summarize(noteText)
                _uiState.value = _uiState.value.copy(
                    summary = summary,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to generate summary"
                )
            }
        }
    }

    fun saveNote(onSaved: () -> Unit) {
        val state = _uiState.value
        val content = state.noteText.trim()
        if (content.isEmpty()) {
            _uiState.value = state.copy(error = "Cannot save empty note")
            return
        }

        viewModelScope.launch {
            try {
                val title = state.title.ifBlank {
                    content.take(50).let { if (content.length > 50) "$it..." else it }
                }
                val note = Note(
                    title = title,
                    content = content,
                    summary = state.summary.ifBlank { null }
                )
                noteDao.insertNote(note)
                _uiState.value = state.copy(isSaved = true)
                onSaved()
            } catch (e: Exception) {
                _uiState.value = state.copy(error = "Failed to save note: ${e.message}")
            }
        }
    }

    fun clearAll() {
        _uiState.value = SummarizeUiState()
    }

    companion object {
        fun factory(noteDao: NoteDao, inferenceClient: InferenceClient): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SummarizeViewModel(noteDao, inferenceClient) as T
                }
            }
        }
    }
}
