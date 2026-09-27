package org.example.project.ui.frames

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.TemplateFrame
import org.example.project.ui.templates.TemplatesContent
import org.example.project.ui.templates.TemplatesToast
import org.example.project.ui.templates.TemplatesUiState
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Frames catalog browser. It looks and behaves exactly like the Templates screen — same
 * content composable, same premium/editor handling — and differs only in where [FramesViewModel]
 * gets its data from.
 */
@Composable
fun FramesScreen(
    onBack: () -> Unit,
    onOpenEditor: (TemplateFrame) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FramesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        TemplatesContent(
            uiState = uiState,
            onBack = onBack,
            onCategorySelected = viewModel::selectCategory,
            // Premium frames only raise a toast; free ones open the editor with the selection.
            onFrameClick = { frame ->
                if (frame.isPremium) toastMessage = "This is a premium frame" else onOpenEditor(frame)
            },
            onGoPro = { toastMessage = "Go Premium — coming soon" },
            onRetry = viewModel::retry,
            title = "Frames",
            emptyMessage = "No frames in this category",
        )
        TemplatesToast(
            message = toastMessage,
            onDismissed = { toastMessage = null },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Preview
@Composable
private fun FramesScreenPreview() {
    ThemePreviews {
        TemplatesContent(
            uiState = TemplatesUiState(
                categories = listOf(
                    TemplateCategory("1", "All"),
                    TemplateCategory("2", "Love"),
                    TemplateCategory("3", "Summer"),
                ),
                selectedCategoryId = "1",
                isLoadingFrames = true,
            ),
            onBack = {},
            onCategorySelected = {},
            onFrameClick = {},
            onGoPro = {},
            onRetry = {},
            title = "Frames",
            emptyMessage = "No frames in this category",
        )
    }
}
