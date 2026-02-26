import Foundation
import CoreLocation

@MainActor
final class LocationStore: ObservableObject {
    @Published private(set) var locations: [StoredLocation] = []

    private let storageURL: URL

    init(fileManager: FileManager = .default) {
        let appSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let folder = appSupport.appendingPathComponent("LocationTrackeriOS", isDirectory: true)
        try? fileManager.createDirectory(at: folder, withIntermediateDirectories: true, attributes: nil)
        storageURL = folder.appendingPathComponent("locations.json")
        load()
    }

    @discardableResult
    func record(_ location: CLLocation, isBackground: Bool) -> StoredLocation {
        let entry = StoredLocation(from: location, isBackground: isBackground)
        locations.insert(entry, at: 0)
        save()
        return entry
    }

    func pendingLocations() -> [StoredLocation] {
        locations.filter { !$0.uploaded }
    }

    func markUploaded(ids: Set<UUID>) {
        guard !ids.isEmpty else { return }
        locations = locations.map { item in
            var copy = item
            if ids.contains(copy.id) {
                copy.uploaded = true
            }
            return copy
        }
        save()
    }

    private func load() {
        guard let data = try? Data(contentsOf: storageURL) else { return }
        guard let decoded = try? JSONDecoder().decode([StoredLocation].self, from: data) else { return }
        locations = decoded
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(locations) else { return }
        try? data.write(to: storageURL, options: .atomic)
    }
}
