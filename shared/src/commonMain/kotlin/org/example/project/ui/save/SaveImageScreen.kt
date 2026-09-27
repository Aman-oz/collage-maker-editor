package org.example.project.ui.save

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.app_icon
import photocollagemaker.shared.generated.resources.ic_premium_icon

/** Fill of the premium "Remove watermark" button. */
private val PremiumAccent = Color(0xFF8B5CF6)

/** Fill of the "Remove watermark" hint bubble. */
private val HintGreen = Color(0xFF1E7B3C)

private val HintNotchWidth = 12.dp

/** Space between the hint's notch and the watermark, enough to clear the ✕ badge on its corner. */
private val HintGap = 8.dp

private val PreviewShape = RoundedCornerShape(20.dp)

@Composable
fun SaveImageScreen(
    onBack: () -> Unit,
    onSaved: (imagePath: String) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SaveImageViewModel = koinViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val watermark = imageResource(Res.drawable.app_icon)

    SaveImageContent(
        image = viewModel.image,
        // Premium images are saved clean, so there is no watermark to preview either.
        watermark = watermark.takeUnless { isPremium },
        isPremium = isPremium,
        status = status,
        onBack = onBack,
        onSaveWithWatermark = { viewModel.save(watermark) },
        onSaveClean = { viewModel.save(watermark = null) },
        // Removing the watermark is a premium feature: free users are sent to the paywall.
        onRemoveWatermark = onOpenPremium,
        onStatusShown = viewModel::consumeStatus,
        onSaved = onSaved,
        modifier = modifier,
    )
}

@Composable
private fun SaveImageContent(
    image: ImageBitmap?,
    watermark: ImageBitmap?,
    isPremium: Boolean,
    status: SaveStatus,
    onBack: () -> Unit,
    onSaveWithWatermark: () -> Unit,
    onSaveClean: () -> Unit,
    onRemoveWatermark: () -> Unit,
    onStatusShown: () -> Unit,
    onSaved: (imagePath: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(status) {
        when (status) {
            is SaveStatus.Saved -> {
                onStatusShown()
                onSaved(status.imagePath)
            }
            is SaveStatus.Failed -> {
                onStatusShown()
                snackbarHostState.showSnackbar(status.message)
            }
            else -> Unit
        }
    }
    val saving = status == SaveStatus.Saving

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            SaveImageTopBar(onBack = onBack)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (image != null) {
                    WatermarkedPreview(image = image, watermark = watermark, onRemoveWatermark = onRemoveWatermark)
                }
                if (saving) CircularProgressIndicator(color = PremiumAccent)
            }

            if (isPremium) {
                Button(
                    onClick = onSaveClean,
                    enabled = !saving && image != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 40.dp)
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumAccent, contentColor = Color.White),
                ) {
                    Text(text = "Save Image", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(
                        onClick = onSaveWithWatermark,
                        enabled = !saving && image != null,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        ButtonLabel(text = "Save with\nwatermark", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = onRemoveWatermark,
                        enabled = !saving && image != null,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumAccent, contentColor = Color.White),
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_premium_icon),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        ButtonLabel(text = "Remove\nwatermark", color = Color.White)
                    }
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
private fun SaveImageTopBar(onBack: () -> Unit) {
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
        Text(
            text = "Save Image",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The finished image with the watermark overlaid where [bakeWatermark] will stamp it. The overlay
 * is sized from [watermarkRect] against the on-screen image size, so preview and export match.
 */
@Composable
private fun WatermarkedPreview(image: ImageBitmap, watermark: ImageBitmap?, onRemoveWatermark: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .aspectRatio(image.width.toFloat() / image.height)
            .clip(PreviewShape),
    ) {
        Image(
            bitmap = image,
            contentDescription = "Image to save",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        if (watermark != null) {
            // Measure in whole dp so the fractions from watermarkRect apply unchanged.
            val rect = watermarkRect(maxWidth.value.toInt(), maxHeight.value.toInt())
            Box(modifier = Modifier.offset(rect.left.dp, rect.top.dp).size(rect.width.dp)) {
                val corner = (rect.width * WatermarkCornerFraction).dp
                Image(
                    bitmap = watermark,
                    contentDescription = "Watermark",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(corner))
                        .border(1.5.dp, Color.White, RoundedCornerShape(corner))
                        .clickable(onClick = onRemoveWatermark),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-5).dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onRemoveWatermark),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove watermark",
                        tint = Color.Black,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
            // Right-aligned with the watermark and just above it (clear of the ✕ badge), with the
            // notch shifted so its tip lands on the watermark's center.
            RemoveWatermarkHint(
                notchEndPadding = (rect.width.dp - HintNotchWidth) / 2,
                onClick = onRemoveWatermark,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = maxWidth - rect.right.dp, bottom = maxHeight - rect.top.dp + HintGap),
            )
        }
    }
}

/** Green hint bubble whose notch points down at the watermark below it. Tapping it opens the paywall. */
@Composable
private fun RemoveWatermarkHint(notchEndPadding: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = "Remove watermark",
            modifier = Modifier
                .background(HintGreen, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Canvas(
            modifier = Modifier
                .padding(end = notchEndPadding.coerceAtLeast(0.dp))
                .size(width = HintNotchWidth, height = 6.dp),
        ) {
            val notch = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width / 2f, size.height)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(notch, HintGreen)
        }
    }
}

@Composable
private fun ButtonLabel(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
private fun SaveImageContentPreview() {
    ThemePreviews {
        SaveImageContent(
            image = ImageBitmap(300, 400),
            watermark = ImageBitmap(64, 64),
            isPremium = false,
            status = SaveStatus.Idle,
            onBack = {},
            onSaveWithWatermark = {},
            onSaveClean = {},
            onRemoveWatermark = {},
            onStatusShown = {},
            onSaved = {},
        )
    }
}
