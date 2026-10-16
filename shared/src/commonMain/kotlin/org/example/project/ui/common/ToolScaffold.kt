package org.example.project.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Padding most tool stages keep around the photo; the editor insets its own photo to match. */
internal val ToolStageInset = 20.dp

/**
 * Opening a tool inside the editor is two beats: the editor's toolbar drops away, then the tool's
 * panel rises in its place. Closing runs the same two beats the other way round.
 */
internal const val ToolDropMillis = 260
internal const val ToolSettleMillis = 460

/** The tool's stage fades over the editor's photo as that comes to rest, and off it before it moves. */
private const val StageFadeMillis = 180

/** The first beat: the outgoing bar accelerates off the bottom of the screen. */
internal fun <T> dropSpec(): FiniteAnimationSpec<T> = tween(ToolDropMillis, easing = FastOutLinearInEasing)

/** The second beat: the incoming bar, and the layout around it, decelerates into place. */
internal fun <T> settleSpec(delayMillis: Int = 0): FiniteAnimationSpec<T> =
    tween(ToolSettleMillis, delayMillis = delayMillis, easing = LinearOutSlowInEasing)

/**
 * Drives a tool that opens *inside* the photo editor instead of on its own screen, and is how the
 * editor and the tool's [ToolScaffold] stay in step. [T] is whatever the editor uses to describe
 * the open tool.
 *
 * The four progress values are plain [Animatable]s run by one coroutine ([show]) rather than
 * `AnimatedVisibility`/`AnimatedContent`, for two reasons:
 * - Everything that changes the *layout* (the bottom area's height, the top bar collapsing, the
 *   photo's inset) rides on the single [layout] value, so the photo makes one eased move in step
 *   with the rising panel instead of being pushed around once per beat.
 * - A tool's first composition can be heavy (blurring or desaturating the photo, thumbnails), and
 *   an animation that starts on such a frame skips ahead by however long the frame took. Here the
 *   tool is mounted hidden between the two beats, while nothing is moving, and only then animated.
 */
@Stable
internal class InlineToolHostState<T : Any>(initial: T? = null) {

    /** The tool currently composed over the editor, or `null`. Lags the requested tool by a beat. */
    var mounted by mutableStateOf(initial)
        private set

    // A host created with a tool already open (the editor coming back from a screen that covered
    // it) starts fully open, rather than replaying the opening.
    private val start = if (initial != null) 1f else 0f

    /** 0 = the editor's own top bar and toolbar are in place, 1 = they are gone. */
    val editorBars = Animatable(start)

    /** 0 = the editor's layout, 1 = the tool's: no top bar, bottom area as tall as the tool panel. */
    val layout = Animatable(start)

    /** 0 = the tool's panel is off the bottom of the screen, 1 = in place. */
    val panel = Animatable(start)

    /** Alpha of the tool's stage over the editor's photo. */
    val stage = Animatable(start)

    /** Reported by [ToolScaffold], so the editor can clear exactly the room the panel takes. */
    var panelHeightPx by mutableIntStateOf(0)

    /** Reported by [ToolScaffold]: how far the tool's stage insets the photo. */
    var photoInset by mutableStateOf(ToolStageInset)

    /**
     * Animates to [target] being the open tool (`null` to close). Call it from a `LaunchedEffect`
     * keyed on the target: a new target cancels the run in progress and picks up from wherever the
     * values are.
     */
    suspend fun show(target: T?) {
        if (mounted != null && mounted !== target) {
            // Let the frame that first draws the tool's result pass before anything moves.
            withFrameNanos { }
            coroutineScope {
                launch { stage.runTo(0f, tween(StageFadeMillis)) }
                launch { panel.runTo(0f, dropSpec()) }
            }
            mounted = null
        }
        if (target == null) {
            coroutineScope {
                launch { layout.runTo(0f, settleSpec()) }
                launch { editorBars.runTo(0f, settleSpec()) }
            }
            return
        }
        editorBars.runTo(1f, dropSpec())
        mounted = target
        repeat(2) { withFrameNanos { } }
        coroutineScope {
            launch { layout.runTo(1f, settleSpec()) }
            launch { panel.runTo(1f, settleSpec()) }
            launch {
                delay((ToolSettleMillis - StageFadeMillis).toLong())
                stage.runTo(1f, tween(StageFadeMillis))
            }
        }
    }

