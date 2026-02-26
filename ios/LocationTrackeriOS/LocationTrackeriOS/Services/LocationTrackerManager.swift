import Foundation
import CoreLocation
import UIKit
import UserNotifications

@MainActor
final class LocationTrackerManager: NSObject, ObservableObject {
    @Published private(set) var authorizationStatus: CLAuthorizationStatus

    var hasRequiredPermissions: Bool {
        authorizationStatus == .authorizedAlways
    }

    private let manager = CLLocationManager()
    private let authStore: AuthStore
    private let locationStore: LocationStore
    private let apiClient: APIClient

    // Capture every 10 min while moving, every 30 min while stationary.
    private let movingInterval: TimeInterval = 10 * 60
    private let stationaryInterval: TimeInterval = 30 * 60
    // Anything above 0.5 m/s (~1.8 km/h) is considered moving.
    private let movingSpeedThreshold: Double = 0.5
    private var lastCaptureDate: Date?

    init(authStore: AuthStore, locationStore: LocationStore, apiClient: APIClient) {
        self.authStore = authStore
        self.locationStore = locationStore
        self.apiClient = apiClient
        self.authorizationStatus = manager.authorizationStatus
        super.init()

        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = kCLDistanceFilterNone
        manager.pausesLocationUpdatesAutomatically = false
    }

    func refreshAuthorizationStatus() {
        authorizationStatus = manager.authorizationStatus
    }

    func requestForegroundPermission() {
        manager.requestWhenInUseAuthorization()
    }

    func requestBackgroundPermission() {
        manager.requestAlwaysAuthorization()
    }

    func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }

    func startTrackingIfPossible() {
        guard hasRequiredPermissions else { return }
        manager.allowsBackgroundLocationUpdates = true
        manager.startUpdatingLocation()
        postTrackingNotification(body: "Location tracking is active.")
    }

    func stopTracking() {
        manager.stopUpdatingLocation()
        removeTrackingNotification()
    }

    func syncPendingLocations() async {
        guard let token = authStore.token else { return }

        let pending = locationStore.pendingLocations()
        guard !pending.isEmpty else { return }

        let result: UploadResult
        do {
            let deviceInfo = DeviceInfo.current()
            if pending.count == 1, let first = pending.first {
                result = try await apiClient.uploadSingle(location: first, token: token, deviceInfo: deviceInfo)
            } else {
                result = try await apiClient.uploadBatch(locations: pending, token: token, deviceInfo: deviceInfo)
            }
        } catch {
            return
        }

        switch result {
        case .success(let newToken):
            if let t = newToken, !t.isEmpty {
                authStore.setToken(t)
            }
            let ids = Set(pending.map { $0.id })
            locationStore.markUploaded(ids: ids)
        case .unauthorized:
            authStore.logout()
            stopTracking()
        case .failure:
            break
        }
    }

    private func shouldTrackNow() -> Bool {
        let hour = Calendar.current.component(.hour, from: Date())
        return hour >= 6
    }

    // Returns true if enough time has passed since the last capture,
    // using a shorter interval when the device is moving.
    private func shouldCapture(_ location: CLLocation) -> Bool {
        guard let last = lastCaptureDate else { return true }
        let elapsed = Date().timeIntervalSince(last)
        let isMoving = location.speed >= movingSpeedThreshold
        let required = isMoving ? movingInterval : stationaryInterval
        return elapsed >= required
    }

    // MARK: - Tracking notification

    private static let notificationID = "location-tracker-running"

    // Posts (or replaces) a single persistent notification. Using the same
    // identifier means iOS replaces the previous one instead of stacking them.
    private func postTrackingNotification(body: String) {
        let content = UNMutableNotificationContent()
        content.title = "Location Tracker Running"
        content.body = body
        // .passive interruption level keeps it in the notification tray
        // without making a sound or lighting up the screen on iOS 15+.
        if #available(iOS 15.0, *) {
            content.interruptionLevel = .passive
        }
        let request = UNNotificationRequest(
            identifier: Self.notificationID,
            content: content,
            trigger: nil   // nil = deliver immediately
        )
        UNUserNotificationCenter.current().add(request)
    }

    private func removeTrackingNotification() {
        UNUserNotificationCenter.current()
            .removeDeliveredNotifications(withIdentifiers: [Self.notificationID])
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [Self.notificationID])
    }

    // MARK: - Location handling

    private func handleLocation(_ location: CLLocation, isBackground: Bool) {
        guard shouldTrackNow(), shouldCapture(location) else { return }

        lastCaptureDate = Date()
        let saved = locationStore.record(location, isBackground: isBackground)

        let coords = String(format: "%.5f, %.5f", location.coordinate.latitude, location.coordinate.longitude)
        let time = DateFormatter.localizedString(from: Date(), dateStyle: .none, timeStyle: .short)
        postTrackingNotification(body: "Last point: \(coords) at \(time)")

        guard let token = authStore.token else { return }

        // beginBackgroundTask tells iOS to keep the app alive long enough
        // for the network request to complete after we're sent to background.
        let bgTask = UIApplication.shared.beginBackgroundTask(withName: "location-upload") {
            // Expiration handler — iOS is about to force-suspend us.
            // The location is already saved locally as pending, so nothing is lost.
        }

        Task {
            do {
                let result = try await apiClient.uploadSingle(
                    location: saved,
                    token: token,
                    deviceInfo: DeviceInfo.current()
                )
                switch result {
                case .success(let newToken):
                    if let t = newToken, !t.isEmpty {
                        authStore.setToken(t)
                    }
                    locationStore.markUploaded(ids: [saved.id])
                case .unauthorized:
                    authStore.logout()
                    stopTracking()
                case .failure:
                    break
                }
            } catch {
                // Keep item as pending and retry on next sync cycle.
            }
            UIApplication.shared.endBackgroundTask(bgTask)
        }
    }
}

extension LocationTrackerManager: CLLocationManagerDelegate {
    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        Task { @MainActor in
            refreshAuthorizationStatus()
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        Task { @MainActor in
            let inBackground = UIApplication.shared.applicationState != .active
            handleLocation(location, isBackground: inBackground)
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // Intentionally ignored. Next delivery will retry.
    }
}
