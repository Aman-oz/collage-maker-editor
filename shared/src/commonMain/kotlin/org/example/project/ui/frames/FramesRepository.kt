package org.example.project.ui.frames

import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.TemplateFrame
import org.example.project.ui.templates.TemplatesRepository

/**
 * Catalog source for the Frames screen. It serves the Templates catalog for now: the Frames data
 * hasn't been wired up yet, and routing it through its own repository means only this class changes
 * when it is — [FramesViewModel] and the screens already treat Frames as a separate feature.
 */
class FramesRepository(private val templates: TemplatesRepository) {

    suspend fun getCategories(): List<TemplateCategory> = templates.getCategories()

    suspend fun getFrames(categoryId: String): List<TemplateFrame> = templates.getTemplates(categoryId)
}
