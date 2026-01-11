# Android SDK Setup Guide

This guide will help you set up the Android SDK and Android Studio to run this project.

## Quick Start

1. **Install Android Studio** (includes Android SDK)
   - Download from: https://developer.android.com/studio
   - Follow the installation wizard
   - The wizard will install Android SDK automatically

2. **Configure SDK Path**
   ```bash
   ./setup-android-sdk.sh
   ```

3. **Open in Android Studio**
   - File → Open → Select project folder
   - Wait for Gradle sync

4. **Run the App**
   - Click Run (▶) or press `Shift+F10`

## Detailed Setup

### Android Studio Installation

#### macOS
1. Download Android Studio from https://developer.android.com/studio
2. Open the `.dmg` file
3. Drag Android Studio to Applications
4. Launch Android Studio
5. Complete the setup wizard:
   - Choose "Standard" installation
   - Accept licenses
   - Wait for SDK components to download

#### Linux
1. Download from https://developer.android.com/studio
2. Extract the archive:
   ```bash
   tar -xzf android-studio-*.tar.gz
   ```
3. Run Android Studio:
   ```bash
   cd android-studio/bin
   ./studio.sh
   ```
4. Complete the setup wizard

#### Windows
1. Download the installer from https://developer.android.com/studio
2. Run the installer
3. Follow the installation wizard
4. Complete the setup wizard

### Required SDK Components

The project requires:
- **Android SDK Platform 34** (compileSdk = 34)
- **Android SDK Build-Tools 34.0.0** or later
- **Android SDK Platform-Tools** (for adb)
- **Android Emulator** (recommended for testing)

These are usually installed automatically by Android Studio, but you can verify/install them via:
- Android Studio → Tools → SDK Manager
- Or: `sdkmanager "platforms;android-34" "build-tools;34.0.0"`

### SDK Path Configuration

The SDK path is stored in `local.properties` (not committed to git).

**Automatic Detection:**
```bash
./setup-android-sdk.sh
```

**Manual Configuration:**
Edit `local.properties` and set:
```properties
sdk.dir=/path/to/your/android/sdk
```

Common locations:
- **macOS**: `~/Library/Android/sdk` or `/Users/YOUR_USERNAME/Library/Android/sdk`
- **Linux**: `~/Android/Sdk`
- **Windows**: `%LOCALAPPDATA%\Android\Sdk`

### Verify Installation

Check that everything is set up correctly:

```bash
# Check Android SDK
ls $ANDROID_HOME/platforms/android-34 2>/dev/null && echo "✅ SDK Platform 34 installed" || echo "❌ SDK Platform 34 missing"

# Check Build Tools
ls $ANDROID_HOME/build-tools/34.* 2>/dev/null && echo "✅ Build Tools installed" || echo "❌ Build Tools missing"

# Check Gradle wrapper
./gradlew --version && echo "✅ Gradle wrapper working" || echo "❌ Gradle wrapper issue"

# Try building
./gradlew tasks && echo "✅ Project configured correctly" || echo "❌ Project configuration issue"
```

## Troubleshooting

### "SDK location not found"
- Run `./setup-android-sdk.sh` to auto-detect
- Or manually set `sdk.dir` in `local.properties`

### "Gradle sync failed"
- Check internet connection (Gradle downloads dependencies)
- Verify JDK 17 is installed: `java -version`
- Try: File → Invalidate Caches / Restart

### "Build failed: SDK not found"
- Verify `local.properties` exists and has correct `sdk.dir`
- Check SDK is installed: Android Studio → Tools → SDK Manager
- Ensure Android SDK Platform 34 is installed

### "Emulator not starting"
- Install HAXM (Intel) or enable virtualization in BIOS
- For Apple Silicon Macs: Use ARM64 system images
- Check: Android Studio → Tools → SDK Manager → SDK Tools → Android Emulator

## Project Requirements

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Java Version**: 17
- **Gradle**: 8.13
- **Android Gradle Plugin**: 8.13.2
- **Kotlin**: 1.9.22

## Next Steps

After setting up Android SDK:
1. Install JDK 17 (see README.md)
2. Install Ollama (see README.md)
3. Pull the AI model: `ollama pull qwen2.5:0.5b`
4. Start Ollama: `ollama serve`
5. Run the app!
