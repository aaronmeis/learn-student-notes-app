#!/bin/bash

# Script to detect and configure Android SDK path for this project

echo "🔍 Detecting Android SDK location..."

# Try common locations
SDK_PATHS=(
    "$HOME/Library/Android/sdk"
    "/Users/$USER/Library/Android/sdk"
    "$HOME/Android/Sdk"
    "$ANDROID_HOME"
    "$ANDROID_SDK_ROOT"
)

SDK_DIR=""

for path in "${SDK_PATHS[@]}"; do
    if [ -n "$path" ] && [ -d "$path" ] && [ -f "$path/platform-tools/adb" ]; then
        SDK_DIR="$path"
        echo "✅ Found Android SDK at: $SDK_DIR"
        break
    fi
done

if [ -z "$SDK_DIR" ]; then
    echo "❌ Android SDK not found in common locations."
    echo ""
    echo "Please install Android Studio first:"
    echo "  macOS: Download from https://developer.android.com/studio"
    echo "  Linux: Follow instructions at https://developer.android.com/studio"
    echo "  Windows: Download installer from https://developer.android.com/studio"
    echo ""
    echo "After installing Android Studio, run this script again, or manually"
    echo "update local.properties with your SDK path."
    exit 1
fi

# Update local.properties
LOCAL_PROPERTIES="local.properties"

if [ -f "$LOCAL_PROPERTIES" ]; then
    # Check if SDK path is already set correctly
    CURRENT_SDK=$(grep "^sdk.dir=" "$LOCAL_PROPERTIES" | cut -d'=' -f2)
    if [ "$CURRENT_SDK" = "$SDK_DIR" ]; then
        echo "✅ local.properties already configured correctly"
        exit 0
    fi
    
    # Update existing file
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s|^sdk.dir=.*|sdk.dir=$SDK_DIR|" "$LOCAL_PROPERTIES"
    else
        # Linux
        sed -i "s|^sdk.dir=.*|sdk.dir=$SDK_DIR|" "$LOCAL_PROPERTIES"
    fi
    echo "✅ Updated local.properties with SDK path: $SDK_DIR"
else
    # Create new file
    echo "sdk.dir=$SDK_DIR" > "$LOCAL_PROPERTIES"
    echo "✅ Created local.properties with SDK path: $SDK_DIR"
fi

echo ""
echo "🎉 Android SDK configuration complete!"
echo "You can now build the project with: ./gradlew build"
