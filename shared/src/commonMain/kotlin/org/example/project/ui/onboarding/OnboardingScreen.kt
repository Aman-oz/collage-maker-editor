package org.example.project.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_onboarding_collage_1
import photocollagemaker.shared.generated.resources.ic_onboarding_collage_2
import photocollagemaker.shared.generated.resources.ic_onboarding_editor
import photocollagemaker.shared.generated.resources.ic_onboarding_freestyle
import photocollagemaker.shared.generated.resources.ic_onboarding_templates

/** Accent for the Continue button and the active page dot; matches Splash and Language. */
private val OnboardingAccent = Color(0xFF8B5CF6)

/**
 * One onboarding page.
 *
 * @param animation a Lottie JSON under `composeResources/files/`, played in a loop in place of
 * [image] when set.
 * @param horizontalPadding the gap between the artwork and the screen edges.
 * @param scrollable the artwork is taller than the pager: it is laid out at full width and its
 * natural aspect ratio, and scrolls vertically instead of being scaled down or cropped.
 * @param invertInDark the artwork is dark line art on a transparent background, so it is inverted
 * under a dark theme to stay visible.
 */
private data class OnboardingPage(
    val image: DrawableResource,
    val title: String,
    val animation: String? = null,
    val contentScale: ContentScale = ContentScale.Fit,
    val horizontalPadding: Dp = 2.dp,
    val scrollable: Boolean = false,
    val invertInDark: Boolean = false,
)

private val OnboardingPages = listOf(
    OnboardingPage(
        Res.drawable.ic_onboarding_templates,
        "590+ Templates",
        animation = "files/templates_animation.json",
    ),
    /*OnboardingPage(
        Res.drawable.ic_onboarding_collage_1,
        "500+ Layouts",
        animation = "files/onboarding_layouts.json",
        horizontalPadding = 8.dp,
        scrollable = true,
        invertInDark = true,
    ),*/
    OnboardingPage(
        Res.drawable.ic_onboarding_collage_2,
        "Easily Customizable",
        animation = "files/collage_animation.json",
    ),
    OnboardingPage(
        Res.drawable.ic_onboarding_freestyle,
        "Create Memories with Free Style",
        animation = "files/freestyle_animation.json",
    ),
    OnboardingPage(
        Res.drawable.ic_onboarding_editor,
        "Make Stories with Picture Editor!",
        animation = "files/editor_animation.json",
    ),
)

/** How long after the walkthrough opens before its skip button appears. */
private const val SkipButtonDelayMillis = 3_000L

/** Inverts RGB and keeps alpha, turning black/grey strokes into white/grey ones. */
private val InvertColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

/**
 * Feature walkthrough shown once after the language picker.
 *
 * @param onFinish called from Get Started on the last page. The close button only skips ahead to
 * that page.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    OnboardingContent(
        onFinish = {
            viewModel.complete()
            onFinish()
        },
        modifier = modifier,
    )
}

@Composable
private fun OnboardingContent(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { OnboardingPages.size }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    // The skip button only shows up a while after the walkthrough opens, so the first page gets seen.
    var skipDelayElapsed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SkipButtonDelayMillis)
        skipDelayElapsed = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Fixed height so the pager doesn't jump when the skip button appears or hides.
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = skipDelayElapsed && pagerState.currentPage != OnboardingPages.lastIndex,
                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                exit = fadeOut() + scaleOut(targetScale = 0.6f),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                // Skip jumps to the last page rather than leaving, so Get Started is still the only way on.
                IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(OnboardingPages.lastIndex) } }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Skip",
                        tint = colors.onBackground,
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { index ->
            val page = OnboardingPages[index]
            val painter = page.animation?.let { rememberLoopingLottiePainter(it) }
                ?: painterResource(page.image)
            val imageModifier = if (page.scrollable) {
                // A Lottie painter has no intrinsic size until its composition has parsed.
                val size = painter.intrinsicSize
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (size.isSpecified && size.height > 0f) {
                            Modifier.aspectRatio(size.width / size.height)
                        } else {
                            Modifier
                        },
                    )
            } else {
                Modifier.fillMaxSize()
            }
            val scrollState = rememberScrollState()
            // Hides the fade once the bottom is reached, so the last row isn't left faded out.
            val fade by animateFloatAsState(if (scrollState.canScrollForward) 1f else 0f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (page.scrollable) {
                            Modifier.bottomFade(height = 72.dp, strength = fade).verticalScroll(scrollState)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = page.horizontalPadding),
            ) {
                Image(
                    painter = painter,
                    contentDescription = page.title,
                    modifier = imageModifier,
                    contentScale = page.contentScale,
                    colorFilter = if (page.invertInDark && isDark) InvertColorFilter else null,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        PageIndicator(
            pageCount = OnboardingPages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = OnboardingPages[pagerState.currentPage].title,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val next = pagerState.currentPage + 1
                if (next < OnboardingPages.size) {
                    scope.launch { pagerState.animateScrollToPage(next) }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = OnboardingAccent,
                contentColor = Color.White,
            ),
        ) {
            val isLastPage = pagerState.currentPage == OnboardingPages.lastIndex
            Text(
                text = if (isLastPage) "Get Started" else "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Loops the Lottie file at [path]. Draws nothing until the composition has parsed, which is a
 * frame or two, so there is no placeholder.
 */
@Composable
private fun rememberLoopingLottiePainter(path: String): Painter {
    val composition by rememberLottieComposition(path) {
        LottieCompositionSpec.JsonString(Res.readBytes(path).decodeToString())
    }
    return rememberLottiePainter(composition = composition, iterations = Compottie.IterateForever)
}

/**
 * Fades the bottom [height] of the content out to transparent, hinting there is more below.
 * [strength] 0 draws the content untouched, 1 fades it fully at the bottom edge.
 *
 * Masks with [BlendMode.DstIn] rather than painting the background colour over the edge, so it
 * works on any background; that needs an offscreen layer or it would also punch through whatever
 * is drawn behind.
 */
private fun Modifier.bottomFade(height: Dp, strength: Float): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (strength <= 0f) return@drawWithContent
        val fadeHeight = height.toPx().coerceAtMost(size.height)
        val top = size.height - fadeHeight
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Black.copy(alpha = 1f - strength)),
                startY = top,
                endY = size.height,
            ),
            topLeft = Offset(0f, top),
            size = Size(size.width, fadeHeight),
            blendMode = BlendMode.DstIn,
        )
    }

/** Row of dots; the current page's dot is accent-colored and slightly larger. */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val color by animateColorAsState(
                if (selected) OnboardingAccent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
            )
            val size by animateDpAsState(if (selected) 8.dp else 7.dp)
            Box(
                modifier = Modifier
                    .size(size)
                    .background(color, CircleShape),
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    ThemePreviews {
        OnboardingContent(onFinish = {})
    }
}

@Preview
@Composable
private fun OnboardingLayoutsPagePreview() {
    ThemePreviews {
        OnboardingContent(onFinish = {}, initialPage = 1)
    }
}
