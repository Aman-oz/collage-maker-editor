package org.example.project.ui.common

import androidx.compose.runtime.Composable

/**
 * Keeps the screen out of screenshots and screen recordings while [enabled] and this is in
 * composition: used while a non-subscriber has a premium item applied (the same moments the
 * [ProFeatureBar] shows), so the Pro result can't be kept by capturing the screen instead of
 * subscribing. Several screens may ask at once (an inline tool over the editor); capture is
 * allowed again only when none does.
 */
@Composable
internal expect fun PreventScreenCapture(enabled: Boolean)
