package org.example.project.data.appicon

import org.example.project.AppLog
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.alternateIconName
import platform.UIKit.setAlternateIconName
import platform.UIKit.supportsAlternateIcons

internal actual fun currentAppIcon(): String =
    UIApplication.sharedApplication.alternateIconName ?: AppIcons.DEFAULT

internal actual fun setAppIcon(id: String) {
    val app = UIApplication.sharedApplication
    if (!app.supportsAlternateIcons) return
    // null is the primary icon (`AppIcon`); anything else names an app icon set.
    app.setAlternateIconName(id.takeUnless { it == AppIcons.DEFAULT }) { error: NSError? ->
        if (error != null) AppLog.w("AppIcons", "Couldn't switch to '$id': ${error.localizedDescription}")
    }
}
