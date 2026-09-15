package org.example.project.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.example.project.data.network.NetworkImageLoader
import org.koin.compose.koinInject

/**
 * Asynchronously loads [url] through the shared [NetworkImageLoader] and draws it, showing a small
 * spinner while the bytes are in flight. Used for the collage layout thumbnails and the decorative
 * frame preview. Keyed on [url] so recycled rows re-fetch the correct image.
 */
@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val loader: NetworkImageLoader = koinInject()
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) { bitmap = loader.load(url) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator(color = EditorAccent, modifier = Modifier.size(20.dp))
        }
    }
}