    /** A tween from a value to itself would still wait out its whole duration. */
    private suspend fun Animatable<Float, *>.runTo(target: Float, spec: AnimationSpec<Float>) {
        if (value != target) animateTo(target, spec)
    }
}

/** Set by the photo editor around the tool it hosts; `null` for a tool shown on its own (previews). */
internal val LocalInlineToolHost = compositionLocalOf<InlineToolHostState<*>?> { null }

/**
 * The frame every editor tool is laid out in: a stage ([ToolScaffoldScope.toolStage]) with the tool
 * panel ([ToolScaffoldScope.ToolPanel]: ✕ / title / ✓ over the tool's controls) under it. On a phone
 * on its side ([isSideBySideEditor]) the stage takes the left side at full height instead and the
 * panel sits in an [EditorSidePanelWidth] column on the right, where the photo editor keeps its own
 * top bar and tools, so the photo is never squeezed into the strip a panel underneath would leave.
 *
 * Inside the editor ([LocalInlineToolHost]) it has no background of its own: the stage fades in
 * over the editor's photo and the panel slides up over the editor's toolbar area, both driven by
 * the host.
 *
 * @param photoInset the padding the stage keeps around the photo. The editor moves its own photo to
 * that inset before the stage fades in, so the photo doesn't jump between the two.
 */
@Composable
internal fun ToolScaffold(
    modifier: Modifier = Modifier,
    photoInset: Dp = ToolStageInset,
    insets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable ToolScaffoldScope.() -> Unit,
) {
    val host = LocalInlineToolHost.current
    val surface = MaterialTheme.colorScheme.surface
    if (host != null) SideEffect { host.photoInset = photoInset }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(if (host == null) Modifier.background(surface) else Modifier)
            .windowInsetsPadding(insets),
    ) {
        val sideBySide = isSideBySideEditor(maxWidth, maxHeight)
        val scope = remember(host, surface, sideBySide) { ToolScaffoldScope(host, surface, sideBySide) }
        Layout(content = { scope.content() }, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val stage = measurables.firstOrNull { it.layoutId == ToolStageId }
            val others = measurables.filter { it !== stage }
            if (sideBySide) {
                // The stage on the left at full height; the panel on the right, as tall as the
                // column so its controls can sit at the bottom (anything else stacks from the top).
                val columnWidth = EditorSidePanelWidth.roundToPx().coerceAtMost(width)
                val stagePlaceable = stage?.measure(Constraints.fixed(width - columnWidth, height))
                val minHeight = if (others.size == 1) height else 0
                val placeables = others.map { it.measure(Constraints(columnWidth, columnWidth, minHeight, height)) }
                layout(width, height) {
                    stagePlaceable?.place(0, 0)
                    var y = 0
                    placeables.forEach { placeable ->
                        placeable.place(width - columnWidth, y)
                        y += placeable.height
                    }
                }
            } else {
                // A Column with the stage weighted: everything else takes its own height, the stage the
                // rest, in the order the tool lays them out (stage first, panel under it).
                val placeables = others.associateWith { it.measure(Constraints(width, width, 0, height)) }
                val stageHeight = (height - placeables.values.sumOf { it.height }).coerceAtLeast(0)
                val stagePlaceable = stage?.measure(Constraints.fixed(width, stageHeight))
                layout(width, height) {
                    var y = 0
                    measurables.forEach { measurable ->
                        val placeable = if (measurable === stage) stagePlaceable else placeables[measurable]
                        placeable?.place(0, y)
                        y += placeable?.height ?: 0
                    }
                }
            }
        }
    }
}

