package org.example.project.push

import androidx.compose.runtime.Composable

/**
 * Asks for permission to show push notifications, once, when it enters the composition: the
 * system prompt on iOS, and on Android 13+ (older Android needs no permission). [onAsked] runs when
 * the prompt has been shown or wasn't needed, so the caller can stop asking.
 *
 * Receiving the pushes themselves is set up per platform: `PushNotifications` in androidApp and
 * `AppDelegate` in iosApp.
 */
@Composable
expect fun RequestPushPermission(onAsked: () -> Unit)
