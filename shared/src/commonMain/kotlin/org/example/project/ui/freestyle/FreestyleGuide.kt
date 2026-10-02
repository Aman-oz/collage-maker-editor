package org.example.project.ui.freestyle

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BorderClear
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Pinch
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.example.project.i18n.tr
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.theme.Brand
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_background
import photocollagemaker.shared.generated.resources.ic_undo

/**
 * One point of the freestyle guide. The texts are the English keys, translated where they are
 * shown; [icon] is composable so a point can use a drawable resource, like the tool row does.
 */
private class GuidePoint(val title: String, val description: String, val icon: @Composable () -> ImageVector)

/**
 * The gestures first, then the tools in the order of the tool row. Each icon is the one the editor
 * itself shows for that thing (the corner handle, the delete button, the tool's own icon), so the
 * guide points at what is on screen rather than describing it.
 */
private val GuidePoints = listOf(
    GuidePoint("Move", "Drag a photo, sticker or text with one finger to move it.") { Icons.Outlined.OpenWith },
    GuidePoint("Pinch to resize", "Pinch with two fingers to make it bigger or smaller, and twist to rotate it.") {
        Icons.Outlined.Pinch
    },
    GuidePoint("Corner handle", "Tap an item, then drag the handle at its corner to resize and rotate it.") {
        Icons.Filled.OpenInFull
    },
    GuidePoint("Delete", "Tap an item, then the red button at its corner to remove it.") { Icons.Filled.Delete },
    GuidePoint("Background", "Pick a color or a gradient behind your photos, or mix your own color.") {
        vectorResource(Res.drawable.ic_background)
    },
    GuidePoint("Stickers", "Tap an emoji to place it on the canvas.") { Icons.Outlined.EmojiEmotions },
    GuidePoint("Border", "Set the border width and corner radius. Tap a photo first to change only that one.") {
        Icons.Outlined.BorderClear
    },
    GuidePoint("Text", "Add text, then choose its font, color and background. Double-tap text to edit it.") {
        Icons.Outlined.TextFields
    },
    GuidePoint("Add Image", "Add another photo. Tap a photo first to replace it instead.") {
        Icons.Outlined.AddPhotoAlternate
    },
    GuidePoint("Undo", "Undo and redo step back and forward through your changes.") {
        vectorResource(Res.drawable.ic_undo)
    },
)

/** Dark enough to read white text over any canvas, clear enough that the editor shows through. */
private val GuideScrim = Color.Black.copy(alpha = 0.84f)

/**
 * The freestyle editor's usage guide: a full-screen, slightly see-through layer over the editor,
 * opened from the top bar's "?" button. It lists [GuidePoints] as an icon with a title and one
 * line of how-to, and closes on ✕, "Got it" or system back. It is always white-on-dark, whatever
 * the theme, since it sits on its own dark scrim rather than on the themed surface.
 */
@Composable
internal fun FreestyleGuideOverlay(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = onDismiss,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GuideScrim)
            // Swallows every tap, so nothing under the guide (a layer, a tool) reacts through it.
            .pointerInput(Unit) { detectTapGestures { } }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("How to use Freestyle"),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            GlassTopBarButton(
                icon = Icons.Filled.Close,
                contentDescription = tr("Close"),
                onClick = onDismiss,
                contentColor = Color.White,
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            GuidePoints.forEach { GuideRow(it) }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(50.dp),
            shape = RoundedCornerShape(50),
            // The brand violet, not the dark scheme's pastel primary, like every Done button.
            colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White),
        ) {
            Text(text = tr("Got it"), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun GuideRow(point: GuidePoint) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = point.icon(), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = tr(point.title), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = tr(point.description), color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Preview
@Composable
private fun FreestyleGuideOverlayPreview() {
    ThemePreviews {
        FreestyleGuideOverlay(onDismiss = {})
    }
}
