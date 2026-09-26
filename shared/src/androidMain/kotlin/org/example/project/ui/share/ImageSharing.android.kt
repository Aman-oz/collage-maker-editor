package org.example.project.ui.share

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.context
import java.io.File

internal actual suspend fun shareImage(imagePath: String, target: ShareTarget) {
    val context = FileKit.context
    // FileKit's dialogs module already declares this provider, exposing the cache directory.
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileKitFileProvider", File(imagePath))
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    // Launching straight into the package avoids needing a <queries> entry to check it's installed.
    if (target.androidPackage != null) {
        try {
            context.startActivity(Intent(send).setPackage(target.androidPackage))
            return
        } catch (_: ActivityNotFoundException) {
            // Not installed: fall through to the chooser.
        }
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
