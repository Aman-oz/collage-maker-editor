package org.example.project.ui.common

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Wraps packed `0xAARRGGBB` [pixels] (the layout `ImageBitmap.readPixels` produces) in a new
 * [ImageBitmap]. Common Compose can read pixels but offers no way to write them, hence the actuals.
 */
internal expect fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int): ImageBitmap
