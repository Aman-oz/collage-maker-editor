package org.example.project.data.network

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        ?: error("Could not decode image (${bytes.size} bytes)")
