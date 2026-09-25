package org.example.project.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
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
 * @param invertInDark the artwork is dark line art on a transparent background, so it is inverted
 * under a dark theme to stay visible.
 */
private data class OnboardingPage(
    val image: DrawableResource,
    val title: String,
    val contentScale: ContentScale = ContentScale.Fit,
    val invertInDark: Boolean = false,
)

private val OnboardingPages = listOf(
    OnboardingPage(Res.drawable.ic_onboarding_templates, "590+ Templates"),
    // The layout grid is taller than the screen; it fills the width and bleeds off top and bottom.
    OnboardingPage(
        Res.drawable.ic_onboarding_collage_1,
        "500+ Layouts",
        contentScale = ContentScale.Crop,
        invertInDark = true,
    ),
    OnboardingPage(Res.drawable.ic_onboarding_collage_2, "Easily Customizable"),
    OnboardingPage(Res.drawable.ic_onboarding_freestyle, "Create Memories with Free Style"),
    OnboardingPage(Res.drawable.ic_onboarding_editor, "Make Stories with Picture Editor!"),
)

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
 * @param onFinish called from Continue on the last page, or from the close button on any page.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingContent(onFinish = onFinish, modifier = modifier)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            IconButton(onClick = onFinish, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Skip",
                    tint = colors.onBackground,
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { index ->
            val page = OnboardingPages[index]
            Image(
                painter = painterResource(page.image),
                contentDescription = page.title,
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentScale = page.contentScale,
                colorFilter = if (page.invertInDark && isDark) InvertColorFilter else null,
            )
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
            Text(text = "Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
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
