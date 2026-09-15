package org.example.project.data.network

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes raw encoded image bytes (PNG/JPEG downloaded from the network) into a Compose
 * [ImageBitmap]. FileKit only decodes files that live on disk, so network images — the decorative
 * collage frame and the layout thumbnails — need this platform-specific step.
 *
 * @throws Exception if the bytes are not a decodable image; callers treat that as a load failure.
 */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap
