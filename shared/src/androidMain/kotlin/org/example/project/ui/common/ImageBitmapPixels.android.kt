package org.example.project.ui.common

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()

internal actual fun ImageBitmap.toReadable(): ImageBitmap {
    val bitmap = asAndroidBitmap()
    return if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false).asImageBitmap() else this
}
