#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# ── Version args (optional) ────────────────────────────────────────────────────
# Usage: ./scripts/bundle_release.sh --version-code 2 --version-name 1.1.0
VERSION_CODE=""
VERSION_NAME=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --version-code) VERSION_CODE="$2"; shift 2 ;;
    --version-name) VERSION_NAME="$2"; shift 2 ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
done


if [[ -x "./gradlew" ]]; then
  GRADLE_CMD="./gradlew"
elif command -v gradle > /dev/null 2>&1; then
  GRADLE_CMD="gradle"
else
  CACHED_GRADLE="$(find "${HOME}/.gradle/wrapper/dists" -type f -path '*/bin/gradle' 2>/dev/null | sort | tail -n 1 || true)"
  if [[ -n "${CACHED_GRADLE}" && -x "${CACHED_GRADLE}" ]]; then
    GRADLE_CMD="${CACHED_GRADLE}"
  else
    echo "❌  Error: no Gradle executable found (./gradlew, gradle, or cached wrapper)."
    exit 1
  fi
fi

# ── Auto-generate local.properties if missing ─────────────────────────────────
if [[ ! -f "local.properties" ]]; then
  for SDK_DIR in "${ANDROID_SDK_ROOT:-}" "${ANDROID_HOME:-}" "${HOME}/Library/Android/sdk"; do
    if [[ -n "${SDK_DIR}" && -d "${SDK_DIR}" ]]; then
      printf 'sdk.dir=%s\n' "${SDK_DIR//:/\\:}" > local.properties
      echo "Created local.properties → sdk.dir=${SDK_DIR}"
      break
    fi
  done
fi

# ── Check keystore config ──────────────────────────────────────────────────────
if [[ ! -f "keystore.properties" ]]; then
  echo "❌  keystore.properties not found. Cannot sign release bundle."
  exit 1
fi

# ── Build ──────────────────────────────────────────────────────────────────────
GRADLE_ARGS="bundleRelease"
[[ -n "${VERSION_CODE}" ]] && GRADLE_ARGS="${GRADLE_ARGS} -PversionCode=${VERSION_CODE}"
[[ -n "${VERSION_NAME}" ]] && GRADLE_ARGS="${GRADLE_ARGS} -PversionName=${VERSION_NAME}"

echo "🔨  Building signed release AAB..."
echo "    Using Gradle : ${GRADLE_CMD}"
[[ -n "${VERSION_CODE}" ]] && echo "    Version Code : ${VERSION_CODE}"
[[ -n "${VERSION_NAME}" ]] && echo "    Version Name : ${VERSION_NAME}"
# shellcheck disable=SC2086
"${GRADLE_CMD}" ${GRADLE_ARGS}

AAB_PATH="app/build/outputs/bundle/release/app-release.aab"

if [[ -f "${AAB_PATH}" ]]; then
  AAB_SIZE="$(du -sh "${AAB_PATH}" | cut -f1)"
  echo ""
  echo "✅  Release AAB built successfully!"
  echo "    📦 File   : ${ROOT_DIR}/${AAB_PATH}"
  echo "    📐 Size   : ${AAB_SIZE}"
  echo ""
  echo "Upload this .aab file to Google Play Console → Production (or Internal Testing)."
else
  echo "❌  Build succeeded but AAB not found at ${AAB_PATH}"
  exit 1
fi
