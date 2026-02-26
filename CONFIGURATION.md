# Location Tracker — Configuration Guide

## Updating the Base URL

The API base URL is configured separately for each platform. **Both files must be updated** when changing environments (e.g. staging → production).

---

### Android

**File:** [`app/src/main/java/com/example/locationtracker/data/network/ApiConfig.kt`](file:///Users/mohitsinghparihar/Documents/location-app-android/app/src/main/java/com/example/locationtracker/data/network/ApiConfig.kt)

```kotlin
object ApiConfig {
    const val API_BASE_URL = "https://beapis-in.staging.geoiq.ai/retailapp/stg/v3/"

    const val LOCATION_UPLOAD_URL =
        "https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocation"
    const val LOCATION_UPLOAD_BATCH_URL =
        "https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocationBatch"
}
```

Update `API_BASE_URL`, `LOCATION_UPLOAD_URL`, and `LOCATION_UPLOAD_BATCH_URL` with the new domain/path. Then rebuild:

```bash
./scripts/build_app.sh && ./scripts/install_app.sh
```

---

### iOS

**File:** [`ios/LocationTrackeriOS/LocationTrackeriOS/Config/ApiConfig.swift`](file:///Users/mohitsinghparihar/Documents/location-app-android/ios/LocationTrackeriOS/LocationTrackeriOS/Config/ApiConfig.swift)

```swift
enum ApiConfig {
    static let apiBaseURL = URL(string: "https://beapis-in.staging.geoiq.ai/retailapp/stg/v3/")!
    static let locationUploadURL = URL(string: "https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocation")!
    static let locationUploadBatchURL = URL(string: "https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocationBatch")!
}
```

Update all three URLs, then rebuild:

```bash
xcodebuild \
  -project ios/LocationTrackeriOS/LocationTrackeriOS.xcodeproj \
  -scheme LocationTrackeriOS \
  -configuration Release \
  -destination "id=<DEVICE_ID>" \
  -allowProvisioningUpdates \
  CODE_SIGN_STYLE=Automatic \
  DEVELOPMENT_TEAM=<TEAM_ID>
```

---

### URL Endpoints Summary

| Endpoint | Path | Used For |
|----------|------|----------|
| **Login** | `{API_BASE_URL}user/userlogin` | User authentication |
| **Upload Location** | `{LOCATION_UPLOAD_URL}` | Single location point upload |
| **Upload Batch** | `{LOCATION_UPLOAD_BATCH_URL}` | Batch location upload (offline sync) |

> [!IMPORTANT]
> The login URL is derived from `API_BASE_URL` + `user/userlogin`. The location upload URLs are **separate full URLs** with a different path prefix (`/bdapp/` vs `/retailapp/`). Make sure to update all three when switching environments.

---

## iOS: xcodegen & Info.plist

This project uses [XcodeGen](https://github.com/yonaskolb/XcodeGen) to generate the `.xcodeproj`. The source of truth is `ios/LocationTrackeriOS/project.yml`.

> [!CAUTION]
> **Never edit `Info.plist` directly.** Running `xcodegen generate` regenerates `Info.plist` from `project.yml`, wiping any manual edits. Always add new Info.plist keys to `project.yml` → `info.properties` instead.

### Adding a new Info.plist key

Edit `project.yml` under `targets > LocationTrackeriOS > info > properties`:

```yaml
info:
  path: LocationTrackeriOS/Resources/Info.plist
  properties:
    # Existing keys...
    UIBackgroundModes: [location]
    UILaunchScreen: {}                    # ← Required for full-screen display
    UISupportedInterfaceOrientations:     # ← Required for orientation support
      - UIInterfaceOrientationPortrait
      - UIInterfaceOrientationLandscapeLeft
      - UIInterfaceOrientationLandscapeRight
    # Add new keys here ↓
    MyNewKey: "value"
```

Then regenerate:

```bash
cd ios/LocationTrackeriOS && xcodegen generate
```

### Key entries that must NOT be removed

| Key | Purpose |
|-----|---------|
| `UILaunchScreen: {}` | Tells iOS the app supports the full display. Without it, iOS shows **black bars** at the top and bottom (letterbox mode). |
| `UISupportedInterfaceOrientations` | Lists supported orientations. Required to avoid Xcode validation warnings. |
| `UIBackgroundModes: [location]` | Enables background location tracking. |

---

## Building Release Artifacts (APK / AAB / IPA)

### Android — Debug APK

Use the helper script for a quick debug build:

```bash
./scripts/build_app.sh
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

---

### Android — Release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

> [!NOTE]
> The release APK is **unsigned** by default. To sign it, either configure a `signingConfigs` block in `app/build.gradle.kts` or sign manually with `apksigner`:
>
> ```bash
> # Sign with your keystore
> apksigner sign \
>   --ks my-release-key.jks \
>   --ks-key-alias my-alias \
>   --out app-release-signed.apk \
>   app/build/outputs/apk/release/app-release-unsigned.apk
> ```

---

### Android — AAB (Android App Bundle)

AAB is required for Google Play Store uploads.

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

> [!IMPORTANT]
> The AAB must also be signed. Same signing config applies as for the release APK. Configure `signingConfigs` in `app/build.gradle.kts` to auto-sign:
>
> ```kotlin
> android {
>     signingConfigs {
>         create("release") {
>             storeFile = file("my-release-key.jks")
>             storePassword = "****"
>             keyAlias = "my-alias"
>             keyPassword = "****"
>         }
>     }
>     buildTypes {
>         release {
>             signingConfig = signingConfigs.getByName("release")
>         }
>     }
> }
> ```

---

### iOS — Install on Connected Device

Build and install directly:

```bash
xcodebuild \
  -project ios/LocationTrackeriOS/LocationTrackeriOS.xcodeproj \
  -scheme LocationTrackeriOS \
  -configuration Release \
  -destination "id=<DEVICE_ID>" \
  -allowProvisioningUpdates \
  CODE_SIGN_STYLE=Automatic \
  DEVELOPMENT_TEAM=<TEAM_ID>

xcrun devicectl device install app \
  --device <DEVICE_ID> \
  "$(find ~/Library/Developer/Xcode/DerivedData -name 'LocationTrackeriOS.app' -path '*/Release-iphoneos/*' 2>/dev/null)"
```

Replace `<DEVICE_ID>` with your device UUID and `<TEAM_ID>` with your Apple Developer Team ID.

> [!TIP]
> Find your device ID with: `xcrun devicectl list devices`

---

### iOS — IPA (for distribution)

**Step 1 — Archive:**

```bash
xcodebuild archive \
  -project ios/LocationTrackeriOS/LocationTrackeriOS.xcodeproj \
  -scheme LocationTrackeriOS \
  -configuration Release \
  -archivePath build/LocationTrackeriOS.xcarchive \
  -allowProvisioningUpdates \
  CODE_SIGN_STYLE=Automatic \
  DEVELOPMENT_TEAM=<TEAM_ID>
```

**Step 2 — Create export options plist:**

```bash
cat > build/ExportOptions.plist << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>ad-hoc</string>
    <key>teamID</key>
    <string>YOUR_TEAM_ID</string>
    <key>signingStyle</key>
    <string>automatic</string>
</dict>
</plist>
EOF
```

Change `method` to one of: `ad-hoc`, `app-store`, `development`, `enterprise`.

**Step 3 — Export IPA:**

```bash
xcodebuild -exportArchive \
  -archivePath build/LocationTrackeriOS.xcarchive \
  -exportPath build/ipa \
  -exportOptionsPlist build/ExportOptions.plist \
  -allowProvisioningUpdates
```

Output: `build/ipa/LocationTrackeriOS.ipa`

---

### Quick Reference

| Artifact | Command | Output Path |
|----------|---------|-------------|
| Debug APK | `./scripts/build_app.sh` | `app/build/outputs/apk/debug/app-debug.apk` |
| Release APK | `./gradlew assembleRelease` | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| AAB | `./gradlew bundleRelease` | `app/build/outputs/bundle/release/app-release.aab` |
| IPA | Archive → Export (see above) | `build/ipa/LocationTrackeriOS.ipa` |
