package org.example.project.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import org.example.project.i18n.tr
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_delete
import photocollagemaker.shared.generated.resources.ic_duplicate
import photocollagemaker.shared.generated.resources.ic_reset

/** The purple of the selection's handles. */
private val HandleColor = Color(0xFF8B5CF6)

/** Each handle's touch area; the dot or bar drawn in it is much smaller. */
private val HandleTouchSize = 30.dp
private val CornerDotSize = 9.dp
private val EdgeBarLength = 16.dp
private val EdgeBarThickness = 5.dp

/**
 * An invisible box laid exactly over a text (turned with it) that takes its gestures: tap selects
 * (and a tap on the selected text types in it), one finger drags, two fingers pinch-zoom and twist.
 * When [selected] it draws the selection: a thin rectangle with a dot on each corner, which scales
 * the text, and a bar in the middle of each side, which moves that side to give the words more room
 * or less. A [locked] text only takes the tap, and shows its rectangle without the handles. The
 * Stickers tool uses it too, without the side bars ([showEdgeHandles]): a sticker has no text area.
 *
 * Because the box itself is turned, a drag on a handle arrives in the text's own frame, which is
 * what [onEdgeDrag] wants; [onMove] pans are turned back by [rotationDegrees] into photo coordinates.
 */
@Composable
internal fun TextSelectionBox(
    selected: Boolean,
    locked: Boolean,
    boxSizePx: IntSize,
    rotationDegrees: Float,
    onTap: () -> Unit,
    onMove: (pan: Offset) -> Unit,
    onPinch: (zoom: Float, rotationDelta: Float) -> Unit,
    onScaleStart: () -> Unit,
    onScale: (factorFromStart: Float) -> Unit,
    onEdgeDrag: (edge: TextBoxEdge, delta: Offset) -> Unit,
    modifier: Modifier = Modifier,
    showEdgeHandles: Boolean = true,
) {
    val density = LocalDensity.current
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnPinch by rememberUpdatedState(onPinch)
    val currentLocked by rememberUpdatedState(locked)
    val currentRotation by rememberUpdatedState(rotationDegrees)

    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotationDegrees }
            .size(with(density) { boxSizePx.width.toDp() }, with(density) { boxSizePx.height.toDp() })
            .then(if (selected) Modifier.selectionOutline() else Modifier)
            .pointerInput(Unit) { detectTapGestures { currentOnTap() } }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (currentLocked) return@detectTransformGestures
                    currentOnMove(rotateVector(pan, currentRotation))
                    if (zoom != 1f || rotation != 0f) currentOnPinch(zoom, rotation)
                }
            },
    ) {
        if (selected && !locked) {
            for (corner in listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)) {
                CornerHandle(
                    alignment = corner,
                    boxSizePx = boxSizePx,
                    onScaleStart = onScaleStart,
                    onScale = onScale,
                )
            }
            if (showEdgeHandles) for (edge in TextBoxEdge.entries) {
                EdgeHandle(edge = edge, onDrag = { onEdgeDrag(edge, it) })
            }
        }
    }
}

/** The selection rectangle: a white hairline with a faint dark one outside it, readable on any photo. */
private fun Modifier.selectionOutline(): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    drawRect(Color.Black.copy(alpha = 0.25f), topLeft = Offset(-stroke, -stroke), size = size.copy(size.width + stroke * 2, size.height + stroke * 2), style = Stroke(stroke))
    drawRect(Color.White.copy(alpha = 0.95f), style = Stroke(stroke))
}

/**
 * A corner dot that scales the text around its centre. It follows the finger as a vector from the
 * centre, in the box's own frame: starting at the corner, it gathers each drag delta, and its length
 * against the start's is the scale. Turning is left to the pinch.
 */
@Composable
private fun BoxScope.CornerHandle(
    alignment: Alignment,
    boxSizePx: IntSize,
    onScaleStart: () -> Unit,
    onScale: (Float) -> Unit,
) {
    val currentBoxSize by rememberUpdatedState(boxSizePx)
    val currentOnScaleStart by rememberUpdatedState(onScaleStart)
    val currentOnScale by rememberUpdatedState(onScale)
    val (signX, signY) = when (alignment) {
        Alignment.TopStart -> -1f to -1f
        Alignment.TopEnd -> 1f to -1f
        Alignment.BottomStart -> -1f to 1f
        else -> 1f to 1f
    }
    HandleSlot(
        modifier = Modifier
            .align(alignment)
            .offset(x = HandleTouchSize / 2 * signX, y = HandleTouchSize / 2 * signY)
            .pointerInput(Unit) {
                var start = Offset.Zero
                var finger = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        start = Offset(signX * currentBoxSize.width / 2f, signY * currentBoxSize.height / 2f)
                        finger = start
                        currentOnScaleStart()
                    },
                ) { change, dragAmount ->
                    change.consume()
                    finger += dragAmount
                    val startDistance = start.getDistance()
                    if (startDistance > 0f) currentOnScale(finger.getDistance() / startDistance)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .size(CornerDotSize)
                .clip(CircleShape)
                .background(HandleColor)
                .border(1.dp, Color.White, CircleShape),
        )
    }
}

