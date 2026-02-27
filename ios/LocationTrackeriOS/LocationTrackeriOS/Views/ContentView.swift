import SwiftUI
import CoreLocation

struct ContentView: View {
    @ObservedObject var viewModel: MainViewModel
    @ObservedObject var tracker: LocationTrackerManager

    var body: some View {
        Group {
            if !tracker.hasRequiredPermissions {
                PermissionBlockingView(tracker: tracker)
                    .padding(16)
            } else if !viewModel.authStore.isSignedIn {
                LoginView(viewModel: viewModel)
            } else {
                NavigationStack {
                    LocationsView(viewModel: viewModel)
                }
            }
        }
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
            Text("Missing Compliance Data")
                .font(.title3.weight(.bold))
                .foregroundColor(.red)

            Text("Warning: Your visit cannot be verified. This app is blocked until background location access is granted.")
                .multilineTextAlignment(.center)
                .foregroundColor(.red)

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
    @State private var passwordVisible = false

    private let purple = Color(red: 0.42, green: 0.13, blue: 0.96)
    private let lightBackground = Color(red: 0.95, green: 0.95, blue: 0.97)
    private let placeholderBg = Color(red: 0.93, green: 0.91, blue: 1.0)
    private let textGray = Color(red: 0.42, green: 0.44, blue: 0.50)
    private let iconGray = Color(red: 0.61, green: 0.64, blue: 0.69)
    private let darkText = Color(red: 0.10, green: 0.10, blue: 0.18)
    private let borderColor = Color(red: 0.82, green: 0.82, blue: 0.86)

    var body: some View {
        ScrollView {
            VStack {
                Spacer(minLength: 40)

                    // Card
                    VStack(spacing: 0) {
                        VStack(spacing: 8) {
                            // GeoIQ logo
                            Image("geoiq_logo")
                                .resizable()
                                .scaledToFit()
                                .frame(height: 36)

                            Spacer().frame(height: 8)

                            // Title
                            Text("TrackIQ")
                                .font(.title2.weight(.bold))
                                .foregroundColor(darkText)
                        }

                        Spacer().frame(height: 20)

                        // Email field
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Username or Email")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(darkText)

                            HStack(spacing: 10) {
                                Image(systemName: "envelope")
                                    .foregroundColor(iconGray)
                                TextField("name@company.com", text: $viewModel.email)
                                    .keyboardType(.emailAddress)
                                    .textInputAutocapitalization(.never)
                                    .autocorrectionDisabled()
                                    .foregroundColor(darkText)
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 14)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(borderColor, lineWidth: 1)
                            )
                        }

                        Spacer().frame(height: 14)

                        // Password field
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Password")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(darkText)

                            HStack(spacing: 10) {
                                Image(systemName: "lock")
                                    .foregroundColor(iconGray)
                                if passwordVisible {
                                    TextField("••••••••••", text: $viewModel.password)
                                        .foregroundColor(darkText)
                                } else {
                                    SecureField("••••••••••", text: $viewModel.password)
                                        .foregroundColor(darkText)
                                }
                                Button(action: { passwordVisible.toggle() }) {
                                    Image(systemName: passwordVisible ? "eye" : "eye.slash")
                                        .foregroundColor(iconGray)
                                }
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 14)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(borderColor, lineWidth: 1)
                            )
                        }

                        if let error = viewModel.authError {
                            Text(error)
                                .foregroundStyle(.red)
                                .font(.caption)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.top, 6)
                        }

                        Spacer().frame(height: 20)

                        // Log In button
                        Button(action: { viewModel.login() }) {
                            Text(viewModel.loginInProgress ? "Signing in..." : "Log In")
                                .font(.headline.weight(.bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(purple)
                                )
                        }
                        .disabled(viewModel.loginInProgress)

                        Spacer().frame(height: 14)

                        // Sign up link
                        // HStack(spacing: 0) {
                        //     Text("Don't have an account? ")
                        //         .foregroundColor(textGray)
                        //         .font(.footnote)
                        //     Button("Sign up for free") { }
                        //         .font(.footnote.weight(.bold))
                        //         .foregroundColor(purple)
                        // }
                    }
                    .padding(24)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 24))
                    .shadow(color: Color.black.opacity(0.07), radius: 16, x: 0, y: 4)
                    .padding(.horizontal, 20)

                    Spacer(minLength: 40)
                }
                .frame(minHeight: UIScreen.main.bounds.height)
        }
        .frame(maxWidth: .infinity)
        .background(lightBackground.ignoresSafeArea())
    }
}

