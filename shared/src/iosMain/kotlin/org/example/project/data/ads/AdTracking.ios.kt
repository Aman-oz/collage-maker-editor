package org.example.project.data.ads

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AppTrackingTransparency.ATTrackingManager
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusNotDetermined

/** Shows the "Allow tracking?" prompt the first time; later launches return straight away. */
internal actual suspend fun requestAdTrackingAuthorization() {
    if (ATTrackingManager.trackingAuthorizationStatus != ATTrackingManagerAuthorizationStatusNotDetermined) return
    suspendCancellableCoroutine { cont ->
        ATTrackingManager.requestTrackingAuthorizationWithCompletionHandler { status ->
            println("Ads: tracking authorization status=$status")
            if (cont.isActive) cont.resume(Unit)
        }
    }
}
