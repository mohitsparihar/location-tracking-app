import Foundation

struct LoginRequest: Encodable {
    let emailId: String
    let password: String

    enum CodingKeys: String, CodingKey {
        case emailId = "email_id"
        case password
    }
}

struct LoginResponse: Decodable {
    let loginData: LoginUser?   // new API: keyed "in"
    let user: LoginUser?        // legacy API: keyed "user"
    let message: String?
    let status: Int?

    enum CodingKeys: String, CodingKey {
        case loginData = "in"
        case user
        case message
        case status
    }

    /// Returns whichever payload the server sent.
    var resolvedData: LoginUser? { loginData ?? user }
}

struct LoginUser: Decodable {
    let token: String?
    let email: String?
    let isOnboardingCompleted: Bool?
    let message: String?

    enum CodingKeys: String, CodingKey {
        case token
        case email
        case isOnboardingCompleted = "is_onboarding_completed"
        case message
    }
}

struct UploadLocationRequest: Encodable {
    let latitude: Double
    let longitude: Double
    let accuracy: Float?
    let altitude: Double?
    let altitudeAccuracy: Float?
    let heading: Float?
    let speed: Float?
    let deviceId: String
    let deviceName: String?
    let deviceModel: String?
    let deviceBrand: String?
    let osName: String
    let osVersion: String
    let appVersion: String?
    let timestamp: String
    let clientTimestamp: Int64
    let isBackground: Bool
}

struct UploadLocationBatchRequest: Encodable {
    let items: [UploadLocationRequest]
}
