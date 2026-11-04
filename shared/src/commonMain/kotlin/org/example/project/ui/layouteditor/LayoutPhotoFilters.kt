package org.example.project.ui.layouteditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.example.project.ui.filter.mixFiltered
import org.example.project.ui.filter.photoFilterById
import org.example.project.ui.filter.renderFilter
import org.example.project.ui.filter.scaledToFit

/**
 * The layout editor's photos through its filter ([LayoutEdit.filterId]). A filter can't be a
 * `ColorFilter` (see `runFilterSteps`), so each photo is filtered once, cut to [LayoutBakeLongSide]
 * (a slot is never larger in the bake), and the copy drawn in its place by the preview and the bake
 * alike. The copies are kept for the last few photo/filter pairs, so the preview, a tool's snapshot
 * and Done share them.
 */
internal object LayoutPhotoFilters {
    private const val MaxKept = 12

    // The photo is compared by identity: ImageBitmap has no equals of its own.
    private data class Key(val photo: ImageBitmap, val filterId: String, val intensity: Float)

    private val mutex = Mutex()
    private val kept = LinkedHashMap<Key, ImageBitmap>()

    /** Each photo of [edit] mapped to its filtered copy; empty with no filter. */
    suspend fun of(edit: LayoutEdit): Map<ImageBitmap, ImageBitmap> {
        val filter = photoFilterById(edit.filterId)?.takeUnless { it.isNoneOption } ?: return emptyMap()
        if (edit.filterIntensity <= 0f) return emptyMap()
        return edit.photos.values.map { it.image }.distinct().associateWith { photo ->
            val key = Key(photo, filter.id, edit.filterIntensity)
            mutex.withLock { kept.remove(key)?.also { kept[key] = it } } ?: withContext(Dispatchers.Default) {
                val scaled = scaledToFit(photo, LayoutBakeLongSide)
                mixFiltered(scaled, renderFilter(scaled, filter), edit.filterIntensity)
            }.also { copy ->
                mutex.withLock {
                    kept[key] = copy
                    while (kept.size > MaxKept) kept.remove(kept.keys.first())
                }
            }
        }
    }
}

/** [LayoutPhotoFilters.of] for the preview: empty until the copies are ready, then redone as the filter or photos change. */
@Composable
internal fun rememberFilteredLayoutPhotos(edit: LayoutEdit): Map<ImageBitmap, ImageBitmap> {
    val photos = edit.photos.values.map { it.image }
    return produceState(emptyMap(), edit.filterId, edit.filterIntensity, photos) {
        value = LayoutPhotoFilters.of(edit)
    }.value
}
