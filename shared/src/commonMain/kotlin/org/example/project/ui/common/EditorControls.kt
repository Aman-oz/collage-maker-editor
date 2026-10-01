package org.example.project.ui.common

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_before_after

/** Small circular icon button used throughout the editor's top bars (close, undo, redo, back). */
@Composable
internal fun EditorCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val bounce = rememberSpringBounce()
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = bounce.interactionSource,
        modifier = modifier
            .springBounce(bounce)
            .size(36.dp)
            .clip(CircleShape)
            .background(EditorControlBackground),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) EditorIconTint else EditorIconTint.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Undo / redo button shared by every screen: a bare glyph with no background, so it sits on any
 * bar. The circle clip only shapes the ripple. [tint] defaults to the theme's content colour.
 */
@Composable
internal fun UndoRedoButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = modifier
            .springBounce(bounce)
            .size(UndoRedoButtonSize)
            .clip(CircleShape)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(UndoRedoIconSize),
        )
    }
}

internal val UndoRedoButtonSize = 40.dp
internal val UndoRedoIconSize = 26.dp

/** Press-and-hold button that reports its pressed state — used to preview the original, unedited photo. */
@Composable
internal fun CompareButton(onPressedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = modifier
            .springBounce(bounce)
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EditorControlBackground)
            .pointerInput(onPressedChange) {
                detectTapGestures(
                    onPress = {
                        bounce.press()
                        onPressedChange(true)
                        tryAwaitRelease()
                        onPressedChange(false)
                        bounce.release()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_before_after),
            contentDescription = "Press and hold to compare with the original",
            tint = EditorIconTint,
            modifier = Modifier.size(18.dp),
        )
    }
}
