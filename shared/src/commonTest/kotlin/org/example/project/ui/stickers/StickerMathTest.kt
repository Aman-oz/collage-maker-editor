package org.example.project.ui.stickers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.CentralAssetFieldsDto
import org.example.project.data.centraldata.CentralImageDto

class StickerMathTest {

    private fun asset(aspect: Float, premium: Boolean = false) =
        StickerAsset(id = "a", name = "A", thumbnailUrl = "t", imageUrl = "i", aspectRatio = aspect, isPremium = premium)

    @Test
    fun newPlacedSticker_wideStickerOnPortraitPhotoIsHeldByWidth() {
        val sticker = newPlacedSticker(1, asset(aspect = 2f), photoWidth = 300f, photoHeight = 400f)
        assertEquals(DefaultStickerSizeFraction, sticker.widthFraction)
        assertEquals(0.5f, sticker.centerX)
    }

    @Test
    fun newPlacedSticker_tallStickerOnWidePhotoIsHeldByHeight() {
        // Photo 400 × 200, sticker half as wide as tall: its height may be 0.35 × 200 = 70 px,
        // so its width is 35 px, 35 / 400 of the photo's width.
        val sticker = newPlacedSticker(1, asset(aspect = 0.5f), photoWidth = 400f, photoHeight = 200f)
        assertEquals(35f / 400f, sticker.widthFraction, 1e-5f)
    }

    @Test
    fun moveSticker_keepsCentreOnPhoto() {
        val moved = moveSticker(PlacedSticker(1, "a", "i", 1f, false, centerX = 0.9f, centerY = 0.1f), 0.5f, -0.5f)
        assertEquals(1f, moved.centerX)
        assertEquals(0f, moved.centerY)
    }

    @Test
    fun scaleSticker_clampsToRange() {
        val sticker = PlacedSticker(1, "a", "i", 1f, false, widthFraction = 0.5f)
        assertEquals(StickerWidthFractionRange.endInclusive, scaleSticker(sticker, 100f).widthFraction)
        assertEquals(StickerWidthFractionRange.start, scaleSticker(sticker, 0.001f).widthFraction)
        assertEquals(0.75f, scaleSticker(sticker, 1.5f).widthFraction, 1e-5f)
    }

    @Test
    fun duplicateSticker_nudgesBackFromTheEdge() {
        val copy = duplicateSticker(PlacedSticker(1, "a", "i", 1f, false, centerX = 0.5f, centerY = 0.99f), newId = 7)
        assertEquals(7, copy.id)
        assertEquals(0.5f + StickerDuplicateNudge, copy.centerX, 1e-5f)
        assertEquals(0.99f - StickerDuplicateNudge, copy.centerY, 1e-5f)
    }

    @Test
    fun restackSticker_movesToFrontAndBack() {
        val stickers = (1L..3L).map { PlacedSticker(it, "a", "i", 1f, false) }
        assertEquals(listOf(2L, 3L, 1L), restackSticker(stickers, 1, toFront = true).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), restackSticker(stickers, 3, toFront = false).map { it.id })
        assertEquals(stickers, restackSticker(stickers, 9, toFront = true))
    }

    @Test
    fun needsPremium_onlyWithAPremiumSticker() {
        assertFalse(needsPremium(listOf(PlacedSticker(1, "a", "i", 1f, false))))
        assertTrue(needsPremium(listOf(PlacedSticker(1, "a", "i", 1f, false), PlacedSticker(2, "b", "i", 1f, true))))
    }

    @Test
    fun toStickers_readsSvgFilesInServerOrder() {
        val assets = listOf(
            CentralAssetDto(id = "2", sequence = 2, thumbnailUrl = "/thumb2.svg", moreFields = CentralAssetFieldsDto(svg = listOf(CentralImageDto("/b.svg", width = 200f, height = 100f)))),
            CentralAssetDto(id = "1", sequence = 1, isPremium = true, moreFields = CentralAssetFieldsDto(svg = listOf(CentralImageDto("/a.svg")))),
            CentralAssetDto(id = "3", sequence = 3, isEnabled = false, moreFields = CentralAssetFieldsDto(svg = listOf(CentralImageDto("/c.svg")))),
            CentralAssetDto(id = "4", sequence = 4),
        )
        val stickers = assets.toStickers { "https://host$it" }
        assertEquals(listOf("1", "2"), stickers.map { it.id })
        // No thumbnail of its own: the sticker itself stands in.
        assertEquals("https://host/a.svg", stickers[0].thumbnailUrl)
        assertEquals(1f, stickers[0].aspectRatio)
        assertTrue(stickers[0].isPremium)
        assertEquals("https://host/thumb2.svg", stickers[1].thumbnailUrl)
        assertEquals(2f, stickers[1].aspectRatio)
    }
}

