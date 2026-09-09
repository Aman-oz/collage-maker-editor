package org.example.project

import androidx.compose.ui.window.ComposeUIViewController
import org.example.project.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
