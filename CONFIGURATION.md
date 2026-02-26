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
