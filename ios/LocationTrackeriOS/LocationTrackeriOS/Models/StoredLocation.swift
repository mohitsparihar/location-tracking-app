import Foundation
import CoreLocation

struct StoredLocation: Codable, Identifiable {
    let id: UUID
    let latitude: Double
    let longitude: Double
    let accuracy: Float?
    let altitude: Double?
    let altitudeAccuracy: Float?
    let heading: Float?
    let speed: Float?
    let timestamp: Int64
    let isBackground: Bool
    var uploaded: Bool

    init(from location: CLLocation, isBackground: Bool) {
        id = UUID()
        latitude = location.coordinate.latitude
        longitude = location.coordinate.longitude
        accuracy = location.horizontalAccuracy >= 0 ? Float(location.horizontalAccuracy) : nil
        altitude = location.verticalAccuracy >= 0 ? location.altitude : nil
        altitudeAccuracy = location.verticalAccuracy >= 0 ? Float(location.verticalAccuracy) : nil
        heading = location.course >= 0 ? Float(location.course) : nil
        speed = location.speed >= 0 ? Float(location.speed) : nil
        timestamp = Int64(location.timestamp.timeIntervalSince1970 * 1000)
        self.isBackground = isBackground
        uploaded = false
    }
}
