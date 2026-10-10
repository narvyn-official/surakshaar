#!/usr/bin/env bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -z "${JAVA_HOME:-}" && -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi
if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi
cd "$PROJECT_ROOT/apps/android"
./gradlew testDebugUnitTest assembleDebug "$@"
APK_VERSION="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts)"
if [[ ! "$APK_VERSION" =~ ^[0-9][A-Za-z0-9._-]*$ ]]; then
  echo "Could not read a safe Android versionName" >&2
  exit 1
fi
ARTIFACT_DIR="$PROJECT_ROOT/artifacts/surakshaar-${APK_VERSION%-dev}"
mkdir -p "$ARTIFACT_DIR"
APK_PATH="$ARTIFACT_DIR/surakshaar-$APK_VERSION-debug.apk"
cp app/build/outputs/apk/debug/app-debug.apk "$APK_PATH"
shasum -a 256 "$APK_PATH"
