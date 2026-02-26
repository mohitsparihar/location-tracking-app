import Foundation

@MainActor
final class AuthStore: ObservableObject {
    @Published private(set) var token: String?

    var isSignedIn: Bool {
        !(token?.isEmpty ?? true)
    }

    private let defaults: UserDefaults
    private let tokenKey = "auth_token"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.token = defaults.string(forKey: tokenKey)
    }

    func setToken(_ value: String) {
        token = value
        defaults.set(value, forKey: tokenKey)
    }

    func logout() {
        token = nil
        defaults.removeObject(forKey: tokenKey)
    }
}
