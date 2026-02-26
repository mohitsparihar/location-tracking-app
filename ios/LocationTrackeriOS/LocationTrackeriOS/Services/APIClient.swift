import Foundation
import UIKit

enum APIClientError: LocalizedError {
    case invalidCredentials(String)
    case response(String)

    var errorDescription: String? {
        switch self {
        case .invalidCredentials(let message):
            return message
        case .response(let message):
            return message
        }
    }
}

enum UploadResult {
    case success(token: String?)
    case unauthorized
    case failure
}

struct UploadResponse: Decodable {
    let data: UploadResponseData?
}

struct UploadResponseData: Decodable {
    let token: String?
}

struct DeviceInfo {
    let deviceId: String
    let deviceName: String
    let deviceModel: String
    let deviceBrand: String
    let osName: String
    let osVersion: String
    let appVersion: String?

    static func current() -> DeviceInfo {
        let device = UIDevice.current
        let id = device.identifierForVendor?.uuidString ?? "unknown-ios-id"
        let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        return DeviceInfo(
            deviceId: id,
            deviceName: device.name,
            deviceModel: device.model,
            deviceBrand: "Apple",
            osName: "ios",
            osVersion: device.systemVersion,
            appVersion: appVersion
        )
    }
}

final class APIClient {
    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    func login(email: String, password: String) async throws -> (token: String, email: String?) {
        let endpoint = ApiConfig.apiBaseURL.appendingPathComponent(ApiConfig.loginPath)
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(LoginRequest(emailId: email, password: password))

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw APIClientError.response("Invalid response")
        }

        let body = try? JSONDecoder().decode(LoginResponse.self, from: data)
        if http.statusCode < 200 || http.statusCode >= 300 {
            let message = body?.resolvedData?.message ?? body?.message ?? "Login failed"
            throw APIClientError.response(message)
        }

        if body?.status == 401 {
            throw APIClientError.invalidCredentials(body?.resolvedData?.message ?? body?.message ?? "Invalid credentials")
        }

        guard let token = body?.resolvedData?.token, !token.isEmpty else {
            throw APIClientError.invalidCredentials(body?.resolvedData?.message ?? body?.message ?? "Invalid credentials")
        }

        return (token, body?.resolvedData?.email)
    }

    func uploadSingle(location: StoredLocation, token: String, deviceInfo: DeviceInfo) async throws -> UploadResult {
        let payload = map(location: location, deviceInfo: deviceInfo)
        return try await upload(
            url: ApiConfig.locationUploadURL,
            token: token,
            body: payload
        )
    }

    func uploadBatch(locations: [StoredLocation], token: String, deviceInfo: DeviceInfo) async throws -> UploadResult {
        let payload = UploadLocationBatchRequest(items: locations.map { map(location: $0, deviceInfo: deviceInfo) })
        return try await upload(
            url: ApiConfig.locationUploadBatchURL,
            token: token,
            body: payload
        )
    }

    private func upload<T: Encodable>(url: URL, token: String, body: T) async throws -> UploadResult {
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            return .failure
        }

        if [401, 403, 498].contains(http.statusCode) {
            return .unauthorized
        }

        if (200..<300).contains(http.statusCode) {
            let body = try? JSONDecoder().decode(UploadResponse.self, from: data)
            return .success(token: body?.data?.token)
        }
        return .failure
    }

    private func map(location: StoredLocation, deviceInfo: DeviceInfo) -> UploadLocationRequest {
        UploadLocationRequest(
            latitude: location.latitude,
            longitude: location.longitude,
            accuracy: location.accuracy,
            altitude: location.altitude,
            altitudeAccuracy: location.altitudeAccuracy,
            heading: location.heading,
            speed: location.speed,
            deviceId: deviceInfo.deviceId,
            deviceName: deviceInfo.deviceName,
            deviceModel: deviceInfo.deviceModel,
            deviceBrand: deviceInfo.deviceBrand,
            osName: deviceInfo.osName,
            osVersion: deviceInfo.osVersion,
            appVersion: deviceInfo.appVersion,
            timestamp: Self.formatUTC(millis: location.timestamp),
            clientTimestamp: location.timestamp,
            isBackground: location.isBackground
        )
    }

    private static func formatUTC(millis: Int64) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "MMMM d, yyyy 'at' h:mm:ss a 'UTC'"
        return formatter.string(from: Date(timeIntervalSince1970: TimeInterval(millis) / 1000))
    }
}
