package org.example.project.ui.share

/**
 * Where a share goes. [androidPackage] lets Android open that app directly; iOS cannot target a
 * specific app from the system share sheet, so there every target opens the same sheet.
 */
enum class ShareTarget(val androidPackage: String?) {
    Instagram("com.instagram.android"),
    WhatsApp("com.whatsapp"),
    Snapchat("com.snapchat.android"),
    Facebook("com.facebook.katana"),
    More(null),
}

/**
 * Shares the JPEG at [imagePath] (an absolute path in the app cache) to [target]. Falls back to the
 * system share sheet when the target app is not installed.
 */
internal expect suspend fun shareImage(imagePath: String, target: ShareTarget)
