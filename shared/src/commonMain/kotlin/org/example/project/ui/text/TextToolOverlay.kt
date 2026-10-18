package org.example.project.ui.text

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * The Text tool opened over an editor other than the photo editor (freestyle, PIP): [image] is the
 * editor's picture baked as it was when the tool opened (the tool's backdrop, without the texts,
 * which the tool shows itself), [texts] the texts it opens with and [selectedTextId] the one tapped
 * on the picture, if a tap opened it. Identity matters: each opening is its own session.
 */
internal class TextToolSession(val image: ImageBitmap, val texts: TextLayers?, val selectedTextId: Long?)

/**
 * The Text tool over a whole editor screen while [session] is set, fading in and out (it stays on
 * screen through the fade with the session it had). Back closes it; composed after the editor's own
 * back handler, it takes back first.
 *
 * The session belongs in the editor's ViewModel, not here: the paywall the tool offers covers the
 * editor, which leaves composition, and the tool has to still be open on return (its texts survive
 * as the tool's saveable state).
 */
@Composable
internal fun TextToolOverlay(
    session: TextToolSession?,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (TextLayers) -> Unit,
    onOpenPremium: () -> Unit,
) {
    val current by rememberUpdatedState(session)
    var shown by remember { mutableStateOf(session) }
    if (session != null) shown = session
    AnimatedVisibility(visible = session != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
        shown?.let { opened ->
            key(opened) {
                // The checks keep a tap landing while the tool fades away from closing or applying twice.
                TextTool(
                    sourceImage = opened.image,
                    initialTexts = opened.texts,
                    selectedTextId = opened.selectedTextId,
                    isPremium = isPremium,
                    onClose = { if (current === opened) onClose() },
                    onApply = { layers -> if (current === opened) onApply(layers) },
                    onOpenPremium = onOpenPremium,
                )
            }
        }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = session != null,
        onBackCompleted = onClose,
    )
}
