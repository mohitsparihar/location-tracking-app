import Foundation

@MainActor
final class AuthStore: ObservableObject {
    @Published private(set) var token: String?
    @Published private(set) var userEmail: String?
    @Published private(set) var isShiftActive: Bool = false

    var isSignedIn: Bool {
        !(token?.isEmpty ?? true)
    }

    private let defaults: UserDefaults
    private let tokenKey = "auth_token"
    private let emailKey = "auth_email"
    private let shiftActiveKey = "is_shift_active"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.token = defaults.string(forKey: tokenKey)
        self.userEmail = defaults.string(forKey: emailKey)
        self.isShiftActive = defaults.bool(forKey: shiftActiveKey)
    }

    func setToken(_ value: String) {
        token = value
        defaults.set(value, forKey: tokenKey)
    }

    func setEmail(_ value: String) {
        userEmail = value
        defaults.set(value, forKey: emailKey)
    }

    func setShiftActive(_ value: Bool) {
        isShiftActive = value
        defaults.set(value, forKey: shiftActiveKey)
    }

    func logout() {
        token = nil
        userEmail = nil
        isShiftActive = false
        defaults.removeObject(forKey: tokenKey)
        defaults.removeObject(forKey: emailKey)
        defaults.removeObject(forKey: shiftActiveKey)
    }
}
