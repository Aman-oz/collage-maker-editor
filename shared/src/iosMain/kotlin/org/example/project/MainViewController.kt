package org.example.project

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import org.example.project.data.billing.StoreKitBridge
import org.example.project.data.billing.registeredStoreKitBridge
import org.example.project.di.initKoin
import platform.UIKit.UIViewController

/**
 * The iOS entry point. [storeKitBridge] is the Swift StoreKit 2 implementation behind in-app
 * subscriptions; it must be set before Koin starts, since the billing wrapper is created eagerly.
 */
fun MainViewController(storeKitBridge: StoreKitBridge): UIViewController {
    registeredStoreKitBridge = storeKitBridge
    initKoin()
    // Screens handle the keyboard themselves through WindowInsets (safeDrawing includes the IME);
    // the default FocusableAboveKeyboard would additionally pan the whole view to the focused
    // field, shoving e.g. the Text tool's photo upward while typing on it.
    return ComposeUIViewController(configure = { onFocusBehavior = OnFocusBehavior.DoNothing }) { App() }
}
