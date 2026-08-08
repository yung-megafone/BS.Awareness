#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
VERSION=8.13
DIST="$ROOT/.gradle-dist"
HOME_DIR="$DIST/gradle-$VERSION"
ZIP="$DIST/gradle-$VERSION-bin.zip"
mkdir -p "$DIST"
if [ ! -x "$HOME_DIR/bin/gradle" ]; then
  curl -L "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP"
  unzip -q -o "$ZIP" -d "$DIST"
fi
cd "$ROOT"
"$HOME_DIR/bin/gradle" :app:assembleDebug
printf '\nAPK: %s\n' "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
