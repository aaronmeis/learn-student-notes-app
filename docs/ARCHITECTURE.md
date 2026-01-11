# Architecture

This document describes the overall architecture of the Student Notes app.

## Overview

The app follows **MVVM (Model-View-ViewModel)** architecture with a clean separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │ NotesScreen │  │ Flashcards  │  │  QAScreen   │          │
│  │             │  │   Screen    │  │             │          │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘          │
│         │                │                │                  │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐          │
│  │ NotesView   │  │ Flashcards  │  │  QAView     │          │
│  │   Model     │  │  ViewModel  │  │   Model     │          │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘          │
└─────────┼────────────────┼────────────────┼─────────────────┘
          │                │                │
┌─────────▼────────────────▼────────────────▼─────────────────┐
│                      Domain Layer                            │
│  ┌──────────────────┐      ┌──────────────────┐             │
│  │  NoteRepository  │      │FlashcardRepository│             │
│  └────────┬─────────┘      └────────┬─────────┘             │
└───────────┼─────────────────────────┼───────────────────────┘
            │                         │
┌───────────▼─────────────────────────▼───────────────────────┐
│                       Data Layer                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                 Room Database                        │    │
│  │  ┌─────────┐  ┌─────────────┐  ┌─────────────────┐  │    │
│  │  │ NoteDao │  │FlashcardDao │  │StudentNotesDB   │  │    │
│  │  └─────────┘  └─────────────┘  └─────────────────┘  │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │               Inference Layer                        │    │
│  │  ┌─────────────────┐    ┌─────────────────────┐     │    │
│  │  │InferenceClient  │◄───│    OllamaClient     │     │    │
│  │  │   (interface)   │    │  (implementation)   │     │    │
│  │  └─────────────────┘    └─────────────────────┘     │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Layers

### UI Layer

Composed of Jetpack Compose screens and ViewModels.

| Component | File | Responsibility |
|-----------|------|----------------|
| `NotesScreen` | [NotesScreen.kt](../app/src/main/java/com/studentnotes/features/notes/NotesScreen.kt) | Display list of notes |
| `NoteDetailScreen` | [NoteDetailScreen.kt](../app/src/main/java/com/studentnotes/features/notes/NoteDetailScreen.kt) | Create/edit a note |
| `FlashcardsScreen` | [FlashcardsScreen.kt](../app/src/main/java/com/studentnotes/features/flashcards/FlashcardsScreen.kt) | Flashcard viewer with flip animation |
| `QAScreen` | [QAScreen.kt](../app/src/main/java/com/studentnotes/features/qa/QAScreen.kt) | Chat interface for Q&A |

**ViewModels** expose UI state via `StateFlow` and handle user actions:

```kotlin
// Example: NotesViewModel
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {
    val notes: StateFlow<List<Note>> = repository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

### Domain Layer

Repositories abstract data sources from ViewModels.

| Repository | Purpose |
|------------|---------|
| `NoteRepository` | CRUD operations for notes |
| `FlashcardRepository` | CRUD operations for flashcards |

Repositories depend on DAOs (Room) and return `Flow<T>` for reactive data updates.

### Data Layer

#### Local Storage (Room)

See [DATABASE.md](DATABASE.md) for detailed schema documentation.

#### Inference (Ollama)

The inference layer provides AI capabilities through a clean interface:

```kotlin
interface InferenceClient {
    suspend fun generateFlashcards(noteContent: String): List<FlashcardResult>
    suspend fun answerQuestion(noteContent: String, question: String): String
    suspend fun summarize(noteContent: String): String
}
```

`OllamaClient` implements this interface using OkHttp to communicate with a local Ollama server.

## Data Flow

### Reading Notes

```
NotesScreen
    │
    │ collectAsState()
    ▼
NotesViewModel.notes (StateFlow)
    │
    │ stateIn()
    ▼
NoteRepository.getAllNotes() (Flow)
    │
    │ Room query
    ▼
NoteDao.getAllNotes()
    │
    ▼
SQLite Database
```

### Generating Flashcards

```
User taps "Generate Flashcards"
    │
    ▼
