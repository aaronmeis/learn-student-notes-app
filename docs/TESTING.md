# Testing Guide

This document covers the testing setup, test structure, and how to run tests for the Student Notes App.

## Test Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| JUnit 4 | 4.13.2 | Test framework |
| MockK | 1.13.9 | Kotlin mocking library |
| Kotlinx Coroutines Test | 1.7.3 | Coroutine testing utilities |
| Turbine | 1.0.0 | Flow testing |
| Room Testing | 2.6.1 | In-memory database |
| Robolectric | 4.11.1 | Run Android tests on JVM |
| AndroidX Test Core | 1.5.0 | Application context for tests |
| Arch Core Testing | 2.2.0 | Architecture component testing |

## Test Structure

```
app/src/test/java/com/studentnotes/
├── data/
│   ├── local/
│   │   └── FakeNoteDao.kt          # In-memory DAO implementation
│   └── repository/
│       └── NoteRepositoryTest.kt   # Repository tests with Room
├── features/
│   └── summarize/
│       └── SummarizeViewModelTest.kt  # ViewModel unit tests
└── inference/
    └── FakeInferenceClient.kt      # Test double for AI client
```

## Test Types

### Unit Tests (JVM)

Located in `app/src/test/`. These tests run on the JVM without an Android emulator.

**SummarizeViewModelTest** - Tests the summarize feature ViewModel:
- Initial state verification
- Title and content updates
- Empty text validation
- Loading state during summarization
- Success state with AI response
- Error handling on failure
- Note saving with/without summary
- Auto-generated title from content
- State clearing

**NoteRepositoryTest** - Tests database operations via Robolectric:
- Insert and retrieve notes
- Notes sorted by updatedAt
- Get note by ID (Flow and suspend)
- Update existing notes
- Delete notes (by entity and by ID)
- Search by title and content
- Case-insensitive search
- Summary persistence
- Flow updates on data changes

### Test Doubles

**FakeInferenceClient** - Configurable test implementation of `InferenceClient`:
```kotlin
val fakeClient = FakeInferenceClient()

// Configure responses
fakeClient.summarizeResponse = "Custom summary"
fakeClient.shouldThrowError = true
fakeClient.errorMessage = "Network error"

// Check interactions
assertEquals(1, fakeClient.summarizeCallCount)
assertEquals("input text", fakeClient.lastSummarizeInput)
```

**FakeNoteDao** - In-memory implementation of `NoteDao`:
```kotlin
val fakeDao = FakeNoteDao()

// Use like real DAO
fakeDao.insertNote(note)
fakeDao.getAllNotes().test { ... }

// Reset between tests
fakeDao.clear()
```

## Running Tests

### Command Line

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.studentnotes.features.summarize.SummarizeViewModelTest"

# Run specific test method
./gradlew test --tests "com.studentnotes.features.summarize.SummarizeViewModelTest.summarize shows loading state then success"

# Run with verbose output
./gradlew test --info

# Run and generate HTML report
./gradlew test
# Report: app/build/reports/tests/testDebugUnitTest/index.html
```

### Android Studio

1. Right-click on `app/src/test/java` folder
2. Select "Run 'Tests in 'studentnotes''"

Or run individual test classes/methods:
1. Open a test file
2. Click the green play button next to the class or method
3. Select "Run"

## Writing New Tests

### ViewModel Tests

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test description`() = runTest {
        val viewModel = MyViewModel(fakeDependency)

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.doSomething()

            // Advance coroutines
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(expected, state.value)
        }
    }
}
```

### Repository Tests with Room

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MyRepositoryTest {

    private lateinit var database: StudentNotesDatabase
    private lateinit var repository: MyRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StudentNotesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MyRepository(database.myDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test database operation`() = runTest {
        repository.insert(item)

        repository.getAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

## Testing Flows with Turbine

Turbine simplifies testing Kotlin Flows:

```kotlin
repository.getAllNotes().test {
    // Wait for emission
    val items = awaitItem()

    // Assert
    assertEquals(2, items.size)

    // Cancel collection
    cancelAndIgnoreRemainingEvents()
}
```

For Flows that emit multiple times:

```kotlin
viewModel.uiState.test {
    assertEquals(InitialState, awaitItem())

    viewModel.loadData()
    assertEquals(LoadingState, awaitItem())

    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(SuccessState, awaitItem())
}
```

## Best Practices

1. **Use test dispatchers** - Replace `Dispatchers.Main` with `StandardTestDispatcher` for predictable coroutine execution

2. **Isolate tests** - Each test should be independent; use `@Before` and `@After` for setup/cleanup

3. **Test behavior, not implementation** - Focus on what the code does, not how it does it

4. **Use descriptive test names** - Use backtick syntax for readable names: `` `summarize with empty text shows error` ``

5. **Prefer fakes over mocks** - Fakes (like `FakeNoteDao`) are simpler and less brittle than mocks

6. **Test edge cases** - Empty inputs, null values, error conditions

7. **Keep tests fast** - Use in-memory databases and avoid real network calls
