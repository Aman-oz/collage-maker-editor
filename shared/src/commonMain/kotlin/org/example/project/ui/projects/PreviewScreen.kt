package org.example.project.ui.projects

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.i18n.tr
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorIconTint
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.TopBarButtonSize
import org.example.project.ui.common.navSharedElement
import org.example.project.ui.common.projectImageKey
import org.example.project.ui.common.topBar
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Full-screen view of a saved project, opened from Home's Projects grid. */
@Composable
fun PreviewScreen(
    imagePath: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: PreviewViewModel = koinViewModel { parametersOf(imagePath) },
) {
    val image by viewModel.image.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.errors.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(deleted) { if (deleted) onBack() }
    PreviewContent(
        image = image,
        loadFailed = loadFailed,
        sharedKey = projectImageKey(imagePath),
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = onEdit,
        onShare = viewModel::share,
        onDelete = viewModel::delete,
    )
}

@Composable
private fun PreviewContent(
    image: ImageBitmap?,
    loadFailed: Boolean,
    sharedKey: String,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmingDelete by remember { mutableStateOf(false) }
    if (confirmingDelete) {
        DeleteProjectDialog(
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
    Box(modifier = Modifier.fillMaxSize().background(EditorBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(
                modifier = Modifier.topBar(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassTopBarButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("Back"),
                    onClick = onBack,
                    contentColor = EditorIconTint,
                )
                Spacer(Modifier.weight(1f))
                // Nothing to edit or share until the file loads; a broken entry can still be deleted.
                CircleButton(Icons.Outlined.Edit, tr("Edit"), onEdit, enabled = image != null)
                CircleButton(Icons.Outlined.Share, tr("Share"), onShare, enabled = image != null)
                CircleButton(
                    Icons.Outlined.Delete,
                    tr("Delete"),
                    { confirmingDelete = true },
                    enabled = image != null || loadFailed,
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    image != null -> Image(
                        bitmap = image,
                        contentDescription = tr("Project"),
                        // Crop at the image's own ratio shows the whole picture at rest, and lets it
                        // grow smoothly out of the grid's square crop during the shared transition.
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(image.width.toFloat() / image.height)
                            .navSharedElement(sharedKey)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    loadFailed -> Text(
                        text = tr("This project is no longer available"),
                        color = EditorLabelTint,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}

@Composable
private fun CircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = EditorControlBackground,
            contentColor = EditorIconTint,
            disabledContainerColor = EditorControlBackground,
            disabledContentColor = EditorIconTint.copy(alpha = 0.38f),
        ),
        modifier = Modifier.size(TopBarButtonSize).clip(CircleShape),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun DeleteProjectDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = tr("Delete project?"), fontWeight = FontWeight.Bold) },
        text = {
            Text(text = tr("It will be removed from Projects. The copy saved to your photo library stays."))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = tr("Delete"), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = tr("Cancel")) }
        },
    )
}

@Preview
@Composable
private fun PreviewScreenPreview() {
    ThemePreviews {
        PreviewContent(
            image = ImageBitmap(300, 400),
            loadFailed = false,
            sharedKey = "preview",
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onEdit = {},
            onShare = {},
            onDelete = {},
        )
    }
}
