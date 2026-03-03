import Foundation
import Combine

@MainActor
final class MainViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published private(set) var locations: [StoredLocation] = []
    @Published var loginInProgress = false
    @Published var authError: String?
    
    // COMPLIANCE ADDED: Track consent approval
    @Published private(set) var consentGiven: Bool = UserDefaults.standard.bool(forKey: "consent_given")

    // Force update state
    @Published private(set) var forceUpdateRequired = false
    @Published private(set) var serverVersion: String? = nil

    let authStore: AuthStore
    private let locationStore: LocationStore
    private let apiClient: APIClient
    private let tracker: LocationTrackerManager
    private var cancellables = Set<AnyCancellable>()

    init(authStore: AuthStore, locationStore: LocationStore, apiClient: APIClient, tracker: LocationTrackerManager) {
        self.authStore = authStore
        self.locationStore = locationStore
        self.apiClient = apiClient
        self.tracker = tracker

        // Forward authStore changes so ContentView re-evaluates isSignedIn
        authStore.objectWillChange
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in self?.objectWillChange.send() }
            .store(in: &cancellables)

        locationStore.$locations
            .receive(on: RunLoop.main)
            .sink { [weak self] in self?.locations = $0 }
            .store(in: &cancellables)

        // Check for mandatory update immediately on launch
        Task { await checkForceUpdate() }
    }

    // MARK: – Force Update

    private func checkForceUpdate() async {
        let installed = installedVersionName()
        guard let response = await apiClient.checkAppVersion(),
              let serverVer = response.version, !serverVer.isEmpty else { return }
        if installed.isOlderThan(serverVer) {
            forceUpdateRequired = true
            serverVersion = serverVer
        }
    }

    /// Returns `CFBundleShortVersionString` (e.g. "1.0") from the app bundle.
    private func installedVersionName() -> String {
        (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String) ?? ""
    }

    func login() {
        if email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || password.isEmpty {
            authError = "Email and password are required"
            return
        }

        loginInProgress = true
        authError = nil

        Task {
            do {
                let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
                let result = try await apiClient.login(
                    email: trimmedEmail,
                    password: password
                )
                authStore.setToken(result.token)
                authStore.setEmail(result.email ?? trimmedEmail)
                if authStore.isShiftActive {
                    tracker.startTrackingIfPossible()
                }
                await tracker.syncPendingLocations()
                loginInProgress = false
            } catch {
                loginInProgress = false
                authError = error.localizedDescription
            }
        }
    }

    func logout() {
        authStore.logout()
        tracker.stopTracking()
        locationStore.clearAll()
    }

    func syncPendingLocations() {
        Task {
            await tracker.syncPendingLocations()
        }
    }

    func toggleShift() {
        let nextState = !authStore.isShiftActive
        authStore.setShiftActive(nextState)
        
        if nextState {
            tracker.startTrackingIfPossible()
        } else {
            tracker.stopTracking()
        }
    }
    
    // COMPLIANCE ADDED: User agrees to tracking
    func acceptConsent() {
        UserDefaults.standard.set(true, forKey: "consent_given")
        consentGiven = true
    }
}

// MARK: – Semantic version comparison

private extension String {
    /// Returns true if `self` represents a version older than `other`.
    /// e.g. "1.0".isOlderThan("1.1.0") == true
    ///      "1.0".isOlderThan("0.9")   == false
    ///      "1.0".isOlderThan("1.0")   == false
    func isOlderThan(_ other: String) -> Bool {
        let a = self.trimmingCharacters(in: .whitespaces).split(separator: ".").map { Int($0) ?? 0 }
        let b = other.trimmingCharacters(in: .whitespaces).split(separator: ".").map { Int($0) ?? 0 }
        let maxLen = max(a.count, b.count)
        for i in 0..<maxLen {
            let av = i < a.count ? a[i] : 0
            let bv = i < b.count ? b[i] : 0
            if av < bv { return true }   // installed behind → needs update
            if av > bv { return false }  // installed ahead  → no update
        }
        return false // equal
    }
}
