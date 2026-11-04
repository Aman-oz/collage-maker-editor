package org.example.project.ui.premium

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.example.project.data.billing.BillingStatus
import org.example.project.data.billing.PremiumPack
import org.example.project.data.billing.SubscriptionPlan
import org.example.project.data.billing.SubscriptionProduct
import org.example.project.i18n.tr
import org.example.project.ui.common.isSideBySideEditor
import org.example.project.ui.common.readableWidth
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.settings.SettingsLinks
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.img_premium_collage
import photocollagemaker.shared.generated.resources.img_premium_frame
import photocollagemaker.shared.generated.resources.img_premium_freestyle
import photocollagemaker.shared.generated.resources.img_premium_template_christmas

/**
 * Opens [ProOfferScreen] from wherever a [org.example.project.ui.common.ProFeatureBar] sits, with
 * the pack to offer on leaving it (if any). Provided by `AppNavDisplay`, so no editor or tool has to
 * pass a lambda down for the banner.
 */
internal val LocalOpenProOffer = staticCompositionLocalOf<(PremiumPack?) -> Unit> { {} }

/**
 * The one plan this paywall sells. Its price, period and any free trial come from the store, and the
 * copy follows them (a trial gets "Try for Rs 0", a plain plan "Continue"), so switching to the
 * monthly plan, or adding a trial in Play Console / App Store Connect, needs no other change here.
 */
internal val ProOfferPlan = SubscriptionPlan.Weekly

/** The hero pictures, swiped through in this order. */
private val ProOfferImages: List<DrawableResource> = listOf(
    Res.drawable.img_premium_template_christmas,
    Res.drawable.img_premium_freestyle,
    Res.drawable.img_premium_collage,
    Res.drawable.img_premium_frame,
)

/** How far a page shrinks as the next one slides over it. */
private const val RecedeScale = 0.12f

/** How slowly the picture on show drifts closer while it waits for the next ([HeroAutoAdvanceMillis]). */
private const val DriftZoom = 1.08f

/**
 * The single-plan paywall a [org.example.project.ui.common.ProFeatureBar] opens: a swiping hero of
 * what premium makes, one line about it, and [ProOfferPlan] with its trial. Purchase and restore are
 * [PremiumViewModel]'s, as on the full paywall.
 */
@Composable
fun ProOfferScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PremiumViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PremiumEvent.Message -> showMessage(event.text)
                PremiumEvent.Unlocked -> currentOnClose()
            }
        }
    }

    ProOfferContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onClose = onClose,
        onStart = { viewModel.purchase(ProOfferPlan) },
        onRestore = viewModel::restore,
        onTermsAndPrivacy = {
            val url = SettingsLinks.PrivacyPolicyUrl.ifBlank { SettingsLinks.TermsOfUseUrl }
            if (url.isBlank()) showMessage(tr("Terms & Privacy is coming soon")) else uriHandler.openUri(url)
        },
        modifier = modifier,
    )
}

@Composable
private fun ProOfferContent(
    uiState: PremiumUiState,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onStart: () -> Unit,
    onRestore: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberHeroPagerState(ProOfferImages.size)
    val onRestoreClick = { if (!uiState.busy) onRestore() }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // A phone on its side: the hero takes the left side at full height and the offer sits
        // beside it, as on the full paywall.
        if (isSideBySideEditor(maxWidth, maxHeight)) {
            Row(modifier = Modifier.fillMaxSize()) {
                OfferHero(
                    pagerState = pagerState,
                    onClose = onClose,
                    onRestore = onRestoreClick,
                    insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OfferPanel(pagerState, uiState, onStart, onTermsAndPrivacy)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                // The hero takes whatever the offer leaves, cropped, so the button is always on screen.
                OfferHero(
                    pagerState = pagerState,
                    onClose = onClose,
                    onRestore = onRestoreClick,
                    insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OfferPanel(pagerState, uiState, onStart, onTermsAndPrivacy)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 140.dp),
        )
    }
}

/** The swiping pictures, with close and Restore in their top corners, clear of [insets]. */
@Composable
private fun OfferHero(
    pagerState: PagerState,
    onClose: () -> Unit,
    onRestore: () -> Unit,
    insets: WindowInsets,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier = modifier.clipToBounds()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            OfferPage(pagerState, page)
        }
        // Softens the hero's foot into the page, so the dots and the line below sit on one surface.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.14f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, background))),
        )
        CloseButton(
            onClose = onClose,
            modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(insets).padding(16.dp),
        )
        RestoreChip(
            onClick = onRestore,
            modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(insets).padding(top = 24.dp, end = 16.dp),
        )
    }
}

