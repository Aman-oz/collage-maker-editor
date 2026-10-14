package org.example.project.ui.common

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import org.example.project.ui.theme.findActivity

/** How many composables currently ask for protection; FLAG_SECURE stays set while any does. */
private var secureRequests = 0

/**
 * `FLAG_SECURE` on the activity's window: screenshots and recordings come out black, and the app's
 * thumbnail in Recents is blanked. Counted, so one screen letting go doesn't lift another's request.
 */
@Composable
internal actual fun PreventScreenCapture(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled, view) {
        val window = view.context.findActivity()?.window
        if (!enabled || window == null) return@DisposableEffect onDispose {}
        if (secureRequests++ == 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (--secureRequests == 0) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
