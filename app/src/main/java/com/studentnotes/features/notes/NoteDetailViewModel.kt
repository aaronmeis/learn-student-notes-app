package com.studentnotes.features.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studentnotes.data.local.Note
import com.studentnotes.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteDetailUiState(
    val note: Note? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val title: String = "",
    val content: String = ""
)

class NoteDetailViewModel(
    private val repository: NoteRepository,
    private val noteId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        loadNote()
    }

    private fun loadNote() {
        if (noteId == null || noteId == 0L) {
            _uiState.value = NoteDetailUiState(isLoading = false)
            return
        }

        viewModelScope.launch {
            val note = repository.getNoteByIdOnce(noteId)
            _uiState.value = NoteDetailUiState(
                note = note,
                isLoading = false,
                title = note?.title ?: "",
                content = note?.content ?: ""
            )
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateContent(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
    }

    fun saveNote(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) return

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val note = state.note
            if (note != null) {
                repository.updateNote(
                    note.copy(
                        title = state.title,
                        content = state.content,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                repository.insertNote(
                    Note(
                        title = state.title,
                        content = state.content
                    )
                )
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSaved()
        }
    }

    companion object {
        fun factory(repository: NoteRepository, noteId: Long?): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NoteDetailViewModel(repository, noteId) as T
                }
            }
        }
    }
}
