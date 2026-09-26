package org.example.project

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import org.example.project.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    // Screens handle the keyboard themselves through WindowInsets (safeDrawing includes the IME);
    // the default FocusableAboveKeyboard would additionally pan the whole view to the focused
    // field, shoving e.g. the Text tool's photo upward while typing on it.
    return ComposeUIViewController(configure = { onFocusBehavior = OnFocusBehavior.DoNothing }) { App() }
}
