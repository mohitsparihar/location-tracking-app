# LocationTrackeriOS

SwiftUI iOS counterpart for the Android `locationtracker` app.

## What it mirrors

- Login with `email_id` + `password` against `user/userlogin`
- Persist auth token locally
- Capture location points and store them locally
- Upload pending points to single or batch endpoints
- Logout automatically on `401`, `403`, or `498` from upload APIs
- UI with login screen, permission-blocking screen, and recorded locations list

## Folder layout

- `LocationTrackeriOS/`: App source code
- `project.yml`: XcodeGen project spec

## Run

1. Install XcodeGen (`brew install xcodegen`) if not already installed.
2. From `ios/LocationTrackeriOS`, run:
   - `xcodegen generate`
3. Open `LocationTrackeriOS.xcodeproj` in Xcode.
4. Select a signing team.
5. Run on a real device for background location behavior.

## iOS notes

- Background location requires "Always" location authorization.
- `UIBackgroundModes` includes `location` in `Info.plist`.
- For App Store builds, update permission description strings to your compliance text.
