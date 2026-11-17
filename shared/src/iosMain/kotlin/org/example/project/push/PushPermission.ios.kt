package org.example.project.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.example.project.AppLog
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun RequestPushPermission(onAsked: () -> Unit) {
    LaunchedEffect(Unit) {
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { granted, error ->
            AppLog.i("Push", "Notification permission granted: $granted${error?.let { " ($it)" } ?: ""}")
            dispatch_async(dispatch_get_main_queue()) {
                // Registers with APNs (again) so the FCM token is ready; AppDelegate does it at launch too.
                if (granted) UIApplication.sharedApplication.registerForRemoteNotifications()
                onAsked()
            }
        }
    }
}
