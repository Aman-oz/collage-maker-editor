package org.example.project.ui.text

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.example.project.i18n.tr
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_arrow_circle_minus
import photocollagemaker.shared.generated.resources.ic_arrow_circle_plus
import photocollagemaker.shared.generated.resources.ic_arrow_in_circle

/**
 * The Nudge panel's buttons, in order: four that move the text a step ([TextNudgeStep]) and two that
 * turn it a degree. The arrows share one up-pointing icon, turned by [iconDegrees].
 */
private enum class TextNudge(
    private val englishLabel: String,
    val icon: DrawableResource,
    val iconDegrees: Float,
    val apply: (TextItem) -> TextItem,
) {
    Up("Move up", Res.drawable.ic_arrow_in_circle, 0f, { nudgeTextItem(it, Offset(0f, -TextNudgeStep)) }),
    Down("Move down", Res.drawable.ic_arrow_in_circle, 180f, { nudgeTextItem(it, Offset(0f, TextNudgeStep)) }),
    Left("Move left", Res.drawable.ic_arrow_in_circle, -90f, { nudgeTextItem(it, Offset(-TextNudgeStep, 0f)) }),
    Right("Move right", Res.drawable.ic_arrow_in_circle, 90f, { nudgeTextItem(it, Offset(TextNudgeStep, 0f)) }),
    TurnLeft("Rotate left", Res.drawable.ic_arrow_circle_minus, 0f, { turnTextItem(it, -TextNudgeDegrees) }),
    TurnRight("Rotate right", Res.drawable.ic_arrow_circle_plus, 0f, { turnTextItem(it, TextNudgeDegrees) }),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * The Nudge panel: ✕ / Nudge and the text's turn / ✓ over a row of buttons that move the active text
 * a little at a time and turn it a degree at a time; held down, they keep going. Its ✕ puts the text
 * back where it was when the panel opened.
 */
@Composable
internal fun TextNudgePanel(
    item: TextItem?,
    onChange: ((TextItem) -> TextItem) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val degrees = normalizeDegrees(item?.rotationDegrees ?: 0f).roundToInt()
    val sideBySide = LocalTextPanelSideBySide.current
    val button = @Composable { nudge: TextNudge, size: Dp ->
        RepeatingIconButton(
            painter = painterResource(nudge.icon),
            contentDescription = nudge.label,
            iconDegrees = nudge.iconDegrees,
            onClick = { onChange(nudge.apply) },
            size = size,
        )
    }
    TextPanelColumn(
        title = tr("Nudge {0}°", degrees),
        onClose = onClose,
        onDone = onDone,
        modifier = modifier,
        placement = TextPanelPlacement.Center,
    ) {
        if (sideBySide) {
            // The tall column has room for a pad: the arrows where they point, the turns above.
            DirectionPad(
                rows = listOf(
                    listOf(TextNudge.TurnLeft, TextNudge.Up, TextNudge.TurnRight),
                    listOf(TextNudge.Left, null, TextNudge.Right),
                    listOf(null, TextNudge.Down, null),
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { button(it, DirectionPadButtonSize) }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (nudge in TextNudge.entries) button(nudge, 44.dp)
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/** A [DirectionPad]'s buttons, larger than a row's: the column beside the photo has the room. */
internal val DirectionPadButtonSize = 56.dp

/**
 * [rows] of buttons in a grid of equal cells, a null leaving its cell empty: arrows laid out where
 * they point. The Nudge panel's and the Shadow panel's Distance arrows, beside the photo.
 */
@Composable
internal fun <T : Any> DirectionPad(rows: List<List<T?>>, modifier: Modifier = Modifier, cell: @Composable (T) -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                for (value in row) {
                    Box(modifier = Modifier.size(DirectionPadButtonSize), contentAlignment = Alignment.Center) {
                        if (value != null) cell(value)
                    }
                }
            }
        }
    }
}

/** How long a button is held before it starts repeating, and how often it repeats then. */
private const val RepeatDelayMillis = 400L
private const val RepeatIntervalMillis = 50L

/**
 * A round icon button that fires [onClick] on touch, and keeps firing while held after
 * [RepeatDelayMillis] — for nudging, where one step is small. Shared by the Nudge panel and the
 * Shadow panel's Distance arrows. Its icon, about three quarters of its [size], is turned by
 * [iconDegrees].
 */
@Composable
internal fun RepeatingIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconDegrees: Float = 0f,
    size: Dp = 44.dp,
) {
    val bounce = rememberSpringBounce()
    val currentOnClick by rememberUpdatedState(onClick)
    Box(
        modifier = modifier
            .springBounce(bounce)
            .size(size)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                onClick {
                    currentOnClick()
                    true
                }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        bounce.press()
                        currentOnClick()
                        val repeat = launch {
                            delay(RepeatDelayMillis)
                            while (isActive) {
                                currentOnClick()
                                delay(RepeatIntervalMillis)
                            }
                        }
                        waitForUpOrCancellation()
                        repeat.cancel()
                        bounce.release()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size * 0.73f).rotate(iconDegrees),
        )
    }
}
