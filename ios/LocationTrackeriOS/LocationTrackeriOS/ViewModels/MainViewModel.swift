import Foundation
import Combine

@MainActor
final class MainViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published private(set) var locations: [StoredLocation] = []
    @Published var loginInProgress = false
    @Published var authError: String?

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

        locationStore.$locations
            .receive(on: RunLoop.main)
            .sink { [weak self] in self?.locations = $0 }
            .store(in: &cancellables)
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
                tracker.startTrackingIfPossible()
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
    }

    func syncPendingLocations() {
        Task {
            await tracker.syncPendingLocations()
        }
    }
}
