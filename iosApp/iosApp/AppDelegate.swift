import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import Shared

/// Push notifications through Firebase Cloud Messaging.
///
/// Every install subscribes to the `news` topic (the same one as Android), so a campaign sent from
/// the Firebase console to that topic reaches everyone. A single device can be targeted by its FCM
/// token, which is printed at launch. The permission prompt itself is shown from Home
/// (`RequestPushPermission` in shared); until it is granted pushes arrive but aren't shown.
///
/// Firebase needs the app's APNs key (.p8) under Project settings → Cloud Messaging to deliver to iOS.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    private static let topic = "news"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Before anything else touches Firebase. The ads SDK only configures it when nobody has yet.
        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        // Gets the APNs token now (no prompt); notifications show once the user allows them.
        application.registerForRemoteNotifications()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("[Push] Couldn't register with APNs: \(error.localizedDescription)")
    }

    // MARK: MessagingDelegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        print("[Push] FCM token: \(fcmToken)")
        messaging.subscribe(toTopic: Self.topic) { error in
            if let error {
                print("[Push] Couldn't subscribe to topic '\(Self.topic)': \(error.localizedDescription)")
            } else {
                print("[Push] Subscribed to topic '\(Self.topic)'")
            }
        }
    }

    // MARK: UNUserNotificationCenterDelegate

    /// Shows a push as a banner while the app is open too (iOS hides it by default).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    /// A tap opens the app. A usage reminder (data/reminders in shared) also names the feature to
    /// open, which the app does once Home is showing; a Firebase push just opens the app.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let feature = response.notification.request.content.userInfo["feature"] as? String {
            ReminderTaps.shared.onTapped(featureName: feature)
        }
        completionHandler()
    }
}
