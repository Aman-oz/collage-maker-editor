import SwiftUI

@main
struct iOSApp: App {
    // Push notifications (Firebase Messaging); see AppDelegate.
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
