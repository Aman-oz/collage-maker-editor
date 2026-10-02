package org.example.project.ui.frames

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.i18n.tr
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.TemplateFrame
import org.example.project.ui.templates.TemplatesContent
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
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FramesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    TemplatesContent(
        uiState = uiState,
        onBack = onBack,
        onCategorySelected = viewModel::selectCategory,
        // A premium frame opens the paywall unless the user already subscribes.
        onFrameClick = { frame ->
            if (frame.isPremium && !isPremium) onOpenPremium() else onOpenEditor(frame)
        },
        onGoPro = onOpenPremium,
        onRetry = viewModel::retry,
        title = tr("Frames"),
        emptyMessage = tr("No frames in this category"),
        modifier = modifier,
    )
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
            title = tr("Frames"),
            emptyMessage = tr("No frames in this category"),
        )
    }
}
