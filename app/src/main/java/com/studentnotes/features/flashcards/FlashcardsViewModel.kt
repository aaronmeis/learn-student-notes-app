package com.studentnotes.features.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studentnotes.data.local.Flashcard
import com.studentnotes.data.repository.FlashcardRepository
import com.studentnotes.data.repository.NoteRepository
import com.studentnotes.inference.InferenceClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FlashcardsUiState(
    val isGenerating: Boolean = false,
    val currentCardIndex: Int = 0,
    val showAnswer: Boolean = false,
    val error: String? = null
)

class FlashcardsViewModel(
    private val flashcardRepository: FlashcardRepository,
    private val noteRepository: NoteRepository,
    private val inferenceClient: InferenceClient
) : ViewModel() {

    val flashcards: StateFlow<List<Flashcard>> = flashcardRepository.getAllFlashcards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(FlashcardsUiState())
    val uiState: StateFlow<FlashcardsUiState> = _uiState.asStateFlow()

    fun generateFlashcardsForNote(noteId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)

            try {
                val note = noteRepository.getNoteByIdOnce(noteId)
                if (note == null) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = "Note not found"
                    )
                    return@launch
                }

                val results = inferenceClient.generateFlashcards(note.content)
                val flashcards = results.map { result ->
                    Flashcard(
                        noteId = noteId,
                        question = result.question,
                        answer = result.answer
                    )
                }
                flashcardRepository.insertFlashcards(flashcards)

                _uiState.value = _uiState.value.copy(isGenerating = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Failed to generate flashcards"
                )
            }
        }
    }

    fun nextCard() {
        val currentIndex = _uiState.value.currentCardIndex
        val totalCards = flashcards.value.size
        if (currentIndex < totalCards - 1) {
            _uiState.value = _uiState.value.copy(
                currentCardIndex = currentIndex + 1,
                showAnswer = false
            )
        }
    }

    fun previousCard() {
        val currentIndex = _uiState.value.currentCardIndex
        if (currentIndex > 0) {
            _uiState.value = _uiState.value.copy(
                currentCardIndex = currentIndex - 1,
                showAnswer = false
            )
        }
    }

    fun toggleAnswer() {
        _uiState.value = _uiState.value.copy(
            showAnswer = !_uiState.value.showAnswer
        )
    }

    fun resetCards() {
        _uiState.value = _uiState.value.copy(
            currentCardIndex = 0,
            showAnswer = false
        )
    }

    fun deleteFlashcard(flashcard: Flashcard) {
        viewModelScope.launch {
            flashcardRepository.deleteFlashcard(flashcard)
        }
    }

    companion object {
        fun factory(
            flashcardRepository: FlashcardRepository,
            noteRepository: NoteRepository,
            inferenceClient: InferenceClient
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FlashcardsViewModel(
                        flashcardRepository,
                        noteRepository,
                        inferenceClient
                    ) as T
                }
            }
        }
    }
}