/** A bar in the middle of a side; dragging it moves that side. */
@Composable
private fun BoxScope.EdgeHandle(edge: TextBoxEdge, onDrag: (Offset) -> Unit) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val horizontal = edge == TextBoxEdge.Top || edge == TextBoxEdge.Bottom
    val (alignment, offsetX, offsetY) = when (edge) {
        TextBoxEdge.Left -> Triple(Alignment.CenterStart, -HandleTouchSize / 2, 0.dp)
        TextBoxEdge.Right -> Triple(Alignment.CenterEnd, HandleTouchSize / 2, 0.dp)
        TextBoxEdge.Top -> Triple(Alignment.TopCenter, 0.dp, -HandleTouchSize / 2)
        TextBoxEdge.Bottom -> Triple(Alignment.BottomCenter, 0.dp, HandleTouchSize / 2)
    }
    HandleSlot(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = if (horizontal) EdgeBarLength else EdgeBarThickness,
                    height = if (horizontal) EdgeBarThickness else EdgeBarLength,
                )
                .clip(RoundedCornerShape(50))
                .background(HandleColor)
                .border(1.dp, Color.White, RoundedCornerShape(50)),
        )
    }
}

/** A touch area centred on the handle's point, so a small dot is still easy to grab. */
@Composable
private fun HandleSlot(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.size(HandleTouchSize), contentAlignment = Alignment.Center) { content() }
}

/** The toolbar's height, which the tool needs to place it above the selection before it is measured. */
internal val TextToolbarHeight = 40.dp
private val ToolbarShape = RoundedCornerShape(50)

/**
 * The glass pill over the selected text: Duplicate, Delete, and ⋯ for the rest ([TextMoreMenu]),
 * which reports where it is so the menu can unfold from it. The glass samples the photo
 * ([photoLiquidState]), the one thing under it.
 */
@Composable
internal fun TextSelectionToolbar(
    photoLiquidState: LiquidState,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMore: (anchorInRoot: Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    var moreBounds by remember { mutableStateOf(Rect.Zero) }
    Row(
        modifier = modifier
            .height(TextToolbarHeight)
            .clip(ToolbarShape)
            .liquid(photoLiquidState) {
                shape = ToolbarShape
                frost = 10.dp
                curve = 0.35f
                refraction = 0.3f
                edge = 0.6f
                dispersion = 0.15f
                saturation = 1.2f
                tint = Color.Black.copy(alpha = 0.22f)
            }
            .border(0.75.dp, Color.White.copy(alpha = 0.35f), ToolbarShape)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ToolbarButton(vectorResource(Res.drawable.ic_duplicate), tr("Duplicate"), onDuplicate)
        ToolbarButton(vectorResource(Res.drawable.ic_delete), tr("Delete"), onDelete)
        ToolbarButton(
            icon = Icons.Outlined.Pending,
            contentDescription = tr("More"),
            onClick = { onMore(moreBounds) },
            modifier = Modifier.onGloballyPositioned { moreBounds = it.boundsInRoot() },
        )
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(21.dp))
    }
}

/** The ⋯ menu's actions, top to bottom. */
internal enum class TextMoreAction { Duplicate, Reset, Copy, Paste, Lock, Delete }

private val MoreMenuWidth = 180.dp
private val MoreMenuRowHeight = 42.dp

/**
 * The ⋯ menu: a [GlassGenieMenu] unfolding down from the ⋯ button ([anchor]), each row its label and
 * icon, Delete in red. Lock reads Unlock on a [locked] text; Paste is dimmed while nothing is copied.
 */
@Composable
internal fun TextMoreMenu(
    open: Boolean,
    anchor: Rect?,
    liquidState: LiquidState,
    locked: Boolean,
    canPaste: Boolean,
    onAction: (TextMoreAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val actions = TextMoreAction.entries
    GlassGenieMenu(
        open = open,
        anchor = anchor,
        liquidState = liquidState,
        rowCount = actions.size,
        rowHeight = MoreMenuRowHeight,
        width = MoreMenuWidth,
        direction = GenieDirection.Down,
        onDismiss = onDismiss,
    ) { cascade ->
        actions.forEachIndexed { index, action ->
            val (label, icon) = when (action) {
                TextMoreAction.Duplicate -> tr("Duplicate") to vectorResource(Res.drawable.ic_duplicate)
                TextMoreAction.Reset -> tr("Reset") to vectorResource(Res.drawable.ic_reset)
                TextMoreAction.Copy -> tr("Copy") to Icons.Outlined.ContentCopy
                TextMoreAction.Paste -> tr("Paste") to Icons.Outlined.ContentPaste
                TextMoreAction.Lock -> if (locked) tr("Unlock") to Icons.Outlined.LockOpen else tr("Lock") to Icons.Outlined.Lock
                TextMoreAction.Delete -> tr("Delete") to vectorResource(Res.drawable.ic_delete)
            }
            val enabled = open && (action != TextMoreAction.Paste || canPaste)
            val color = if (action == TextMoreAction.Delete) Color(0xFFE53935) else scheme.onSurface
            MenuRow(
                label = label,
                icon = icon,
                color = color,
                height = MoreMenuRowHeight,
                showDivider = index > 0,
                enabled = enabled,
                dimmed = action == TextMoreAction.Paste && !canPaste,
                onClick = { onAction(action) },
                // Top-down: the row nearest the ⋯ comes first.
                modifier = Modifier.cascadeIn(cascade, index, actions.size, riseDp = -10f),
            )
        }
    }
}

/** One row of a ⋯ menu: [label] and [icon] in [color], over a hairline when [showDivider]. */
@Composable
internal fun MenuRow(
    label: String,
    icon: ImageVector,
    color: Color,
    height: Dp,
    showDivider: Boolean,
    enabled: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        // Inside the row rather than between rows, so the rows add up to the menu's height.
        if (showDivider) {
            HorizontalDivider(color = scheme.onSurface.copy(alpha = 0.12f), modifier = Modifier.align(Alignment.TopCenter))
        }
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .alpha(if (dimmed) 0.4f else 1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Spacer(modifier = Modifier.weight(1f))
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
    }
}
