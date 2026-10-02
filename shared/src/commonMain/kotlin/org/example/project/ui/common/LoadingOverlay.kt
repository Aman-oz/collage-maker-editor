package org.example.project.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import photocollagemaker.shared.generated.resources.Res
import org.example.project.i18n.tr

/** The app's loading animation, first used by the background remover's Ai Magic. */
private const val LoadingAnimationPath = "files/loading_animation.json"
private val LoadingAnimationSize = 140.dp

/**
 * The app's loading state: the looping loading animation and [message] over a dimmed scrim that
 * swallows touches, so nothing underneath can be edited while it shows. It fills its parent, so it
 * can cover a whole screen or just the area that is loading.
 */
@Composable
internal fun LoadingOverlay(
    modifier: Modifier = Modifier,
    message: String = tr("Processing… Please wait!"),
    contentDescription: String = tr("Loading"),
    animationSize: Dp = LoadingAnimationSize,
) {
    // Also shown while the Lottie JSON parses (a frame or two), when the painter draws nothing yet.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = rememberLoopingLottiePainter(LoadingAnimationPath),
                contentDescription = contentDescription,
                modifier = Modifier.size(animationSize),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Loops the Lottie file at [path] (a `composeResources/files/` path). Draws nothing until the
 * composition has parsed, which is a frame or two.
 */
@Composable
internal fun rememberLoopingLottiePainter(path: String): Painter {
    val composition by rememberLottieComposition(path) {
        LottieCompositionSpec.JsonString(Res.readBytes(path).decodeToString())
    }
    return rememberLottiePainter(composition = composition, iterations = Compottie.IterateForever)
}
