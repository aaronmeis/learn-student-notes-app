# Database Schema

This document describes the Room database schema for the Student Notes app.

## Overview

The app uses Room (SQLite) for local persistence with two main entities:

```
┌─────────────────────────────────────────────────────────────┐
│                   student_notes_database                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────┐         ┌─────────────────────────┐    │
│  │     notes       │         │      flashcards         │    │
│  ├─────────────────┤         ├─────────────────────────┤    │
│  │ id (PK)         │◄────────│ noteId (FK)             │    │
│  │ title           │    1:N  │ id (PK)                 │    │
│  │ content         │         │ question                │    │
│  │ createdAt       │         │ answer                  │    │
│  │ updatedAt       │         │ createdAt               │    │
│  └─────────────────┘         └─────────────────────────────┘│
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Entities

### Note

Stores user-created study notes.

**Table**: `notes`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `INTEGER` | PRIMARY KEY, AUTOINCREMENT | Unique identifier |
| `title` | `TEXT` | NOT NULL | Note title |
| `content` | `TEXT` | NOT NULL | Note body/content |
| `createdAt` | `INTEGER` | NOT NULL | Unix timestamp (ms) when created |
| `updatedAt` | `INTEGER` | NOT NULL | Unix timestamp (ms) when last modified |

**Entity Definition**: [Note.kt](../app/src/main/java/com/studentnotes/data/local/Note.kt)

```kotlin
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### Flashcard

Stores AI-generated flashcards linked to notes.

**Table**: `flashcards`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `INTEGER` | PRIMARY KEY, AUTOINCREMENT | Unique identifier |
| `noteId` | `INTEGER` | FOREIGN KEY → notes.id, NOT NULL | Parent note reference |
| `question` | `TEXT` | NOT NULL | Flashcard question |
| `answer` | `TEXT` | NOT NULL | Flashcard answer |
| `createdAt` | `INTEGER` | NOT NULL | Unix timestamp (ms) when generated |

**Entity Definition**: [Flashcard.kt](../app/src/main/java/com/studentnotes/data/local/Flashcard.kt)

```kotlin
@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class Flashcard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val question: String,
    val answer: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

## Relationships

### Note → Flashcard (One-to-Many)

- One note can have many flashcards
- Deleting a note cascades to delete all its flashcards (`onDelete = CASCADE`)
- The `noteId` column is indexed for efficient queries

```
Note (id=1, title="Biology Chapter 1")
  │
  ├── Flashcard (id=1, noteId=1, question="What is mitosis?")
  ├── Flashcard (id=2, noteId=1, question="What is meiosis?")
  └── Flashcard (id=3, noteId=1, question="What are chromosomes?")
```

## Data Access Objects (DAOs)

### NoteDao

**File**: [NoteDao.kt](../app/src/main/java/com/studentnotes/data/local/NoteDao.kt)

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAllNotes()` | `Flow<List<Note>>` | Get all notes, sorted by `updatedAt` DESC |
| `getNoteById(id)` | `Flow<Note?>` | Get single note (reactive) |
| `getNoteByIdOnce(id)` | `suspend Note?` | Get single note (one-shot) |
| `insertNote(note)` | `suspend Long` | Insert note, returns new ID |
| `updateNote(note)` | `suspend Unit` | Update existing note |
| `deleteNote(note)` | `suspend Unit` | Delete note by entity |
| `deleteNoteById(id)` | `suspend Unit` | Delete note by ID |
| `searchNotes(query)` | `Flow<List<Note>>` | Search notes by title or content |

**Query Examples**:

```kotlin
// Get all notes sorted by most recently updated
@Query("SELECT * FROM notes ORDER BY updatedAt DESC")
fun getAllNotes(): Flow<List<Note>>

// Search notes (case-insensitive partial match)
@Query("""
    SELECT * FROM notes
    WHERE title LIKE '%' || :query || '%'
       OR content LIKE '%' || :query || '%'
    ORDER BY updatedAt DESC
""")
fun searchNotes(query: String): Flow<List<Note>>
```

### FlashcardDao

