package org.example.project.ui.share

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.shareFile

internal actual suspend fun shareImage(imagePath: String, target: ShareTarget) {
    FileKit.shareFile(PlatformFile(imagePath))
}
