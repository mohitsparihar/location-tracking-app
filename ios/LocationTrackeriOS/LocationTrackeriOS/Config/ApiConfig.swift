import Foundation

enum ApiConfig {
    static let apiBaseURL = URL(string: "https://beapis-in.staging.geoiq.ai/retailapp/stg/v3/")!
    static let locationUploadURL = URL(string: "https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocation")!
    static let locationUploadBatchURL = URL(string: "https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocationBatch")!
    static let loginPath = "user/userlogin"

    // Force-update version check
    // Returns { "version": "1.0", "version_code": 1, "status": 200 }
    static let appVersionCheckURL = URL(string: "https://beapis-in.staging.geoiq.ai/retailapp/stg/v3/app/getAppConfig?app=location-tracker&platform=ios")!

    // App Store link — opened when user taps "Update Now" on the force update screen
    // Replace with a direct APK/IPA URL to distribute outside the App Store
    static let appStoreURL = URL(string: "https://apps.apple.com/app/id<YOUR_APP_ID>")!
}