private struct LocationsView: View {
    @ObservedObject var viewModel: MainViewModel
    @State private var selectedTab: LocationTab = .all
    @State private var showSettings = false

    private let purple = Color(red: 0.42, green: 0.13, blue: 0.96)
    private let lightBg = Color(red: 0.94, green: 0.94, blue: 0.96)
    private let iconBg = Color(red: 0.91, green: 0.88, blue: 1.0)
    private let tabGray = Color(red: 0.55, green: 0.56, blue: 0.60)

    enum LocationTab: String, CaseIterable {
        case all = "All Records"
        case synced = "Synced"
        case offline = "Offline"
    }

    private let displayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM d, yyyy, h:mm a"
        return f
    }()

    private var filteredLocations: [StoredLocation] {
        switch selectedTab {
        case .all:     return viewModel.locations
        case .synced:  return viewModel.locations.filter { $0.uploaded }
        case .offline: return viewModel.locations.filter { !$0.uploaded }
        }
    }

    var body: some View {
        ZStack {
            lightBg.ignoresSafeArea()

            VStack(spacing: 0) {
                // Header
                HStack(spacing: 8) {
                    Image(systemName: "mappin.circle.fill")
                        .font(.title2)
                        .foregroundColor(purple)
                    Text("Location History")
                        .font(.title3.weight(.bold))
                    Spacer()
                    Button(action: { showSettings = true }) {
                        Image(systemName: "gearshape.fill")
                            .font(.title3)
                            .foregroundColor(Color(red: 0.10, green: 0.10, blue: 0.18))
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 16)

                // Tab row
                HStack(spacing: 0) {
                    ForEach(LocationTab.allCases, id: \.self) { tab in
                        Button(action: { selectedTab = tab }) {
                            VStack(spacing: 8) {
                                Text(tab.rawValue)
                                    .font(.subheadline.weight(selectedTab == tab ? .semibold : .regular))
                                    .foregroundColor(selectedTab == tab ? purple : tabGray)
                                Rectangle()
                                    .fill(selectedTab == tab ? purple : Color.clear)
                                    .frame(height: 2)
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                .overlay(alignment: .bottom) { Divider() }
                .padding(.horizontal, 16)

                // Content
                if filteredLocations.isEmpty {
                    Spacer()
                    Text("No locations to show.")
                        .foregroundStyle(.secondary)
                    Spacer()
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(filteredLocations) { item in
                                LocationCard(
                                    item: item,
                                    formatter: displayFormatter,
                                    purple: purple,
                                    iconBg: iconBg
                                )
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 16)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationDestination(isPresented: $showSettings) {
            SettingsView(viewModel: viewModel)
        }
        .navigationBarHidden(true)
    }
}

private struct LocationCard: View {
    let item: StoredLocation
    let formatter: DateFormatter
    let purple: Color
    let iconBg: Color

    private let textGray = Color(red: 0.55, green: 0.56, blue: 0.60)
    private let darkText = Color(red: 0.10, green: 0.10, blue: 0.18)

    private var date: Date {
        Date(timeIntervalSince1970: TimeInterval(item.timestamp) / 1000)
    }

    private var coordText: String {
        let lat = String(format: "%.4f", abs(item.latitude))
        let lng = String(format: "%.4f", abs(item.longitude))
        let latDir = item.latitude >= 0 ? "N" : "S"
        let lngDir = item.longitude >= 0 ? "E" : "W"
        return "\(lat)° \(latDir), \(lng)° \(lngDir)"
    }

    var body: some View {
        HStack(spacing: 12) {
            // Icon box
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(iconBg)
                    .frame(width: 58, height: 58)
                Image(systemName: item.uploaded ? "scope" : "location.fill")
                    .font(.title3)
                    .foregroundColor(purple)
            }

            // Details
            VStack(alignment: .leading, spacing: 5) {
                Text(coordText)
                    .font(.subheadline.weight(.bold))
                    .foregroundColor(darkText)

                HStack(spacing: 4) {
                    Image(systemName: "calendar")
                        .font(.caption2)
                    Text(formatter.string(from: date))
                        .font(.caption)
                }
                .foregroundColor(textGray)

                if let accuracy = item.accuracy {
                    HStack(spacing: 4) {
                        Image(systemName: "ruler")
                            .font(.caption2)
                        Text("Accuracy: \(Int(accuracy))m")
                            .font(.caption)
                    }
                    .foregroundColor(textGray)
                }
            }

            Spacer()

            // Status badge
            Group {
                if item.uploaded {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.circle.fill").font(.caption2)
                        Text("SYNCED").font(.caption2.weight(.bold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Capsule().fill(Color(red: 0.13, green: 0.73, blue: 0.45)))
                } else {
                    HStack(spacing: 4) {
                        Image(systemName: "ellipsis.circle.fill").font(.caption2)
                        Text("PENDING").font(.caption2.weight(.bold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Capsule().fill(Color(red: 1.0, green: 0.60, blue: 0.0)))
                }
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: Color.black.opacity(0.06), radius: 8, x: 0, y: 2)
    }
}

private struct SettingsView: View {
    @ObservedObject var viewModel: MainViewModel
    @Environment(\.dismiss) private var dismiss

    private let slateIcon = Color(red: 0.31, green: 0.36, blue: 0.45)
    private let logoutRed = Color(red: 0.93, green: 0.23, blue: 0.23)
    private let logoutBg = Color(red: 1.0, green: 0.93, blue: 0.93)
    private let emailCardBg = Color(red: 0.94, green: 0.95, blue: 0.98)
    private let textGray = Color(red: 0.42, green: 0.44, blue: 0.50)
    private let chevronGray = Color(red: 0.70, green: 0.72, blue: 0.76)

    var body: some View {
        VStack(spacing: 0) {
            // Header
            ZStack {
                HStack {
                    Button(action: { dismiss() }) {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(Color(red: 0.10, green: 0.10, blue: 0.18))
                    }
                    Spacer()
                }
                Text("Settings")
                    .font(.headline.weight(.bold))
                    .foregroundColor(Color(red: 0.10, green: 0.10, blue: 0.18))
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(Color.white)

            Divider()

            ScrollView {
                VStack(spacing: 20) {
                    // Email card
                    Text(viewModel.authStore.userEmail ?? "")
                        .font(.subheadline)
                        .foregroundColor(textGray)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.vertical, 24)
                        .background(
                            RoundedRectangle(cornerRadius: 14)
                                .fill(emailCardBg)
                        )

                    // Menu section
                    VStack(spacing: 0) {

                        Button(action: {
                            if let url = URL(string: "https://geoiq.ai/t&c") {
                                UIApplication.shared.open(url)
                            }
                        }) {
                            SettingsRow(
                                icon: "doc.text",
                                title: "Terms of Service",
                                iconBg: slateIcon,
                                chevronColor: chevronGray
                            )
                        }
                        Divider().padding(.leading, 68)
                        Button(action: {
                            if let url = URL(string: "https://geoiq.ai/privacy-policy") {
                                UIApplication.shared.open(url)
                            }
                        }) {
                            SettingsRow(
                                icon: "lock.shield",
                                title: "Privacy Policy",
                                iconBg: slateIcon,
                                chevronColor: chevronGray
                            )
                        }
                    }
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                    // Log out button
                    Button(action: { viewModel.logout() }) {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.right.square")
                                .font(.system(size: 17, weight: .semibold))
                            Text("Log out")
                                .font(.subheadline.weight(.semibold))
                        }
                        .foregroundColor(logoutRed)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 18)
                        .background(
                            RoundedRectangle(cornerRadius: 14)
                                .fill(logoutBg)
                        )
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 24)
            }

            Spacer()
        }
        .background(Color.white.ignoresSafeArea())
        .navigationBarHidden(true)
    }
}

private struct SettingsRow: View {
    let icon: String
    let title: String
    let iconBg: Color
    let chevronColor: Color

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(iconBg)
                    .frame(width: 36, height: 36)
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.white)
            }
            Text(title)
                .font(.subheadline)
                .foregroundColor(Color(red: 0.10, green: 0.10, blue: 0.18))
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(chevronColor)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }
}
