package com.studentnotes.features.qa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studentnotes.data.local.Note
import com.studentnotes.data.repository.NoteRepository
import com.studentnotes.inference.InferenceClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class QAUiState(
    val messages: List<ChatMessage> = emptyList(),
    val selectedNoteId: Long? = null,
    val question: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class QAViewModel(
    private val noteRepository: NoteRepository,
    private val inferenceClient: InferenceClient
) : ViewModel() {

    val notes: StateFlow<List<Note>> = noteRepository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(QAUiState())
    val uiState: StateFlow<QAUiState> = _uiState.asStateFlow()

    fun selectNote(noteId: Long) {
        _uiState.value = _uiState.value.copy(selectedNoteId = noteId)
    }

    fun updateQuestion(question: String) {
        _uiState.value = _uiState.value.copy(question = question)
    }

    fun askQuestion() {
        val state = _uiState.value
        val noteId = state.selectedNoteId ?: return
        val question = state.question.trim()
        if (question.isEmpty()) return

        viewModelScope.launch {
            val userMessage = ChatMessage(content = question, isUser = true)
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + userMessage,
                question = "",
                isLoading = true,
                error = null
            )

            try {
                val note = noteRepository.getNoteByIdOnce(noteId)
                if (note == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Note not found"
                    )
                    return@launch
                }

                val answer = inferenceClient.answerQuestion(note.content, question)
                val assistantMessage = ChatMessage(content = answer, isUser = false)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMessage,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to get answer"
                )
            }
        }
    }

    fun clearChat() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    companion object {
        fun factory(
            noteRepository: NoteRepository,
            inferenceClient: InferenceClient
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return QAViewModel(noteRepository, inferenceClient) as T
                }
            }
        }
    }
}
