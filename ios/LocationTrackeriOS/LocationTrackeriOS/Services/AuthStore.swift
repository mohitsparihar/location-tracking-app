import Foundation

@MainActor
final class AuthStore: ObservableObject {
    @Published private(set) var token: String?
    @Published private(set) var userEmail: String?

    var isSignedIn: Bool {
        !(token?.isEmpty ?? true)
    }

    private let defaults: UserDefaults
    private let tokenKey = "auth_token"
    private let emailKey = "auth_email"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.token = defaults.string(forKey: tokenKey)
        self.userEmail = defaults.string(forKey: emailKey)
    }

    func setToken(_ value: String) {
        token = value
        defaults.set(value, forKey: tokenKey)
    }

    func setEmail(_ value: String) {
        userEmail = value
        defaults.set(value, forKey: emailKey)
    }

    func logout() {
        token = nil
        userEmail = nil
        defaults.removeObject(forKey: tokenKey)
        defaults.removeObject(forKey: emailKey)
    }
}
