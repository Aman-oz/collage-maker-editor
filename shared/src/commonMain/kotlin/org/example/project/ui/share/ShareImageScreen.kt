package org.example.project.ui.share

import org.example.project.ui.common.readableWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
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
import org.example.project.i18n.tr
import org.example.project.ui.common.EditorSidePanelWidth
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.isSideBySideEditor
import org.example.project.ui.common.SavedImageKey
import org.example.project.ui.common.navSharedElement
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.topBar
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_home_outline

/**
 * What the primary button next to Home offers to start next. The caller picks it from the flow the
 * user came from, so someone who just made a collage is nudged toward a different feature.
 */
enum class ShareSuggestion(private val englishLabel: String) {
    NewCollage("Try Collage"),
    Freestyle("Try Freestyle"),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/** Fill of the suggestion button (same accent as the save screen's premium button). */
private val PrimaryAccent = Color(0xFF8B5CF6)

private val SavedChipBackground = Color(0xFFB9F3CF)
private val SavedChipContent = Color(0xFF1E9E4A)

private val PreviewShape = RoundedCornerShape(20.dp)

@Composable
fun ShareImageScreen(
    imagePath: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    suggestion: ShareSuggestion,
    onSuggestion: () -> Unit,
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
        suggestion = suggestion,
        onSuggestion = onSuggestion,
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
    suggestion: ShareSuggestion,
    onSuggestion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            val topBar: @Composable () -> Unit = {
                Box(modifier = Modifier.topBar()) {
                    GlassTopBarButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = tr("Back"),
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
            }
            val preview: @Composable (Modifier) -> Unit = { previewModifier ->
                Box(modifier = previewModifier, contentAlignment = Alignment.Center) {
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = tr("Saved image"),
                            modifier = Modifier
                                .aspectRatio(image.width.toFloat() / image.height)
                                // Arrives from the save screen's preview of the same image.
                                .navSharedElement(SavedImageKey)
                                .clip(PreviewShape),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        CircularProgressIndicator(color = PrimaryAccent)
                    }
                }
            }
            if (isSideBySideEditor(maxWidth, maxHeight)) {
                // A phone on its side: the picture takes the left at full height and the chip,
                // targets and buttons stack beside it, as on the save screen this came from.
                Row(modifier = Modifier.fillMaxSize()) {
                    preview(Modifier.weight(1f).fillMaxHeight().padding(16.dp))
                    Column(modifier = Modifier.width(EditorSidePanelWidth).fillMaxHeight()) {
                        topBar()
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            ShareActions(
                                hasImage = image != null,
                                compact = true,
                                onShare = onShare,
                                onHome = onHome,
                                suggestion = suggestion,
                                onSuggestion = onSuggestion,
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    topBar()
                    preview(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp))
                    // Capped on a tablet: spread across the full width the icons drift far apart
                    // and the buttons become screen-wide bars.
                    ShareActions(
                        hasImage = image != null,
                        compact = false,
                        onShare = onShare,
                        onHome = onHome,
                        suggestion = suggestion,
                        onSuggestion = onSuggestion,
                        modifier = Modifier.readableWidth(),
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

/**
 * The Saved chip, the share targets and the next-step buttons. [compact] trims the space around
 * them to fit the side column of a phone on its side.
 */
@Composable
private fun ShareActions(
    hasImage: Boolean,
    compact: Boolean,
    onShare: (ShareTarget) -> Unit,
    onHome: () -> Unit,
    suggestion: ShareSuggestion,
    onSuggestion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SavedChip()

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = if (compact) 20.dp else 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ShareTarget.entries.forEach { target ->
                val bounce = rememberSpringBounce()
                ShareTargetIcon(
                    target = target,
                    modifier = Modifier
                        .springBounce(bounce)
                        .size(44.dp)
                        .clickable(
                            interactionSource = bounce.interactionSource,
                            indication = LocalIndication.current,
                            enabled = hasImage,
                        ) { onShare(target) },
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = if (compact) 16.dp else 48.dp),
            // Home keeps its natural width; the suggestion takes all the space left over.
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val suggestionBounce = rememberSpringBounce()
            Button(
                onClick = onSuggestion,
                interactionSource = suggestionBounce.interactionSource,
                modifier = Modifier.springBounce(suggestionBounce).height(44.dp).weight(1f),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = suggestion.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            val homeBounce = rememberSpringBounce()
            OutlinedButton(
                onClick = onHome,
                interactionSource = homeBounce.interactionSource,
                modifier = Modifier.springBounce(homeBounce).height(44.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_home_outline),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tr("Home"),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
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
        Text(text = tr("Saved"), color = SavedChipContent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
            suggestion = ShareSuggestion.NewCollage,
            onSuggestion = {},
        )
    }
}
