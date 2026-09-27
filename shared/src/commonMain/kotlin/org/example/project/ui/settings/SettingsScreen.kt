package org.example.project.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.launch
import org.example.project.data.ThemeMode
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_about_us
import photocollagemaker.shared.generated.resources.ic_language
import photocollagemaker.shared.generated.resources.ic_privacy_policy
import photocollagemaker.shared.generated.resources.ic_rate_us
import photocollagemaker.shared.generated.resources.ic_share_app
import photocollagemaker.shared.generated.resources.ic_terms_and_conditions
import photocollagemaker.shared.generated.resources.ic_theme
import photocollagemaker.shared.generated.resources.img_premium_card

/** Brand purple shared with the language screen, splash and onboarding. */
internal val SettingsAccent = Color(0xFF8B5CF6)
private val TryNowGradient = listOf(Color(0xFFFFB347), Color(0xFFFF8A00))

private val RowShape = RoundedCornerShape(percent = 50)
private val PremiumCardShape = RoundedCornerShape(16.dp)

/** Width / height of `img_premium_card.png` (1050 × 420). */
private const val PremiumCardAspectRatio = 2.5f

/** Which settings dialog is open. Saved by name, since an enum is not saveable on every platform. */
private enum class SettingsDialog { None, Theme, RateUs }

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLanguage: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }
    // Links are filled in later; until then a blank one explains itself instead of doing nothing.
    val openLink: (url: String, name: String) -> Unit = { url, name ->
        if (url.isBlank()) showMessage("$name is coming soon") else uriHandler.openUri(url)
    }

    SettingsContent(
        themeMode = themeMode,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onPremium = { showMessage("Premium is coming soon") },
        onOpenLanguage = onOpenLanguage,
        onApplyTheme = viewModel::setThemeMode,
        onRate = { rating ->
            if (rating >= StoreRatingThreshold) openStoreRating() else showMessage("Thanks for your feedback!")
        },
        onShareApp = {
            val storeUrl = appStoreUrl()
            if (storeUrl == null) showMessage("Sharing is coming soon") else shareText(shareAppMessage(storeUrl))
        },
        onAboutUs = { openLink(SettingsLinks.AboutUsUrl, "About Us") },
        onPrivacyPolicy = { openLink(SettingsLinks.PrivacyPolicyUrl, "Privacy Policy") },
        onTermsOfUse = { openLink(SettingsLinks.TermsOfUseUrl, "Terms of Use") },
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    themeMode: ThemeMode,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onOpenLanguage: () -> Unit,
    onApplyTheme: (ThemeMode) -> Unit,
    onRate: (rating: Int) -> Unit,
    onShareApp: () -> Unit,
    onAboutUs: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTermsOfUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogName by rememberSaveable { mutableStateOf(SettingsDialog.None.name) }
    val dialog = SettingsDialog.valueOf(dialogName)
    val closeDialog = { dialogName = SettingsDialog.None.name }
    // Keeps the panel's content on screen while its exit animation plays after [dialog] is None.
    var shownDialog by remember { mutableStateOf(dialog) }
    if (dialog != SettingsDialog.None) shownDialog = dialog
    val liquidState = rememberLiquidState()

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = dialog != SettingsDialog.None,
        onBackCompleted = closeDialog,
    )

    Box(modifier = modifier.fillMaxSize()) {
        // The glass dialogs refract this layer, so it must not contain them (see GlassDialogHost).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .liquefiable(liquidState)
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
        ) {
            SettingsTopBar(onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                PremiumCard(onClick = onPremium)

                SectionHeader("General")
                SettingsRow(Res.drawable.ic_language, "Languages", onOpenLanguage)
                SettingsRow(Res.drawable.ic_theme, "Theme") { dialogName = SettingsDialog.Theme.name }

                SectionHeader("About")
                SettingsRow(Res.drawable.ic_rate_us, "Rate Us") { dialogName = SettingsDialog.RateUs.name }
                SettingsRow(Res.drawable.ic_share_app, "Share App", onShareApp)
                SettingsRow(Res.drawable.ic_about_us, "About Us", onAboutUs)

                SectionHeader("Legal")
                SettingsRow(Res.drawable.ic_privacy_policy, "Privacy Policy", onPrivacyPolicy)
                SettingsRow(Res.drawable.ic_terms_and_conditions, "Terms of Use", onTermsOfUse)

                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )

        GlassDialogHost(
            visible = dialog != SettingsDialog.None,
            liquidState = liquidState,
            onDismiss = closeDialog,
        ) {
            when (shownDialog) {
                SettingsDialog.None -> Unit
                SettingsDialog.Theme -> ThemeDialog(
                    current = themeMode,
                    onApply = { mode ->
                        onApplyTheme(mode)
                        closeDialog()
                    },
                    onDismiss = closeDialog,
                )
                SettingsDialog.RateUs -> RateUsDialog(
                    onRate = { rating ->
                        closeDialog()
                        onRate(rating)
                    },
                    onDismiss = closeDialog,
                )
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.onBackground.copy(alpha = 0.07f))
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = colors.onBackground,
            )
        }
        Text(
            text = "Settings",
            modifier = Modifier.align(Alignment.Center),
            color = colors.onBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The artwork has no text, so the app name and the Try Now pill are laid over its empty left side. */
@Composable
private fun PremiumCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PremiumCardAspectRatio)
            .clip(PremiumCardShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Image(
            painter = painterResource(Res.drawable.img_premium_card),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 18.dp),
        ) {
            Text(
                text = "Pic Collage Maker",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "TRY NOW",
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(TryNowGradient))
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                color = Color(0xFF3A1D00),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** Raised pill row: a soft top-to-bottom sheen plus a light shadow, as in the design. */
@Composable
private fun SettingsRow(icon: DrawableResource, title: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bottom = lerp(colors.surface, colors.onSurface, 0.07f)

    Row(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .height(54.dp)
            .shadow(elevation = 3.dp, shape = RowShape, ambientColor = Color.Black.copy(alpha = 0.3f))
            .clip(RowShape)
            .background(Brush.verticalGradient(listOf(colors.surface, bottom)))
            .border(1.dp, colors.onSurface.copy(alpha = 0.22f), RowShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        // The drawables are single-color vectors, so tinting keeps them visible in dark mode.
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = colors.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.onSurface,
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    ThemePreviews {
        SettingsContent(
            themeMode = ThemeMode.System,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onPremium = {},
            onOpenLanguage = {},
            onApplyTheme = {},
            onRate = {},
            onShareApp = {},
            onAboutUs = {},
            onPrivacyPolicy = {},
            onTermsOfUse = {},
        )
    }
}
