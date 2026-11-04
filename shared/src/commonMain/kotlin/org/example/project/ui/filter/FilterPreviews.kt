package org.example.project.ui.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [source] scaled to fit [maxSide], worked out off the main thread so a tool's first frame stays light; null until then. */
@Composable
internal fun rememberScaledImage(source: ImageBitmap?, maxSide: Int): ImageBitmap? =
    produceState<ImageBitmap?>(null, source, maxSide) {
        value = source?.let { withContext(Dispatchers.Default) { scaledToFit(it, maxSide) } }
    }.value

/** A tool's preview as rendered for [key] (the pick it shows). */
internal class RenderedImage(val key: Any?, val image: ImageBitmap)

/**
 * [source] through [render], redone whenever [key] changes; null until the first result. Meanwhile
 * the previous result stays (its [RenderedImage.key] tells it apart), so the preview doesn't flash
 * the plain photo between two picks.
 */
@Composable
internal fun rememberRendered(source: ImageBitmap?, key: Any?, render: suspend (ImageBitmap) -> ImageBitmap): RenderedImage? =
    produceState<RenderedImage?>(null, source, key) {
        value = source?.let { RenderedImage(key, render(it)) }
    }.value

/**
 * The chips' thumbnails, by id, kept while the tool is open so switching tabs doesn't redo them.
 * They render on their own two threads, so a strip of them filling in doesn't hold up the preview.
 */
@Stable
internal class FilterThumbnails {
    private val rendered = mutableStateMapOf<String, ImageBitmap>()
    private val dispatcher = Dispatchers.Default.limitedParallelism(2)

    operator fun get(id: String): ImageBitmap? = rendered[id]

    suspend fun render(id: String, source: ImageBitmap, render: suspend (ImageBitmap) -> ImageBitmap) {
        if (id in rendered) return
        rendered[id] = withContext(dispatcher) { render(source) }
    }
}

@Composable
internal fun rememberFilterThumbnails(source: ImageBitmap?): FilterThumbnails = remember(source) { FilterThumbnails() }

/** The thumbnail of [id] from [thumbnails], rendering it the first time it is asked for; null until it is ready. */
@Composable
internal fun FilterThumbnails.thumbnail(id: String, source: ImageBitmap?, render: suspend (ImageBitmap) -> ImageBitmap): ImageBitmap? {
    LaunchedEffect(this, id, source) {
        if (source != null) render(id, source, render)
    }
    return this[id]
}

/** The Filter and Overlay tools' tabs: scrolling labels, the picked one underlined. */
@Composable
internal fun <T> ToolCategoryTabs(
    categories: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        categories.forEach { category ->
            val isSelected = category == selected
            // Width(IntrinsicSize.Max) sizes the column to its label, so the underline below can
            // be a fraction of the text width without measuring it.
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(category) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label(category),
                    color = if (isSelected) scheme.onSurface else scheme.onSurface.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) scheme.primary else Color.Transparent),
                )
            }
        }
    }
}