/**
 * One picture of the hero. The page being left stays put, shrinking and dimming as the next slides
 * over it like a card dealt onto a stack, and the picture on show drifts slowly closer while it waits.
 */
@Composable
private fun OfferPage(pagerState: PagerState, page: Int) {
    val drift = remember { Animatable(1f) }
    val settled = pagerState.settledPage == page
    LaunchedEffect(settled) {
        if (settled) drift.animateTo(DriftZoom, tween(HeroAutoAdvanceMillis.toInt() + HeroSlideMillis, easing = LinearEasing))
    }
    Image(
        painter = painterResource(ProOfferImages[page % ProOfferImages.size]),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // 0 on show, up to 1 as it leaves to the left; negative while it comes in from the right.
                val leaving = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                val recede = leaving.coerceAtLeast(0f)
                // Cancels the pager's own slide, so the leaving page stays where it is, under the next.
                translationX = recede * size.width
                val scale = drift.value * (1f - RecedeScale * recede)
                scaleX = scale
                scaleY = scale
                alpha = 1f - 0.6f * recede
            },
    )
}

/** The dots, the line about premium, the plan's trial and price, the button and the terms. */
@Composable
private fun OfferPanel(
    pagerState: PagerState,
    uiState: PremiumUiState,
    onStart: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
) {
    val product = uiState.billing.products[ProOfferPlan]
    val subscribed = uiState.billing.activePlan != null
    val copy = offerCopy(product, loading = uiState.billing.status == BillingStatus.Connecting, subscribed = subscribed)
    val subtext = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
    Column(
        modifier = Modifier.readableWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        OfferIndicator(pagerState)
        Spacer(Modifier.height(18.dp))
        Text(
            text = tr("Create stunning collages, beautiful frames, with unlimited premium design tools"),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(24.dp))
        // Crossfades as the store's answer arrives, rather than popping from "Loading price…".
        AnimatedContent(
            targetState = copy.headline to copy.chargeLine,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
        ) { (headline, chargeLine) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = headline,
                    color = if (copy.highlight) PremiumRed else subtext,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                if (chargeLine != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = chargeLine,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ContinueButton(
            label = copy.button,
            enabled = !subscribed && !uiState.busy,
            busy = uiState.busy,
            onClick = onStart,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = rememberPremiumTerms(onTermsAndPrivacy),
            color = subtext,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OfferIndicator(pagerState: PagerState) {
    val current = pagerState.currentPage % ProOfferImages.size
    val inactive = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ProOfferImages.indices.forEach { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 20.dp else 8.dp)
            val color by animateColorAsState(if (active) PremiumRed else inactive)
            Box(Modifier.size(width = width, height = 8.dp).clip(RoundedCornerShape(percent = 50)).background(color))
        }
    }
}

/**
 * What the offer says about [ProOfferPlan], in the app's language.
 *
 * @param highlight whether [headline] is the red offer line rather than a muted status.
 */
private data class OfferCopy(val headline: String, val chargeLine: String?, val button: String, val highlight: Boolean)

private fun offerCopy(product: SubscriptionProduct?, loading: Boolean, subscribed: Boolean): OfferCopy {
    if (product == null) {
        val status = if (loading) tr("Loading price…") else tr("Price unavailable")
        return OfferCopy(status, null, tr("Continue"), highlight = false)
    }
    val unit = product.billingPeriod.localizedUnitLabel()
    val price = product.formattedPrice
    val trial = product.freeTrial
    return when {
        subscribed -> OfferCopy("$price/$unit", null, tr("Current Plan"), highlight = true)
        trial != null -> OfferCopy(
            headline = trial.localizedTrialLabel(),
            chargeLine = tr("Then {0}/{1} will be charged.", price, unit),
            // "Try for Rs 0": nothing is charged today, in the store's own currency format.
            button = formatPriceLike(price, 0)?.let { tr("Try for {0}", it) } ?: tr("Start Free Trial"),
            highlight = true,
        )
        else -> OfferCopy("$price/$unit", tr("Auto Renewable. Cancel Anytime"), tr("Continue"), highlight = true)
    }
}

@Preview
@Composable
private fun ProOfferScreenPreview() {
    ThemePreviews {
        ProOfferContent(
            uiState = PremiumUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onClose = {},
            onStart = {},
            onRestore = {},
            onTermsAndPrivacy = {},
        )
    }
}
