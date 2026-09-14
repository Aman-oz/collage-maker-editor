package org.example.project.gallery

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private fun requiredPermissions(): Array<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.READ_MEDIA_IMAGES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
    } else {
        add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}.toTypedArray()

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun currentStatus(context: Context): GalleryAccessStatus = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isGranted(context, Manifest.permission.READ_MEDIA_IMAGES) ->
        GalleryAccessStatus.Granted
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE) ->
        GalleryAccessStatus.Granted
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> GalleryAccessStatus.Limited
    else -> GalleryAccessStatus.NotDetermined
}

@Composable
actual fun rememberGalleryAccessState(): GalleryAccessState {
    val context = LocalContext.current
    var lastCheckedStatus by remember { mutableStateOf(currentStatus(context)) }
    var hasRequestedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasRequestedOnce = true
        lastCheckedStatus = currentStatus(context)
    }

    val resolvedStatus = if (lastCheckedStatus != GalleryAccessStatus.NotDetermined) {
        lastCheckedStatus
    } else if (hasRequestedOnce) {
        GalleryAccessStatus.Denied
    } else {
        GalleryAccessStatus.NotDetermined
    }

    return object : GalleryAccessState {
        override val status: GalleryAccessStatus = resolvedStatus
        override fun requestAccess() {
            launcher.launch(requiredPermissions())
        }
    }
}
