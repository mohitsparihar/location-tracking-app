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
    let user: LoginUser?
    let message: String?
    let status: Int?
}

struct LoginUser: Decodable {
    let token: String?
    let isOnboardingCompleted: Bool?

    enum CodingKeys: String, CodingKey {
        case token
        case isOnboardingCompleted = "is_onboarding_completed"
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
