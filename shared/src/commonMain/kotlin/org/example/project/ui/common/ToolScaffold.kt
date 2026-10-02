package org.example.project.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
private const val DropMillis = 260
private const val SettleMillis = 460

/** The tool's stage fades over the editor's photo as that comes to rest, and off it before it moves. */
private const val StageFadeMillis = 180

/** The first beat: the outgoing bar accelerates off the bottom of the screen. */
private fun dropSpec(): AnimationSpec<Float> = tween(DropMillis, easing = FastOutLinearInEasing)

/** The second beat: the incoming bar, and the layout around it, decelerates into place. */
private fun settleSpec(): AnimationSpec<Float> = tween(SettleMillis, easing = LinearOutSlowInEasing)

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
                delay((SettleMillis - StageFadeMillis).toLong())
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
 * panel ([ToolScaffoldScope.ToolPanel]: ✕ / title / ✓ over the tool's controls) under it.
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(if (host == null) Modifier.background(surface) else Modifier)
            .windowInsetsPadding(insets),
    ) {
        val scope = remember(this, host, surface) { ToolScaffoldScope(this, host, surface) }
        scope.content()
    }
}

@Stable
internal class ToolScaffoldScope(
    private val column: ColumnScope,
    private val host: InlineToolHostState<*>?,
    private val surface: Color,
) : ColumnScope by column {

    /** Marks the tool's stage: it takes all the room above the panel. Add the stage's own look after it. */
    fun Modifier.toolStage(): Modifier = with(column) { this@toolStage.fillMaxWidth().weight(1f) }
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
        // Slides by its own height plus the system bar under it, so while hidden it is fully off
        // screen rather than showing through a translucent navigation bar. Placed at an offset
        // rather than moved by a graphicsLayer, which would make the Liquid Glass buttons sample
        // the wrong backdrop.
        val bottomInsetPx = WindowInsets.safeDrawing.getBottom(LocalDensity.current)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (host != null) {
                        Modifier
                            .onSizeChanged { host.panelHeightPx = it.height }
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    val hidden = 1f - host.panel.value
                                    placeable.place(0, (hidden * (placeable.height + bottomInsetPx)).roundToInt())
                                }
                            }
                    } else {
                        Modifier
                    },
                )
                .background(surface),
        ) {
            ToolTopBar(title = title, onClose = onClose, onDone = onDone, doneEnabled = doneEnabled)
            content()
        }
    }
}
