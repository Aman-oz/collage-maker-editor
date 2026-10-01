package org.example.project.ui.pip

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs the Pip picker (the LAS app's `TemplateActivity`): the bundled [templates] and their
 * thumbnails, decoded in grid order so the first rows fill in first.
 */
class PipViewModel(private val assetLoader: PipAssetLoader) : ViewModel() {

    val templates: List<PipTemplate> = PipTemplates

    private val _previews = MutableStateFlow(emptyMap<String, ImageBitmap>())

    /** Decoded thumbnails by template name; a template missing here is still loading. */
    val previews: StateFlow<Map<String, ImageBitmap>> = _previews.asStateFlow()

    init {
        viewModelScope.launch {
            for (template in templates) {
                val preview = assetLoader.preview(template) ?: continue
                _previews.update { it + (template.name to preview) }
            }
        }
    }
}
