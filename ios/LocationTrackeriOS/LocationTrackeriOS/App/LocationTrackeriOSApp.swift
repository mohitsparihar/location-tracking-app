import SwiftUI
import UserNotifications
import BackgroundTasks // COMPLIANCE ADDED: Background tasks framework

// Allows notifications to appear as banners even while the app is in the foreground.
private final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationDelegate()

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list])
    }
}

@main
struct LocationTrackeriOSApp: App {
    @StateObject private var authStore: AuthStore
    @StateObject private var locationStore: LocationStore
    @StateObject private var tracker: LocationTrackerManager
    @StateObject private var viewModel: MainViewModel

    init() {
        UNUserNotificationCenter.current().delegate = NotificationDelegate.shared
        let authStore = AuthStore()
        let locationStore = LocationStore()
        let apiClient = APIClient()
        let tracker = LocationTrackerManager(authStore: authStore, locationStore: locationStore, apiClient: apiClient)
        let viewModel = MainViewModel(
            authStore: authStore,
            locationStore: locationStore,
            apiClient: apiClient,
            tracker: tracker
        )

        _authStore = StateObject(wrappedValue: authStore)
        _locationStore = StateObject(wrappedValue: locationStore)
        _tracker = StateObject(wrappedValue: tracker)
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    var body: some Scene {
        WindowGroup {
            ContentView(viewModel: viewModel, tracker: tracker)
                .preferredColorScheme(.light)
                .task {
                    _ = try? await UNUserNotificationCenter.current()
                        .requestAuthorization(options: [.alert, .sound])
                }
        }
        // COMPLIANCE ADDED: Background scheduling registration
        .onChange(of: viewModel.authStore.isSignedIn) { signedIn in
            if signedIn {
                scheduleAppRefresh()
            }
        }
        .backgroundTask(.appRefresh("com.geoiq.trackiq.locationFetch")) {
            if viewModel.authStore.isSignedIn {
                // COMPLIANCE: Fetch location and sync pending data
                tracker.fetchSingleLocation()
                await tracker.syncPendingLocations()
            }
            scheduleAppRefresh()
        }
    }

    // COMPLIANCE ADDED: Schedule the 10 minute task
    func scheduleAppRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: "com.geoiq.trackiq.locationFetch")
        request.earliestBeginDate = Date(timeIntervalSinceNow: 10 * 60) // 10 minutes
        try? BGTaskScheduler.shared.submit(request)
    }
}