**File**: [FlashcardDao.kt](../app/src/main/java/com/studentnotes/data/local/FlashcardDao.kt)

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getFlashcardsByNoteId(noteId)` | `Flow<List<Flashcard>>` | Get flashcards for a specific note |
| `getAllFlashcards()` | `Flow<List<Flashcard>>` | Get all flashcards across all notes |
| `insertFlashcard(flashcard)` | `suspend Long` | Insert single flashcard |
| `insertFlashcards(flashcards)` | `suspend Unit` | Batch insert flashcards |
| `deleteFlashcard(flashcard)` | `suspend Unit` | Delete single flashcard |
| `deleteFlashcardsByNoteId(noteId)` | `suspend Unit` | Delete all flashcards for a note |

## Database Instance

**File**: [StudentNotesDatabase.kt](../app/src/main/java/com/studentnotes/data/local/StudentNotesDatabase.kt)

```kotlin
@Database(
    entities = [Note::class, Flashcard::class],
    version = 1,
    exportSchema = false
)
abstract class StudentNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun flashcardDao(): FlashcardDao

    companion object {
        @Volatile
        private var INSTANCE: StudentNotesDatabase? = null

        fun getDatabase(context: Context): StudentNotesDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    StudentNotesDatabase::class.java,
                    "student_notes_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

## Usage Patterns

### Reactive Queries (Flow)

Use `Flow` for data that should update the UI automatically:

```kotlin
// In ViewModel
val notes: StateFlow<List<Note>> = repository.getAllNotes()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// In Composable
val notes by viewModel.notes.collectAsState()
```

### One-Shot Queries (Suspend)

Use `suspend` functions for single reads before operations:

```kotlin
// Get note content before sending to LLM
val note = repository.getNoteByIdOnce(noteId)
val flashcards = inferenceClient.generateFlashcards(note.content)
```

### Transactions

For operations that need atomicity:

```kotlin
// Example: Move flashcards between notes (not currently implemented)
@Transaction
suspend fun moveFlashcards(fromNoteId: Long, toNoteId: Long) {
    val flashcards = getFlashcardsByNoteIdOnce(fromNoteId)
    flashcards.forEach {
        updateFlashcard(it.copy(noteId = toNoteId))
    }
}
```

## Migration Strategy

Currently at version 1. Future migrations should:

1. Increment the version number
2. Add a `Migration` object
3. Preserve user data

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT DEFAULT ''")
    }
}

Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2)
    .build()
```

## Performance Considerations

### Indexing

- `flashcards.noteId` is indexed for fast lookups by note
- Consider adding index on `notes.updatedAt` if list performance degrades

### Query Optimization

- Use `Flow` with `WhileSubscribed(5000)` to stop observing when UI is not visible
- Use `getNoteByIdOnce()` instead of `getNoteById().first()` for one-shot reads
- Batch insert flashcards with `insertFlashcards(list)` instead of individual inserts

### Memory

- Room automatically handles cursor management
- Large note content is loaded on-demand (no eager loading of relationships)

## Debugging

Enable Room query logging in debug builds:

```kotlin
Room.databaseBuilder(...)
    .setQueryCallback({ sqlQuery, bindArgs ->
        Log.d("Room", "Query: $sqlQuery Args: $bindArgs")
    }, Executors.newSingleThreadExecutor())
    .build()
```

View database with Android Studio's Database Inspector:
**View → Tool Windows → App Inspection → Database Inspector**

## Quick Reference

### Creating a Note

```kotlin
val note = Note(
    title = "Biology Chapter 1",
    content = "Mitosis is the process of cell division..."
)
val noteId = noteRepository.insertNote(note)
```

### Updating a Note

```kotlin
val existingNote = noteRepository.getNoteByIdOnce(noteId)
existingNote?.let {
    noteRepository.updateNote(it.copy(
        title = "Updated Title",
        updatedAt = System.currentTimeMillis()
    ))
}
```

### Generating and Saving Flashcards

```kotlin
val note = noteRepository.getNoteByIdOnce(noteId)
val results = inferenceClient.generateFlashcards(note.content)
val flashcards = results.map {
    Flashcard(noteId = noteId, question = it.question, answer = it.answer)
}
flashcardRepository.insertFlashcards(flashcards)
```

### Deleting a Note (cascades to flashcards)

```kotlin
noteRepository.deleteNoteById(noteId)
// All associated flashcards are automatically deleted
```

## Related Documentation

- [Architecture](ARCHITECTURE.md) - Overall app architecture
- [InferenceClient.kt](../app/src/main/java/com/studentnotes/inference/InferenceClient.kt) - LLM interface
- [NoteRepository.kt](../app/src/main/java/com/studentnotes/data/repository/NoteRepository.kt) - Repository layer
