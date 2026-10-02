package org.example.project.ui.bgremover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.i18n.tr
import org.example.project.ui.common.BackgroundRemoverPhotoKey
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.crop.CropContent
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Background remover step 1: the shared crop UI with its rotate/flip row switched on. Done puts the
 * cropped photo in the session and hands off to [BackgroundRemoverEditorScreen].
 */
@Composable
fun BackgroundRemoverCropScreen(
    imagePath: String,
    onBack: () -> Unit,
    onCropped: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackgroundRemoverCropViewModel = koinViewModel { parametersOf(imagePath) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        is BackgroundRemoverCropUiState.Ready -> CropContent(
            sourceImage = state.image,
            onBack = onBack,
            onCropConfirmed = { cropped ->
                viewModel.applyCrop(cropped)
                onCropped()
            },
            showTransformTools = true,
            // The cropped area flies into the eraser's canvas, and back out of it on Back.
            cropSharedKey = BackgroundRemoverPhotoKey,
            modifier = modifier,
        )
        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .safeDrawingPadding(),
        ) {
            ToolTopBar(title = tr("Crop"), onClose = onBack, onDone = {}, doneEnabled = false)
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (state is BackgroundRemoverCropUiState.Error) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Preview
@Composable
private fun BackgroundRemoverCropScreenPreview() {
    ThemePreviews {
        CropContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onCropConfirmed = {}, showTransformTools = true)
    }
}
