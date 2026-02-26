import SwiftUI
import CoreLocation

struct ContentView: View {
    @ObservedObject var viewModel: MainViewModel
    @ObservedObject var tracker: LocationTrackerManager

    var body: some View {
        Group {
            if !tracker.hasRequiredPermissions {
                PermissionBlockingView(tracker: tracker)
            } else if !viewModel.authStore.isSignedIn {
                LoginView(viewModel: viewModel)
            } else {
                LocationsView(viewModel: viewModel)
            }
        }
        .padding(16)
        .onAppear {
            tracker.refreshAuthorizationStatus()
            if viewModel.authStore.isSignedIn {
                tracker.startTrackingIfPossible()
                viewModel.syncPendingLocations()
            }
        }
        .onChange(of: tracker.authorizationStatus) { _ in
            if viewModel.authStore.isSignedIn {
                tracker.startTrackingIfPossible()
            }
        }
    }
}

private struct PermissionBlockingView: View {
    @ObservedObject var tracker: LocationTrackerManager

    var body: some View {
        VStack(spacing: 12) {
            Text("Location permission required")
                .font(.title3.weight(.bold))

            Text("This app is blocked until background location access is granted.")
                .multilineTextAlignment(.center)

            Button("Grant Foreground Location") {
                tracker.requestForegroundPermission()
            }
            .buttonStyle(.borderedProminent)

            Button("Grant Background Location") {
                tracker.requestBackgroundPermission()
            }
            .buttonStyle(.borderedProminent)

            Button("Open App Settings") {
                tracker.openSettings()
            }
            .buttonStyle(.bordered)

            Text(statusMessage(tracker.authorizationStatus))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func statusMessage(_ status: CLAuthorizationStatus) -> String {
        switch status {
        case .notDetermined:
            return "Permission not requested yet"
        case .restricted:
            return "Permission is restricted"
        case .denied:
            return "Permission denied"
        case .authorizedWhenInUse:
            return "Foreground granted. Background still required."
        case .authorizedAlways:
            return "Permission granted"
        @unknown default:
            return "Unknown permission state"
        }
    }
}

private struct LoginView: View {
    @ObservedObject var viewModel: MainViewModel

    var body: some View {
        VStack(spacing: 12) {
            Text("Sign in")
                .font(.title2.weight(.bold))

            TextField("Email", text: $viewModel.email)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)

            SecureField("Password", text: $viewModel.password)
                .textFieldStyle(.roundedBorder)

            if let error = viewModel.authError {
                Text(error)
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Button(viewModel.loginInProgress ? "Signing in..." : "Sign In") {
                viewModel.login()
            }
            .buttonStyle(.borderedProminent)
            .disabled(viewModel.loginInProgress)
            .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: 520)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct LocationsView: View {
    @ObservedObject var viewModel: MainViewModel

    private let formatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter
    }()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recorded Locations")
                    .font(.title3.weight(.bold))
                Spacer()
                Button("Sync") { viewModel.syncPendingLocations() }
                Button("Logout") { viewModel.logout() }
            }

            if viewModel.locations.isEmpty {
                Text("No locations recorded yet. Keep the app running to capture points every 10 minutes.")
                    .foregroundStyle(.secondary)
            } else {
                List(viewModel.locations) { item in
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Lat: \(item.latitude), Lng: \(item.longitude)")
                        Text("Accuracy: \(item.accuracy.map { String($0) } ?? "N/A") m")
                        Text("Time: \(formatter.string(from: Date(timeIntervalSince1970: TimeInterval(item.timestamp) / 1000)))")
                        Text("Mode: \(item.isBackground ? "Background" : "Foreground")")
                        Text(item.uploaded ? "Uploaded" : "Pending upload")
                            .foregroundStyle(item.uploaded ? .green : .orange)
                    }
                    .padding(.vertical, 4)
                }
                .listStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}
