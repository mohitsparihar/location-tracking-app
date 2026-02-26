#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

VARIANT="${1:-debug}"
CAP_VARIANT="$(printf '%s' "$VARIANT" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')"
TASK="assemble${CAP_VARIANT}"

if [[ ! -f "local.properties" ]]; then
  if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
    SDK_DIR="${ANDROID_SDK_ROOT}"
  elif [[ -n "${ANDROID_HOME:-}" ]]; then
    SDK_DIR="${ANDROID_HOME}"
  elif [[ -d "${HOME}/Library/Android/sdk" ]]; then
    SDK_DIR="${HOME}/Library/Android/sdk"
  else
    SDK_DIR=""
  fi

  if [[ -n "${SDK_DIR}" ]]; then
    printf 'sdk.dir=%s\n' "${SDK_DIR//:/\\:}" > local.properties
    echo "Created local.properties with sdk.dir=${SDK_DIR}"
  fi
fi

if [[ ! -x "./gradlew" ]]; then
  if ! command -v gradle >/dev/null 2>&1; then
    CACHED_GRADLE="$(find "${HOME}/.gradle/wrapper/dists" -type f -path '*/bin/gradle' 2>/dev/null | sort | tail -n 1 || true)"
    if [[ -n "${CACHED_GRADLE}" && -x "${CACHED_GRADLE}" ]]; then
      GRADLE_CMD="${CACHED_GRADLE}"
    else
      echo "Error: no Gradle executable found (./gradlew, gradle, or cached wrapper gradle)."
      exit 1
    fi
  else
    GRADLE_CMD="gradle"
  fi
else
  GRADLE_CMD="./gradlew"
fi

echo "Building app with task: $TASK"
echo "Using Gradle: $GRADLE_CMD"
"$GRADLE_CMD" "$TASK"

echo "Build completed for variant: $VARIANT"
