package org.example.project.ui.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_home_outline

/** Fill of the "New Collage" button (same accent as the save screen's premium button). */
private val PrimaryAccent = Color(0xFF8B5CF6)

private val SavedChipBackground = Color(0xFFB9F3CF)
private val SavedChipContent = Color(0xFF1E9E4A)

private val PreviewShape = RoundedCornerShape(20.dp)

@Composable
fun ShareImageScreen(
    imagePath: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNewCollage: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShareImageViewModel = koinViewModel { parametersOf(imagePath) },
) {
    val image by viewModel.image.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.errors.collect { snackbarHostState.showSnackbar(it) }
    }

    ShareImageContent(
        image = image,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onShare = viewModel::share,
        onHome = onHome,
        onNewCollage = onNewCollage,
        modifier = modifier,
    )
}

@Composable
private fun ShareImageContent(
    image: ImageBitmap?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onShare: (ShareTarget) -> Unit,
    onHome: () -> Unit,
    onNewCollage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Box(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = "Saved image",
                        modifier = Modifier
                            .aspectRatio(image.width.toFloat() / image.height)
                            .clip(PreviewShape),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            }

            SavedChip(modifier = Modifier.align(Alignment.CenterHorizontally))

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ShareTarget.entries.forEach { target ->
                    ShareTargetIcon(
                        target = target,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(enabled = image != null) { onShare(target) },
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = onNewCollage,
                    modifier = Modifier.height(36.dp).width(150.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(text = "New Collage", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier.height(36.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_home_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Home",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

@Composable
private fun SavedChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(SavedChipBackground)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = SavedChipContent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "Saved", color = SavedChipContent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview
@Composable
private fun ShareImageContentPreview() {
    ThemePreviews {
        ShareImageContent(
            image = ImageBitmap(300, 400),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onShare = {},
            onHome = {},
            onNewCollage = {},
        )
    }
}
