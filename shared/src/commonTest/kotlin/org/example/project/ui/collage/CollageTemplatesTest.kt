package org.example.project.ui.collage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollageTemplatesTest {

    @Test
    fun everyImageCount_hasAtLeastOneTemplate() {
        for (count in CollageImageCounts) {
            val templates = CollageTemplates.getTemplatesByImageCount(count)
            assertTrue(templates.isNotEmpty(), "no templates for $count photos")
            assertTrue(templates.all { it.imageCount == count }, "template imageCount mismatch for $count photos")
        }
    }

    @Test
    fun everyTemplate_hasExactlyImageCountSlots() {
        for (count in CollageImageCounts) {
            for (template in CollageTemplates.getTemplatesByImageCount(count)) {
                assertEquals(count, template.slots.size, "template ${template.id} has the wrong slot count")
            }
        }
    }

    @Test
    fun everySlot_hasNormalizedBoundsWithinTheUnitSquare() {
        for (count in CollageImageCounts) {
            for (template in CollageTemplates.getTemplatesByImageCount(count)) {
                for (slot in template.slots) {
                    assertTrue(slot.left in 0f..1f, "${template.id} slot ${slot.index} left out of range")
                    assertTrue(slot.top in 0f..1f, "${template.id} slot ${slot.index} top out of range")
                    assertTrue(slot.right in 0f..1f, "${template.id} slot ${slot.index} right out of range")
                    assertTrue(slot.bottom in 0f..1f, "${template.id} slot ${slot.index} bottom out of range")
                    assertTrue(slot.width > 0f, "${template.id} slot ${slot.index} has non-positive width")
                    assertTrue(slot.height > 0f, "${template.id} slot ${slot.index} has non-positive height")
                }
            }
        }
    }

    @Test
    fun everyTemplateId_isUnique() {
        val allIds = CollageImageCounts.flatMap { CollageTemplates.getTemplatesByImageCount(it) }.map { it.id }
        assertEquals(allIds.size, allIds.toSet().size, "duplicate template id")
    }

    @Test
    fun findById_locatesAnExistingTemplate() {
        val template = CollageTemplates.getTemplatesByImageCount(2).first()
        assertEquals(template, CollageTemplates.findById(template.id))
    }

    @Test
    fun findById_returnsNullForAnUnknownId() {
        assertEquals(null, CollageTemplates.findById("does_not_exist"))
    }
}