/** The [layoutId] that tells [ToolScaffold] which child is the stage. */
private object ToolStageId

/**
 * What a tool lays out in [ToolScaffold]: its stage and its [ToolPanel].
 *
 * @param sideBySide the stage and panel sit side by side (a phone on its side) rather than one over
 * the other; the panel then slides in from the right edge instead of up from the bottom, and fills
 * the column's height. Its controls sit at the bottom of it, under the title bar's free space; a tool
 * with a list of options can give that list `Modifier.weight(1f)` there (a vertical grid, say) to
 * use the space instead.
 */
@Stable
internal class ToolScaffoldScope(
    private val host: InlineToolHostState<*>?,
    private val surface: Color,
    val sideBySide: Boolean,
) {

    /** Marks the tool's stage: it takes all the room the panel leaves. Add the stage's own look after it. */
    fun Modifier.toolStage(): Modifier = this.layoutId(ToolStageId)
        .then(if (host != null) Modifier.graphicsLayer { alpha = host.stage.value } else Modifier)
        // Opaque, so once faded in it fully covers the editor's photo underneath.
        .background(surface)

    /** The tool's bottom panel: the ✕ / [title] / ✓ row with the tool's controls under it. */
    @Composable
    fun ToolPanel(
        title: String,
        onClose: () -> Unit,
        onDone: () -> Unit,
        doneEnabled: Boolean = true,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        PanelFrame {
            ToolTopBar(title = title, onClose = onClose, onDone = onDone, doneEnabled = doneEnabled)
            PanelControls(content)
        }
    }

    /**
     * A panel with no ✕ / title / ✓ row, for a tool whose controls carry their own way out (the Text
     * tool's floating options bar). It slides in and reports its size exactly like the titled one.
     */
    @Composable
    fun ToolPanel(content: @Composable ColumnScope.() -> Unit) {
        PanelFrame { PanelControls(content) }
    }

    /** Beside the stage the controls sit at the bottom of the full-height column. */
    @Composable
    private fun ColumnScope.PanelControls(content: @Composable ColumnScope.() -> Unit) {
        if (sideBySide) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Bottom,
                content = content,
            )
        } else {
            content()
        }
    }

    @Composable
    private fun PanelFrame(content: @Composable ColumnScope.() -> Unit) {
        // Slides by its own height plus the system bar under it, so while hidden it is fully off
        // screen rather than showing through a translucent navigation bar; beside the stage, by its
        // own width plus the inset at the right edge instead. Placed at an offset rather than moved
        // by a graphicsLayer, which would make the Liquid Glass buttons sample the wrong backdrop.
        val density = LocalDensity.current
        val bottomInsetPx = WindowInsets.safeDrawing.getBottom(density)
        val endInsetPx = WindowInsets.safeDrawing.getRight(density, LayoutDirection.Ltr)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (sideBySide) Modifier.fillMaxHeight() else Modifier)
                .then(
                    if (host != null) {
                        Modifier
                            .onSizeChanged { host.panelHeightPx = it.height }
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    val hidden = 1f - host.panel.value
                                    if (sideBySide) {
                                        placeable.place((hidden * (placeable.width + endInsetPx)).roundToInt(), 0)
                                    } else {
                                        placeable.place(0, (hidden * (placeable.height + bottomInsetPx)).roundToInt())
                                    }
                                }
                            }
                    } else {
                        Modifier
                    },
                )
                .background(surface),
        ) {
            // The panel's background spans the screen, but its controls stay phone-width and centred:
            // a slider or chip row stretched across a tablet is hard to use and reads as empty.
            Column(
                modifier = Modifier.readableWidth().then(if (sideBySide) Modifier.fillMaxHeight() else Modifier),
                content = content,
            )
        }
    }
}
