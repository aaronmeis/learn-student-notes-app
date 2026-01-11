# Student Notes App

An Android app for taking notes with AI-powered features using local LLM inference via Ollama.

level: beginner | proves: edge ai + resource optimization
the challenge
build an offline-first mobile app using small language models. zero api costs. complete privacy. this teaches you how to optimize models for restricted hardware.
key architectural decisions :
model management: lazy loading models on-demand to preserve memory. unload inactive models when memory pressure is detected. preload frequently used models during idle time.
context window: implement sliding window with semantic chunking. keep the most relevant context, drop the oldest. use embedding similarity to determine what stays in the window versus what gets archived.
quantization strategy: dynamic quantization based on device capabilities. 4-bit quantization for older devices (pre-2020), 8-bit for newer devices. detect available ram and adjust accordingly.
battery optimization: batch inference requests to reduce wake cycles. throttle model calls during low battery mode. defer non-critical processing until charging.
offline-first sync: store user data locally in encrypted format. sync to cloud only when connected and with user permission. conflict resolution prioritizes local changes.

![overview](./overview.png)


## Feature Status

| Feature | Status | Description |
|---------|--------|-------------|
| Notes List | Working | View, delete saved notes |
| Create Note | Working | Add title and content |
| AI Summarization | Working | Generate summaries via Ollama |
| Save Notes | Working | Persist to Room database |
| Note Detail/Edit | Placeholder | Coming soon |
| Flashcards | Placeholder | Coming soon |
| Q&A | Placeholder | Coming soon |

## Screenshots

```
┌─────────────────┐  ┌─────────────────┐
│  Notes List     │  │  New Note       │
│                 │  │                 │
│  ┌───────────┐  │  │  Title: ______  │
│  │ Biology   │  │  │                 │
│  │ Chapter 1 │  │  │  Content:       │
│  │ Summary...|🗑│  │  ____________   │
│  └───────────┘  │  │  ____________   │
│  ┌───────────┐  │  │                 │
│  │ Math      │  │  │  [Summarize]    │
│  │ Notes     │  │  │                 │
│  └───────────┘  │  │  Summary: ...   │
│                 │  │                 │
│           [+]   │  │  [Save] [Back]  │
└─────────────────┘  └─────────────────┘
```

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Database | Room |
| Async | Coroutines & Flow |
| Networking | Ktor Client |
| LLM | Ollama (local inference) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

## Documentation

- [Architecture](docs/ARCHITECTURE.md) - MVVM architecture, layers, data flow, and design decisions
- [Database](docs/DATABASE.md) - Room schema, entities, DAOs, and query patterns

## Project Structure

```
student-notes-app/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/studentnotes/
│           ├── MainActivity.kt
│           ├── StudentNotesApp.kt
│           ├── StudentNotesApplication.kt
│           ├── data/
│           │   ├── local/          # Room entities, DAOs, database
│           │   └── repository/     # Data repositories
│           ├── inference/          # LLM client interface & Ollama
│           ├── features/
│           │   ├── notes/          # Notes list and detail screens
│           │   ├── flashcards/     # Flashcard viewer
│           │   └── qa/             # Q&A chat interface
│           └── ui/theme/           # Compose theme
├── docs/
│   ├── ARCHITECTURE.md
│   └── DATABASE.md
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Ollama installed on your machine

## Setup (New Machine)

### 1. Install Java 17

**macOS:**
```bash
brew install openjdk@17
```

Add to your shell profile (`~/.zshrc` or `~/.bash_profile`):
```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export PATH="$JAVA_HOME/bin:$PATH"
```

Then reload:
```bash
source ~/.zshrc
```

Verify installation:
```bash
java -version
# Should show: openjdk version "17.x.x"
```

**Linux:**
```bash
sudo apt install openjdk-17-jdk
```

**Windows:**
Download from [adoptium.net](https://adoptium.net/) and run the installer.

### 2. Install Ollama

**macOS:**
```bash
brew install ollama
```

**Linux:**
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

**Windows:**
Download from [ollama.com/download](https://ollama.com/download)

### 3. Pull the AI Model

```bash
# Start Ollama service (runs in background)
ollama serve

# In a new terminal, pull the model (~400MB)
ollama pull qwen2.5:0.5b
```

Verify it's working:
```bash
ollama run qwen2.5:0.5b "Hello"
```

### 4. Clone and Open the Project

```bash
git clone <repository-url>
cd student-notes-app
```

Open the project in Android Studio:
- File → Open → Select the `student-notes-app` folder
- Wait for Gradle sync to complete

### 5. Run the App

**Option A: Android Studio (Recommended)**
1. Ensure Ollama is running (`ollama serve`)
2. Select an emulator or connected device
3. Click the Run button (▶) or press `Shift+F10`

**Option B: Command Line**
```bash
# Make sure Ollama is running first
ollama serve &

# Build and install on connected device/emulator
./gradlew installDebug

# Or just build the APK
./gradlew assembleDebug
# APK location: app/build/outputs/apk/debug/app-debug.apk
```

## Using the App

1. **Create a Note**: Tap the + button on the Notes List screen
2. **Add Content**: Enter a title and your note content
3. **Generate Summary**: Tap "Summarize" to get an AI-generated summary
4. **Save**: Tap the save icon to persist the note
5. **View Notes**: Notes appear on the main list with their summaries

## Ollama Configuration

The app connects to Ollama at `http://10.0.2.2:11434` (Android emulator's localhost alias).

| Device Type | Base URL |
|-------------|----------|
| Android Emulator | `http://10.0.2.2:11434` (default) |
| Physical Device | `http://<your-machine-ip>:11434` |

To change the URL or model, edit [OllamaClient.kt](app/src/main/java/com/studentnotes/inference/OllamaClient.kt):

```kotlin
class OllamaClient(
    private val baseUrl: String = "http://10.0.2.2:11434",
    private val model: String = "qwen2.5:0.5b"
) : InferenceClient
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Connection refused" error | Ensure `ollama serve` is running |
| Slow response | First request loads the model; subsequent requests are faster |
| App crashes on launch | Check Logcat for Room migration issues; uninstall and reinstall |
| Emulator can't reach Ollama | Verify Ollama is on port 11434 and firewall allows connections |

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Clean build
./gradlew clean assembleDebug
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

MIT
