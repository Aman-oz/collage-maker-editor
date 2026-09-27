package org.example.project.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.context

internal actual fun openStoreRating() {
    val context = FileKit.context
    val packageName = context.packageName
    // market:// opens the Play Store app directly; without it (no Play Store) use the web listing.
    try {
        context.startActivity(view("market://details?id=$packageName"))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(view("https://play.google.com/store/apps/details?id=$packageName"))
    }
}

internal actual fun appStoreUrl(): String? =
    "https://play.google.com/store/apps/details?id=${FileKit.context.packageName}"

private fun view(url: String) =
    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
