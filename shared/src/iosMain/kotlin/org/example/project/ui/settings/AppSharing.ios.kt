package org.example.project.ui.settings

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

internal actual fun shareText(text: String) {
    val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    topViewController()?.presentViewController(sheet, animated = true, completion = null)
}

/** The controller currently on screen, so the sheet is presented above any modal already shown. */
private fun topViewController(): UIViewController? {
    @Suppress("DEPRECATION")
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