class StickerEditMathTest {

    private fun sticker(
        aspect: Float = 2f,
        width: Float = 0.4f,
        rotation: Float = 0f,
        flipH: Boolean = false,
    ) = PlacedSticker(
        id = 1, assetId = "a", imageUrl = "i", aspectRatio = aspect, naturalAspect = aspect, isPremium = false,
        centerX = 0.5f, centerY = 0.5f, widthFraction = width, rotationDegrees = rotation, flippedHorizontally = flipH,
    )

    private fun assertNear(expected: Float, actual: Float, message: String = "") =
        assertEquals(expected, actual, 1e-4f, message)

    @Test
    fun stickerWithCrop_rightHalfMovesCentreRightAndHalvesWidth() {
        val cropped = stickerWithCrop(sticker(), androidx.compose.ui.geometry.Rect(0.5f, 0f, 1f, 1f), photoAspect = 1f)
        assertNear(0.2f, cropped.widthFraction)
        assertNear(1f, cropped.aspectRatio)
        // The right half's centre is a quarter of the 0.4-wide sticker right of its centre.
        assertNear(0.6f, cropped.centerX)
        assertNear(0.5f, cropped.centerY)
    }

    @Test
    fun stickerWithCrop_followsFlipAndTurn() {
        val flipped = stickerWithCrop(sticker(flipH = true), androidx.compose.ui.geometry.Rect(0.5f, 0f, 1f, 1f), 1f)
        // Flipped, the source's right half is shown on the left.
        assertNear(0.4f, flipped.centerX)
        val turned = stickerWithCrop(sticker(rotation = 90f), androidx.compose.ui.geometry.Rect(0.5f, 0f, 1f, 1f), 1f)
        // Turned a quarter clockwise, its right goes down.
        assertNear(0.5f, turned.centerX)
        assertNear(0.6f, turned.centerY)
    }

