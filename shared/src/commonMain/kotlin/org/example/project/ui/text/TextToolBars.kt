package org.example.project.ui.text

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import org.example.project.data.fonts.FontEntry
import org.example.project.i18n.tr
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.ColorPickerFill
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.wholeNumberLabel
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.LocalInlineToolHost
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_color
import photocollagemaker.shared.generated.resources.ic_curve
import photocollagemaker.shared.generated.resources.ic_font_size
import photocollagemaker.shared.generated.resources.ic_fonts
import photocollagemaker.shared.generated.resources.ic_format
import photocollagemaker.shared.generated.resources.ic_keyboard
import photocollagemaker.shared.generated.resources.ic_nudge
import photocollagemaker.shared.generated.resources.ic_opacity
import photocollagemaker.shared.generated.resources.ic_position
import photocollagemaker.shared.generated.resources.ic_shadow
import photocollagemaker.shared.generated.resources.ic_spacing

/**
 * The Text tool's customization options, in the order of the floating bar. [tinted] is false for the
 * colour wheel, whose icon is drawn in its own colours.
 */
internal enum class TextOption(private val englishLabel: String, val icon: DrawableResource, val tinted: Boolean = true) {
    Edit("Edit", Res.drawable.ic_keyboard),
    Font("Font", Res.drawable.ic_fonts),
    FontSize("Font size", Res.drawable.ic_font_size),
    Color("Color", Res.drawable.ic_color, tinted = false),
    Opacity("Opacity", Res.drawable.ic_opacity),
    Format("Format", Res.drawable.ic_format),
    Spacing("Spacing", Res.drawable.ic_spacing),
    Shadow("Shadow", Res.drawable.ic_shadow),
    Curve("Curve", Res.drawable.ic_curve),
    Position("Position", Res.drawable.ic_position),
    Nudge("Nudge", Res.drawable.ic_nudge),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/** How the lines of a text label line up; the order of the keyboard bar's alignment buttons. */
@Serializable
internal enum class TextAlignOption(private val englishLabel: String, val icon: ImageVector, val textAlign: TextAlign) {
    Left("Align left", Icons.AutoMirrored.Filled.FormatAlignLeft, TextAlign.Left),
    Center("Align center", Icons.Filled.FormatAlignCenter, TextAlign.Center),
    Right("Align right", Icons.AutoMirrored.Filled.FormatAlignRight, TextAlign.Right),
    ;

    val label: String get() = tr(englishLabel)
}

private val OptionsBarShape = RoundedCornerShape(24.dp)
private val OptionsBarHeight = 68.dp
private val OptionWidth = 64.dp

/** Each item rises in this long after the one before it, and takes [CascadeRiseMillis] to settle. */
private const val CascadeStaggerMillis = 35
private const val CascadeRiseMillis = 260

/** Runs a [cascadeIn] of [count] items from 0 to 1, after [delayMillis]. */
internal fun cascadeSpec(count: Int, delayMillis: Int = 0) =
    tween<Float>(cascadeMillis(count), delayMillis = delayMillis, easing = LinearEasing)

private fun cascadeMillis(count: Int) = CascadeRiseMillis + CascadeStaggerMillis * (count - 1).coerceAtLeast(0)

/**
 * Item [index] of [count] rising into place, one after another, as [cascade] runs 0 → 1 (see
 * [cascadeSpec]). One Animatable runs the whole row and each item reads its own slice of it in a
 * graphicsLayer, so the cascade redraws without recomposing. [riseDp] is how far below each starts;
 * negative drops it in from above.
 */
internal fun Modifier.cascadeIn(cascade: Animatable<Float, *>, index: Int, count: Int, riseDp: Float = 18f): Modifier =
    graphicsLayer {
        val elapsed = cascade.value * cascadeMillis(count) - index * CascadeStaggerMillis
        val progress = (elapsed / CascadeRiseMillis).coerceIn(0f, 1f)
        // An ease-out cubic, so each item slows into place.
        val eased = 1f - (1f - progress).let { it * it * it }
        alpha = eased
        translationY = (1f - eased) * riseDp * density
        scaleX = 0.85f + 0.15f * eased
        scaleY = scaleX
    }

/**
 * The floating Liquid Glass bar of [TextOption]s, scrolling sideways under a glass ✕ at its end.
 *
 * The options rise in one after another ([cascadeIn]) once the editor has slid the panel most of
 * the way up.
 *
 * Like [org.example.project.ui.common.GlassTopBarButton], the glass carries its own backdrop: a soft
 * gradient in a `liquefiable` sibling layer, since the panel under it is a flat surface that would
 * give the glass nothing to refract.
 */
@Composable
internal fun TextOptionsBar(
    onOptionSelected: (TextOption) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    // Holds the cascade back while the bar itself is still waiting to rise (coming back from the
    // Font panel, which drops away first).
    cascadeDelayMillis: Int = 0,
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    val liquidState = rememberLiquidState()
    val backdrop = Brush.linearGradient(
        if (isLight) {
            listOf(scheme.onSurface.copy(alpha = 0.03f), scheme.onSurface.copy(alpha = 0.1f))
        } else {
            listOf(scheme.onSurface.copy(alpha = 0.14f), scheme.onSurface.copy(alpha = 0.04f))
        },
    )
    val rim = if (isLight) scheme.onSurface.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)