FlashcardsViewModel.generateFlashcardsForNote(noteId)
    │
    ├──► NoteRepository.getNoteByIdOnce(noteId)
    │         │
    │         ▼
    │    Note content retrieved
    │
    ├──► InferenceClient.generateFlashcards(content)
    │         │
    │         ▼
    │    OllamaClient sends HTTP POST to /api/generate
    │         │
    │         ▼
    │    LLM returns JSON flashcards
    │
    ▼
FlashcardRepository.insertFlashcards(flashcards)
    │
    ▼
UI updates via Flow
```

## Navigation

Navigation is handled by Jetpack Navigation Compose:

```kotlin
sealed class Screen(val route: String) {
    data object Notes : Screen("notes")
    data object NoteDetail : Screen("note/{noteId}")
    data object Flashcards : Screen("flashcards")
    data object QA : Screen("qa")
}
```

The navigation graph is defined in [StudentNotesApp.kt](../app/src/main/java/com/studentnotes/StudentNotesApp.kt).

## Dependency Management

Dependencies are created manually in `StudentNotesApplication`:

```kotlin
class StudentNotesApplication : Application() {
    val database: StudentNotesDatabase by lazy {
        StudentNotesDatabase.getDatabase(this)
    }
}
```

ViewModels use factory methods to receive dependencies:

```kotlin
companion object {
    fun factory(repository: NoteRepository): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotesViewModel(repository) as T
            }
        }
    }
}
```

For larger projects, consider using Hilt or Koin for dependency injection.

## Threading Model

| Operation | Thread |
|-----------|--------|
| Room queries (Flow) | Background (automatic) |
| Room writes | `Dispatchers.IO` via `suspend` |
| Ollama HTTP calls | `Dispatchers.IO` |
| UI updates | Main thread |

All coroutines are scoped to `viewModelScope` for automatic cancellation.

## Error Handling

Errors are captured in UI state classes:

```kotlin
data class QAUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null  // Displayed in UI
)
```

Network errors from Ollama are caught and surfaced to the user.

## Testing Strategy

| Layer | Testing Approach |
|-------|------------------|
| ViewModels | Unit tests with fake repositories |
| Repositories | Unit tests with in-memory Room database |
| DAOs | Instrumented tests |
| UI | Compose UI tests |
| Inference | Unit tests with mock HTTP responses |

## Key Files Reference

| File | Purpose |
|------|---------|
| [MainActivity.kt](../app/src/main/java/com/studentnotes/MainActivity.kt) | Entry point, sets up Compose |
| [StudentNotesApp.kt](../app/src/main/java/com/studentnotes/StudentNotesApp.kt) | Navigation graph |
| [StudentNotesApplication.kt](../app/src/main/java/com/studentnotes/StudentNotesApplication.kt) | Application class with database |
| [Note.kt](../app/src/main/java/com/studentnotes/data/local/Note.kt) | Note entity |
| [Flashcard.kt](../app/src/main/java/com/studentnotes/data/local/Flashcard.kt) | Flashcard entity |
| [StudentNotesDatabase.kt](../app/src/main/java/com/studentnotes/data/local/StudentNotesDatabase.kt) | Room database |
| [InferenceClient.kt](../app/src/main/java/com/studentnotes/inference/InferenceClient.kt) | LLM interface |
| [OllamaClient.kt](../app/src/main/java/com/studentnotes/inference/OllamaClient.kt) | Ollama implementation |

## Build Configuration

| File | Purpose |
|------|---------|
| [build.gradle.kts](../build.gradle.kts) | Root build configuration |
| [app/build.gradle.kts](../app/build.gradle.kts) | App module dependencies |
| [settings.gradle.kts](../settings.gradle.kts) | Project settings |
| [gradle.properties](../gradle.properties) | Gradle properties |

## Future Considerations

- **Dependency Injection**: Migrate to Hilt for better testability
- **Offline Support**: Queue inference requests when offline
- **Multiple LLM Backends**: Add support for cloud LLMs (OpenAI, Claude)
- **Sync**: Cloud backup/sync for notes
- **Search**: Full-text search across notes
- **Tags/Categories**: Organize notes with tags
- **Export**: Export notes to PDF/Markdown
