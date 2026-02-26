import Foundation

enum ApiConfig {
    static let apiBaseURL = URL(string: "https://beapis-in.staging.geoiq.ai/retailapp/stg/v3/")!
    static let locationUploadURL = URL(string: "https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocation")!
    static let locationUploadBatchURL = URL(string: "https://beapis-in.staging.geoiq.ai/bdapp/stg/v1/bd/locationTracking/updateUserLocationBatch")!
    static let loginPath = "user/userlogin"
}
