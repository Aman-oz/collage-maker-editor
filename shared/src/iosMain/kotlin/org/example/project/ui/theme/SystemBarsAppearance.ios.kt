package org.example.project.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.uikit.LocalUIViewController
import org.example.project.data.ThemeMode
import platform.UIKit.UIUserInterfaceStyle

@Composable
internal actual fun SystemBarsAppearance(mode: ThemeMode, darkTheme: Boolean) {
    val controller = LocalUIViewController.current
    SideEffect {
        // The default status bar style follows the controller's interface style, so overriding it
        // flips the status bar; Unspecified hands control back to the system setting.
        controller.overrideUserInterfaceStyle = when (mode) {
            ThemeMode.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            ThemeMode.Light -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            ThemeMode.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }
        controller.setNeedsStatusBarAppearanceUpdate()
    }
}
