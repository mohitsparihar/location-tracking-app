#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

VARIANT="${1:-debug}"
APK_PATH="app/build/outputs/apk/${VARIANT}/app-${VARIANT}.apk"

if command -v adb >/dev/null 2>&1; then
  ADB_CMD="$(command -v adb)"
elif [[ -x "${HOME}/Library/Android/sdk/platform-tools/adb" ]]; then
  ADB_CMD="${HOME}/Library/Android/sdk/platform-tools/adb"
else
  echo "Error: adb not found in PATH or default SDK location."
  exit 1
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found at: $APK_PATH"
  echo "Building first..."
  "${ROOT_DIR}/scripts/build_app.sh" "$VARIANT"
fi

DEVICE_COUNT="$("$ADB_CMD" devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')"
if [[ "$DEVICE_COUNT" -lt 1 ]]; then
  echo "Error: no connected Android device found in 'device' state."
  echo "Connect a device and enable USB debugging, then retry."
  exit 1
fi

echo "Installing: $APK_PATH"
"$ADB_CMD" install -r "$APK_PATH"

echo "Install successful."