    @Test
    fun stickerWithCrop_uncroppingRestoresTheOriginal() {
        val original = sticker(rotation = 30f)
        val cropped = stickerWithCrop(original, androidx.compose.ui.geometry.Rect(0.1f, 0.2f, 0.7f, 0.9f), photoAspect = 0.75f)
        val back = stickerWithCrop(cropped, androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f), photoAspect = 0.75f)
        assertNear(original.centerX, back.centerX)
        assertNear(original.centerY, back.centerY)
        assertNear(original.widthFraction, back.widthFraction)
        assertNear(original.aspectRatio, back.aspectRatio)
    }

    @Test
    fun displayCrop_mirrorsUnderAFlip() {
        val crop = androidx.compose.ui.geometry.Rect(0.1f, 0f, 0.4f, 1f)
        assertEquals(androidx.compose.ui.geometry.Rect(0.6f, 0f, 0.9f, 1f).toString(), sourceToDisplayCrop(sticker(flipH = true), crop).toString())
        assertEquals(crop, sourceToDisplayCrop(sticker(), crop))
    }

    @Test
    fun dragCropCorner_keepsOppositeCornerAndRatio() {
        val full = androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f)
        val free = dragCropCorner(full, corner = 2, dx = -0.3f, dy = -0.1f, ratio = null, fullAspect = 2f)
        assertNear(0f, free.left)
        assertNear(0.7f, free.right)
        assertNear(0.9f, free.bottom)
        // A square crop on a 2:1 sticker is half as wide (in fractions) as it is tall.
        val square = dragCropCorner(full, corner = 0, dx = 0.5f, dy = 0f, ratio = 1f, fullAspect = 2f)
        assertNear(1f, square.right)
        assertNear(1f, square.bottom)
        assertNear(square.width * 2f, square.height)
    }

    @Test
    fun squareToQuad_mapsTheSquaresCornersOntoTheQuad() {
        val quad = listOf(
            androidx.compose.ui.geometry.Offset(10f, 5f), androidx.compose.ui.geometry.Offset(90f, 20f),
            androidx.compose.ui.geometry.Offset(80f, 95f), androidx.compose.ui.geometry.Offset(0f, 70f),
        )
        val map = squareToQuad(quad)!!
        val unit = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)
        unit.forEachIndexed { i, (u, v) ->
            val p = applyProjective(map, u, v)
            assertNear(quad[i].x, p.x, "x of corner $i")
            assertNear(quad[i].y, p.y, "y of corner $i")
        }
    }

    @Test
    fun stickerReshapeMap_isNullUntilReshapedAndHitsTheMovedCorner() {
        val plain = sticker(aspect = 1f)
        assertEquals(null, stickerReshapeMap(plain, 100f, 100f))
        val moved = moveStickerCorner(plain, index = 2, delta = androidx.compose.ui.geometry.Offset(20f, 10f), width = 100f, height = 100f)
        val map = stickerReshapeMap(moved, 100f, 100f)!!
        // The box's bottom-right (50, 50) now lands 20 right and 10 down; the top-left stays.
        val bottomRight = applyProjective(map, 50f, 50f)
        assertNear(70f, bottomRight.x)
        assertNear(60f, bottomRight.y)
        val topLeft = applyProjective(map, -50f, -50f)
        assertNear(-50f, topLeft.x)
        assertNear(-50f, topLeft.y)
    }

    @Test
    fun moveStickerCorner_refusesAFoldedShape() {
        val plain = sticker(aspect = 1f)
        // Top-left dragged past the bottom-right would turn the shape inside out.
        val folded = moveStickerCorner(plain, index = 0, delta = androidx.compose.ui.geometry.Offset(150f, 150f), width = 100f, height = 100f)
        assertEquals(plain, folded)
        assertTrue(isConvexQuad(stickerCorners(moveStickerCorner(plain, 0, androidx.compose.ui.geometry.Offset(-30f, 10f), 100f, 100f), 100f, 100f)))
    }

    @Test
    fun photoPointToSticker_mapsCornersThroughCropAndFlip() {
        // 400 × 200 px sticker centred on a 1000 × 1000 photo.
        val plain = sticker()
        val topLeft = photoPointToSticker(plain, androidx.compose.ui.geometry.Offset(300f, 400f), 1000f, 1000f)
        assertNear(0f, topLeft.x)
        assertNear(0f, topLeft.y)
        val flippedPoint = photoPointToSticker(sticker(flipH = true), androidx.compose.ui.geometry.Offset(300f, 400f), 1000f, 1000f)
        assertNear(1f, flippedPoint.x)
        val cropped = plain.copy(cropLeft = 0.5f)
        assertNear(0.75f, photoPointToSticker(cropped, androidx.compose.ui.geometry.Offset(500f, 500f), 1000f, 1000f).x)
    }

    @Test
    fun alignAndArrange() {
        val aligned = alignSticker(sticker(), org.example.project.ui.text.TextPhotoAlign.Right, halfWidth = 0.2f, halfHeight = 0.1f)
        assertNear(0.8f, aligned.centerX)
        val stickers = (1L..3L).map { sticker().copy(id = it) }
        assertEquals(listOf(2L, 1L, 3L), arrangeStickers(stickers, 1, org.example.project.ui.text.TextLayerMove.Forward).map { it.id })
    }

    @Test
    fun colorMatrix_onlyWithAColourEdit() {
        assertEquals(null, stickerColorMatrix(sticker()))
        assertTrue(stickerColorMatrix(sticker().copy(fillArgb = 0xFFFF0000.toInt())) != null)
        assertTrue(stickerColorMatrix(sticker().copy(adjust = StickerAdjust(brightness = 20f))) != null)
        // A full fill replaces the colour and keeps the alpha.
        val fill = fillColorMatrix(androidx.compose.ui.graphics.Color.Red, 1f)
        assertEquals(0f, fill[0])
        assertEquals(255f, fill[4])
        assertEquals(1f, fill[18])
    }

    @Test
    fun needsPremium_forAPremiumEffect() {
        val premiumFilter = org.example.project.ui.filter.PhotoFilters.first { it.isPremium }
        assertTrue(needsPremium(listOf(sticker().copy(filterLabel = premiumFilter.label))))
    }

    @Test
    fun sliders_roundTrip() {
        assertNear(-90f, sliderToRotation(rotationToSlider(-90f)))
        assertNear(0.5f, sliderToStickerSize(stickerSizeToSlider(0.5f)))
    }
}
