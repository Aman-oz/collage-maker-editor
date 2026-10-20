package org.example.project.ui.stickers

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertTrue

/** Runs the sticker renderer on Skia, which the preview and the bake both draw through. */
class StickerRenderingIOSTest {

    /** A solid blue 100 × 100 source picture. */
    private fun source(): ImageBitmap {
        val image = ImageBitmap(100, 100)
        Canvas(image).drawRect(0f, 0f, 100f, 100f, Paint().apply { color = Color.Blue })
        return image
    }

    private val sticker = PlacedSticker(id = 1, assetId = "a", imageUrl = "i", aspectRatio = 1f, naturalAspect = 1f, isPremium = false)

    @Test
    fun eraseStroke_clearsTheBodyUnderIt() {
        val erased = sticker.copy(eraseStrokes = listOf(StickerStroke(points = listOf(0.25f, 0.5f), radius = 0.1f)))
        val pixels = renderStickerBody(source(), erased, 64).toPixelMap()
        assertTrue(pixels[16, 32].alpha < 0.05f, "erased spot is ${pixels[16, 32]}")
        assertTrue(pixels[48, 32].alpha > 0.95f, "untouched spot is ${pixels[48, 32]}")
    }

    @Test
    fun fill_turnsTheBodyItsColour() {
        val filled = sticker.copy(fillArgb = Color.Red.toArgb(), fillIntensity = 100f)
        val pixel = renderStickerBody(source(), filled, 32).toPixelMap()[16, 16]
        assertTrue(pixel.red > 0.95f && pixel.blue < 0.05f, "filled pixel is $pixel")
    }

    @Test
    fun outline_drawsOutsideTheBox() {
        // A 0.2-wide sticker in the middle of a 200 px picture: its box runs 80..120.
        val outlined = sticker.copy(widthFraction = 0.2f, outlineWidth = 100f, outlineArgb = Color.Green.toArgb())
        val art = renderStickerArt(source(), outlined, 64)
        val output = ImageBitmap(200, 200)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(output), Size(200f, 200f)) {
            drawSticker(art, outlined, Size(200f, 200f))
        }
        val pixels = output.toPixelMap()
        val inside = pixels[100, 100]
        val justOutside = pixels[121, 100]
        val farOutside = pixels[150, 100]
        assertTrue(inside.blue > 0.9f, "inside is $inside")
        assertTrue(justOutside.green > 0.9f, "the outline is $justOutside")
        assertTrue(farOutside.alpha < 0.05f, "far outside is $farOutside")
    }

    @Test
    fun shadow_fallsBelowRightOfTheSticker() {
        val shadowed = sticker.copy(widthFraction = 0.2f, shadowOpacity = 1f, shadowDx = 0.3f, shadowDy = 0.3f, shadowBlur = 0f)
        val art = renderStickerArt(source(), shadowed, 64)
        val output = ImageBitmap(200, 200)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(output), Size(200f, 200f)) {
            drawSticker(art, shadowed, Size(200f, 200f))
        }
        // Offset 0.3 × 0.5 × 40 px = 6 px down and right: just past the box's corner is shadow.
        val pixel = output.toPixelMap()[123, 123]
        assertTrue(pixel.alpha > 0.9f && pixel.blue < 0.1f, "shadow pixel is $pixel")
    }
}
