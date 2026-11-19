package org.example.project.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.common.readableWidth
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import org.example.project.data.appicon.AppIcons
import org.example.project.i18n.tr
import com.ozi.nextgen.compose.ads_kmp.GetStartedSection
import com.ozi.nextgen.compose.ads_manager.navigation.GetStartedVariant
import org.example.project.data.ads.AdPoints
import com.ozi.nextgen.compose.ads_manager.navigation.NavScreen

/** Splash brand color; the title and the loading bar derive from it. */
private val SplashPrimary = Color(0xFF8B5CF6)

/**
 * Loading screen shown on every launch. Once loaded it continues as Remote Config's
 * `get_started_variant` says: by itself, or behind a "Get Started" button (with a banner or native
 * ad under it, or alone).
 *
 * @param onNavigate called once, with the first screen of the startup flow.
 */
@Composable
fun SplashScreen(
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = koinViewModel(),
) {
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)
    var leaving by remember { mutableStateOf(false) }
    val start = {
        if (!leaving) {
            leaving = true
            viewModel.start { screen -> currentOnNavigate(screen) }
        }
    }
    SplashContent(
        modifier = modifier,
        bottom = {
            if (!ready) {
                SplashProgress()
            } else {
                GetStartedSection(
                    variant = viewModel.getStartedVariant,
                    onGetStarted = start,
                    point = if (viewModel.getStartedVariant == GetStartedVariant.BUTTON_WITH_NATIVE) {
                        AdPoints.SPLASH_NATIVE_SPLASH
                    } else {
                        AdPoints.SPLASH_BANNER_SPLASH
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    isDarkTheme = isSystemInDarkTheme(),
                ) { onClick ->
                    Button(
                        onClick = onClick,
                        enabled = !leaving,
                        colors = ButtonDefaults.buttonColors(containerColor = SplashPrimary, contentColor = Color.White),
                        modifier = Modifier.readableWidth().padding(horizontal = 32.dp).fillMaxWidth().height(52.dp),
                    ) {
                        Text(tr("Get Started"), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
    )
}

@Composable
private fun SplashContent(modifier: Modifier = Modifier, bottom: @Composable () -> Unit = { SplashProgress() }) {
    // A column, not layers: the logo is centred in whatever the bottom (button + ad) leaves, so a
    // tall ad pushes it up instead of covering it.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            Image(
                painter = painterResource(remember { AppIcons.selectedArtwork() }),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp)),
            )
            Text(
                text = "Collage Maker: Pic Editor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SplashPrimary,
                textAlign = TextAlign.Center,
            )
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            bottom()
        }
    }
}

@Composable
private fun SplashProgress() {
    LinearProgressIndicator(
        modifier = Modifier
            .padding(vertical = 32.dp)
            // Capped so it doesn't become a bar across a whole tablet screen.
            .readableWidth()
            .padding(horizontal = 48.dp)
            .height(6.dp)
            .clip(CircleShape),
        color = SplashPrimary,
        trackColor = SplashPrimary.copy(alpha = 0.2f),
    )
}

@Preview
@Composable
private fun SplashScreenPreview() {
    ThemePreviews { SplashContent() }
}
