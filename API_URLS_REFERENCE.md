# API URLs Reference — TrackIQ Android App

All API endpoints are centralised in **one file**. In most cases you only need to change that one file.

---

## Primary Config File

### [`ApiConfig.kt`](app/src/main/java/com/example/locationtracker/data/network/ApiConfig.kt)

This is the **single source of truth** for all API URLs. Change URLs here first.

| Constant | Current Value | Purpose |
|---|---|---|
| `API_BASE_URL` | `https://beapis-in.staging.geoiq.ai/retailapp/stg/v3/` | Base URL — all relative endpoints build off this |
| `LOGIN_URL` | `{API_BASE_URL}user/userlogin` | User login |
| `LOCATION_UPLOAD_URL` | `https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocation` | Single location upload |
| `LOCATION_UPLOAD_BATCH_URL` | `https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocationBatch` | Batch location upload |
| `APP_VERSION_CHECK_URL` | `{API_BASE_URL}app/getAppConfig?app=location-tracker&platform=android` | Force-update version check |

> **Note:** `LOCATION_UPLOAD_URL` and `LOCATION_UPLOAD_BATCH_URL` point to a different subdomain (`bdapp`) than the base URL (`retailapp`), so they are defined as full absolute URLs — not relative to `API_BASE_URL`.

---

## Switching Environments (Staging → Production)

To point the app at production, update **three** constants in `ApiConfig.kt`:

```kotlin
// 1. Base URL
const val API_BASE_URL = "https://beapis-in.geoiq.ai/retailapp/v3/"

// 2. Location upload (single)
const val LOCATION_UPLOAD_URL =
    "https://beapis-in.geoiq.ai/bdapp/v1/bd/locationTracking/updateUserLocation"

// 3. Location upload (batch)
const val LOCATION_UPLOAD_BATCH_URL =
    "https://beapis-in.geoiq.ai/bdapp/v1/bd/locationTracking/updateUserLocationBatch"
```

`LOGIN_URL` and `APP_VERSION_CHECK_URL` will automatically resolve correctly because they derive from `API_BASE_URL`.

---

## Play Store / APK Download URL (Force Update Screen)

This is the URL that opens when the user taps **"Update Now"** on the force update screen.

**File:** [`AppView.kt`](app/src/main/java/com/example/locationtracker/ui/AppView.kt)  
**Function:** `ForceUpdateScreen()`  
**Line to change:**

```kotlin
val playStoreUrl = "https://play.google.com/store/apps/details?id=com.geoiq.trackiq"
```

Replace the value with a Direct APK link if you want to distribute outside the Play Store:
```kotlin
val playStoreUrl = "https://yourdomain.com/downloads/trackiq-latest.apk"
```

---

## How the Version Check Works

1. On app launch, the app calls `APP_VERSION_CHECK_URL`
2. The API returns `{ "version": "x.y.z", "version_code": N }`
3. The app compares the server `version` against the installed `versionName` (set in [`build.gradle.kts`](app/build.gradle.kts))
4. If the installed version is **older** than the server version → force update screen is shown
5. If versions are equal or installed is newer → app proceeds normally

To trigger a force update, set `version` in the backend to a value **higher** than the current `versionName` in `build.gradle.kts`:
```kotlin
// app/build.gradle.kts
versionName = (findProperty("versionName") as String?) ?: "1.0"
```
