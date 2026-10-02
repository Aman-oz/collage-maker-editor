package org.example.project.ui.collage

import org.example.project.ui.templates.MaxSlotZoom
import org.example.project.ui.templates.SlotTransform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.example.project.ui.collage.geom.TemplateItem

class CollageMathTest {

    @Test
    fun fitAspect_wideRatioInTallBox_isWidthBound() {
        assertEquals(320f to 180f, fitAspect(320f, 500f, 16f / 9f))
    }

    @Test
    fun fitAspect_tallRatioInWideBox_isHeightBound() {
        assertEquals(225f to 400f, fitAspect(500f, 400f, 9f / 16f))
    }

    @Test
    fun fitAspect_squareInSquare_fillsBox() {
        assertEquals(300f to 300f, fitAspect(300f, 300f, 1f))
    }

    @Test
    fun fitAspect_emptyBox_isZero() {
        assertEquals(0f to 0f, fitAspect(0f, 300f, 1f))
    }

    @Test
    fun collageOutputSize_keepsLongSideAndRatio() {
        assertEquals(1080 to 1080, collageOutputSize(CollageRatio.Square.aspect))
        assertEquals(1080 to 608, collageOutputSize(CollageRatio.Landscape.aspect))
        assertEquals(864 to 1080, collageOutputSize(CollageRatio.Portrait.aspect))
        assertEquals(608 to 1080, collageOutputSize(CollageRatio.Story.aspect))
    }

    @Test
    fun slotPhotoSize_coverFitsThenScales() {
        // A 2:1 photo in a square slot is height-bound at 1x, then doubled at 2x.
        assertEquals(200f to 100f, slotPhotoSize(400f, 200f, 100f, 100f, 1f))
        assertEquals(400f to 200f, slotPhotoSize(400f, 200f, 100f, 100f, 2f))
    }

    @Test
    fun collageGesture_canPanTheCroppedAxisAtFitScale() {
        // The 2:1 photo overhangs a square slot by 50% of the slot width on each side.
        val result = SlotTransform().applyCollageGesture(30f, 30f, 1f, 400f, 200f, 100f, 100f)
        assertEquals(0.3f, result.offsetX, 1e-4f)
        assertEquals(0f, result.offsetY, 1e-4f, "the fitted axis has no overhang to pan into")
    }

    @Test
    fun collageGesture_panIsClampedSoPhotoCoversSlot() {
        val result = SlotTransform().applyCollageGesture(1000f, -1000f, 1f, 400f, 200f, 100f, 100f)
        assertEquals(0.5f, result.offsetX, 1e-4f)
        assertEquals(0f, result.offsetY, 1e-4f)
    }

    @Test
    fun collageGesture_zoomIsClampedToRange() {
        assertEquals(1f, SlotTransform().applyCollageGesture(0f, 0f, 0.5f, 100f, 100f, 100f, 100f).scale)
        assertEquals(MaxSlotZoom, SlotTransform(scale = 4f).applyCollageGesture(0f, 0f, 2f, 100f, 100f, 100f, 100f).scale)
    }

    @Test
    fun clampedToSlot_pullsStaleOffsetBackWhenSlotChanges() {
        // Panned fully right in a square slot, then the slot turns 2:1 wide: no horizontal overhang left.
        val stale = SlotTransform(offsetX = 0.5f)
        assertEquals(0f, stale.clampedToSlot(400f, 200f, 200f, 100f).offsetX, 1e-4f)
    }

    @Test
    fun isPremiumCollageEdit_defaultCollageIsFree() {
        val default = CollageState(template = TemplateItem())
        assertFalse(isPremiumCollageEdit(default.space, default.ratio))
    }

    @Test
    fun isPremiumCollageEdit_borderWidthBandOrPortraitRatio() {
        assertTrue(isPremiumCollageEdit(2f, CollageRatio.Square))
        assertTrue(isPremiumCollageEdit(5f, CollageRatio.Square))
        assertFalse(isPremiumCollageEdit(1.9f, CollageRatio.Square))
        assertFalse(isPremiumCollageEdit(5.1f, CollageRatio.Story))
        assertTrue(isPremiumCollageEdit(20f, CollageRatio.Portrait))
    }
}