    val options = TextOption.entries
    val cascade = remember { Animatable(0f) }
    val host = LocalInlineToolHost.current
    LaunchedEffect(Unit) {
        if (host != null) snapshotFlow { host.panel.value }.first { it >= 0.5f }
        cascade.animateTo(1f, cascadeSpec(options.size, cascadeDelayMillis))
    }

    val fadeWidthPx = with(LocalDensity.current) { 72.dp.toPx() }
    Box(modifier = modifier.fillMaxWidth().height(OptionsBarHeight)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(OptionsBarShape)
                .liquefiable(liquidState)
                .background(backdrop),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(OptionsBarShape)
                .liquid(liquidState) {
                    shape = OptionsBarShape
                    frost = 6.dp
                    curve = 0.3f
                    refraction = 0.25f
                    edge = 0.5f
                    dispersion = 0.15f
                    saturation = 1.2f
                    tint = Color.White.copy(alpha = if (isLight) 0.45f else 0.08f)
                }
                .border(1.dp, rim, OptionsBarShape),
        )
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .clip(OptionsBarShape)
                // Fades the options out before they reach the ✕, so they scroll away under it rather
                // than showing through its glass.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fadeEnd = size.width - fadeWidthPx * 0.55f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Black,
                            ((fadeEnd - fadeWidthPx) / size.width).coerceIn(0f, 1f) to Color.Black,
                            (fadeEnd / size.width).coerceIn(0f, 1f) to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
            contentPadding = PaddingValues(start = 6.dp, end = 60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(options) { index, option ->
                TextOptionItem(
                    option = option,
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier.cascadeIn(cascade, index, options.size),
                )
            }
        }
        GlassTopBarButton(
            icon = Icons.Filled.Close,
            contentDescription = tr("Close"),
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
        )
    }
}

@Composable
private fun TextOptionItem(option: TextOption, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val bounce = rememberSpringBounce()
    Column(
        modifier = modifier
            .springBounce(bounce)
            .width(OptionWidth)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(option.icon),
            contentDescription = null,
            tint = if (option.tinted) scheme.onSurface else Color.Unspecified,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = option.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The bar that rides on top of the keyboard while text is typed: a ⌄ that trades the keyboard for
 * the full Font panel, a few fonts (recent, then featured; each named in itself), then the colour
 * wheel and the alignments.
 *
 * Its buttons are plain clickables, which don't take focus on a touch screen, so tapping them keeps
 * the text field focused and the keyboard up.
 */
@Composable
internal fun TextKeyboardBar(
    fonts: List<FontEntry>,
    selectedFamily: String?,
    onFontSelected: (FontEntry) -> Unit,
    alignment: TextAlignOption,
    onAlignmentSelected: (TextAlignOption) -> Unit,
    onPickColor: () -> Unit,
    onShowAllFonts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainer)
            // Swallows taps between the buttons, so they don't fall through to the tool underneath.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.onSurface.copy(alpha = 0.12f))
                    .border(1.dp, scheme.onSurface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onShowAllFonts),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = tr("All fonts"),
                    tint = scheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(fonts, key = { it.family }) { font ->
                    FontPill(font = font, selected = font.family == selectedFamily, onClick = { onFontSelected(font) })
                }
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPickColor),
                contentAlignment = Alignment.Center,
            ) {
                ColorPickerFill(Modifier.size(24.dp).clip(CircleShape))
            }
            for (option in TextAlignOption.entries) {
                val selected = option == alignment
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) scheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onAlignmentSelected(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.label,
                        tint = if (selected) scheme.primary else scheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FontPill(font: FontEntry, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val family = rememberLoadedFontFamily(font)
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .background(if (selected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) scheme.primary else scheme.onSurface,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = font.family,
            fontFamily = family ?: FontFamily.Default,
            fontSize = 15.sp,
            color = if (selected) scheme.primary else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp),
        )
    }
}

/**
 * The Font size panel: ✕ / Font size / ✓ and a 0–100 slider ([sliderToTextSize]) at the bottom of
 * the tool's panel, which keeps its one height so the photo stays put.
 */
@Composable
internal fun TextSizePanel(
    value: Float,
    onValueChange: (Float) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        ToolTopBar(title = tr("Font size"), onClose = onClose, onDone = onDone)
        Text(
            text = tr("Size"),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        )
        CenterFillSlider(
            value = value,
            onValueChange = onValueChange,
            range = 0f..100f,
            referenceValue = 0f,
            trackColor = scheme.onSurface.copy(alpha = 0.12f),
            fillColor = scheme.primary,
            thumbColor = scheme.primary,
            thumbWidth = 32.dp,
            thumbHeight = 18.dp,
            horizontalPadding = 12.dp,
            glassThumb = true,
            glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
            valueLabel = ::wholeNumberLabel,
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}
