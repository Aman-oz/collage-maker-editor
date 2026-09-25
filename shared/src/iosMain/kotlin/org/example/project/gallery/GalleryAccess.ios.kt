package org.example.project.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Foundation.NSURL
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume

private fun mapStatus(status: PHAuthorizationStatus): GalleryAccessStatus = when (status) {
    PHAuthorizationStatusAuthorized -> GalleryAccessStatus.Granted
    PHAuthorizationStatusLimited -> GalleryAccessStatus.Limited
    PHAuthorizationStatusDenied, PHAuthorizationStatusRestricted -> GalleryAccessStatus.Denied
    else -> GalleryAccessStatus.NotDetermined
}

private fun currentIOSStatus(): GalleryAccessStatus =
    mapStatus(PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite))

private suspend fun requestIOSAccess(): GalleryAccessStatus = suspendCancellableCoroutine { continuation ->
    PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { status ->
        if (continuation.isActive) continuation.resume(mapStatus(status))
    }
}

@Composable
actual fun rememberGalleryAccessState(): GalleryAccessState {
    var status by remember { mutableStateOf(currentIOSStatus()) }
    val scope = rememberCoroutineScope()
    val currentStatus = status

    // Picks up a permission granted from Settings when the user comes back to the app.
    LifecycleResumeEffect(Unit) {
        status = currentIOSStatus()
        onPauseOrDispose { }
    }

    return object : GalleryAccessState {
        override val status: GalleryAccessStatus = currentStatus
        override fun requestAccess() {
            scope.launch { status = requestIOSAccess() }
        }
        override fun openSettings() {
            val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
}
