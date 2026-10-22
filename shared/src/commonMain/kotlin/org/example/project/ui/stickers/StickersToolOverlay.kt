package org.example.project.ui.stickers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.example.project.ui.text.TextLayers

/**
 * The Stickers tool opened over an editor other than the photo editor (layout, PIP): [image] is the
 * editor's picture baked as it was when the tool opened, without its stickers and texts (the tool
 * draws both itself), [stickers] what it opens with and [selectedStickerId] the one tapped on the
 * picture, if a tap opened it. Identity matters: each opening is its own session.
 */
internal class StickersToolSession(
    val image: ImageBitmap,
    val stickers: List<PlacedSticker>,
    val selectedStickerId: Long?,
    val texts: TextLayers?,
)

/**
 * The Stickers tool over a whole editor screen while [session] is set, fading in and out like
 * `TextToolOverlay`. Back closes it; composed after the editor's own back handler, it takes back
 * first. The session belongs in the editor's ViewModel, so the tool is still open after the paywall.
 */
@Composable
internal fun StickersToolOverlay(
    session: StickersToolSession?,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (List<PlacedSticker>) -> Unit,
    onOpenPremium: () -> Unit,
) {
    val current by rememberUpdatedState(session)
    var shown by remember { mutableStateOf(session) }
    if (session != null) shown = session
    AnimatedVisibility(visible = session != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
        shown?.let { opened ->
            key(opened) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        // Keeps a tap on the tool's empty parts from reaching the editor under it.
                        .pointerInput(Unit) { detectTapGestures {} },
                ) {
                    // The checks keep a tap landing while the tool fades away from closing or applying twice.
                    StickersTool(
                        sourceImage = opened.image,
                        initialStickers = opened.stickers,
                        selectedStickerId = opened.selectedStickerId,
                        texts = opened.texts,
                        isPremium = isPremium,
                        onClose = { if (current === opened) onClose() },
                        onApply = { stickers -> if (current === opened) onApply(stickers) },
                        onOpenPremium = onOpenPremium,
                    )
                }
            }
        }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = session != null,
        onBackCompleted = onClose,
    )
}
