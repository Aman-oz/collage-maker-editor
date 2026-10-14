package org.example.project.ui.common

import androidx.compose.runtime.Composable

/** iOS has no public API to block screenshots, so this does nothing there for now. */
@Composable
internal actual fun PreventScreenCapture(enabled: Boolean) = Unit
