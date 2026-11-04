package org.example.project.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BrandingWatermark
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.data.billing.BillingStatus
import org.example.project.data.billing.SubscriptionPlan
import org.example.project.data.billing.SubscriptionProduct
import org.example.project.i18n.AppLocale
import org.example.project.i18n.tr
import org.example.project.ui.common.ReadableContentMaxWidth
import org.example.project.ui.common.bubbleClick
import org.example.project.ui.common.isSideBySideEditor
import org.example.project.ui.common.readableWidth
import org.example.project.ui.common.rememberBubbleClick
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.settings.SettingsLinks
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.img_collage_premium
import photocollagemaker.shared.generated.resources.img_freestyle_premium
import photocollagemaker.shared.generated.resources.img_premium_scrolliing_card_filter
import photocollagemaker.shared.generated.resources.img_premium_scrolliing_card_layout
import photocollagemaker.shared.generated.resources.img_premium_scrolliing_card_sticker
import photocollagemaker.shared.generated.resources.img_premium_scrolliing_card_watermark
import photocollagemaker.shared.generated.resources.img_premium_scrolling_card_background
import photocollagemaker.shared.generated.resources.img_premium_scrolling_card_overlay
import photocollagemaker.shared.generated.resources.img_premium_scrolling_card_text
import photocollagemaker.shared.generated.resources.img_template_premium

/** Brand red: checkmarks, the PRO tab, the card outline and the Continue button. */
internal val PremiumRed = Color(0xFFE0282E)

/** Selected plan fill: the red → orange sweep of the design. Also the editors' Pro banner. */
internal val SelectedPlanGradient = Brush.horizontalGradient(listOf(Color(0xFFDD2A2E), Color(0xFFFF8A3D)))

/** The dark chip carrying a plan's badge ("SAVE 80%"). */
internal val PlanBadgeColor = Color(0xFF26262B)

// Chrome follows the app theme (AppTheme light/dark); only the brand accents are fixed.
private val PremiumText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val PremiumSubtext: Color @Composable get() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)

private val CardShape = RoundedCornerShape(20.dp)
private val PillShape = RoundedCornerShape(percent = 50)

/** Hero images, swiped through in this order. */
private val HeroImages: List<DrawableResource> = listOf(
    Res.drawable.img_template_premium,
    Res.drawable.img_collage_premium,
    Res.drawable.img_freestyle_premium,
)

/** Height ÷ width of the hero, taken from the 1206 × 1056 hero images. */
private const val HeroAspect = 1056f / 1206f

/**
 * Pages the pager pretends to have so the hero can keep swiping forward without hitting an end.
 * It starts in the middle, so a swipe backwards never reaches page 0 in practice either.
 */
private const val HeroLoopPageCount = 3_000

internal const val HeroAutoAdvanceMillis = 3_000L
internal const val HeroSlideMillis = 750

/** How far the parallax image lags behind its page, as a fraction of the page width. */
private const val HeroParallax = 0.45f

/** How far the features card rides up over the bottom of the hero. */
private val CardOverlap = 36.dp

/** How long after the paywall opens before its close button appears. */
private const val CloseButtonDelayMillis = 3_000L

/**
 * How each plan is presented. Prices, periods and trials are not here: they come from the store
 * ([SubscriptionProduct]), localized for the user, so the paywall can never show a price the store
 * won't charge. [fallbackUnit] only labels the period until the store answers.
 */
internal enum class PremiumPlan(
    val plan: SubscriptionPlan,
    private val englishTitle: String,
    private val englishFallbackUnit: String,
) {
    Weekly(SubscriptionPlan.Weekly, "Weekly", "week"),
    Monthly(SubscriptionPlan.Monthly, "Monthly", "month"),
    ;

    // In the app's current language; read them where they are shown, never keep them.
    val title: String get() = tr(englishTitle)
    val fallbackUnit: String get() = tr(englishFallbackUnit)

    companion object {
        /** Null for a store plan no longer on sale here (an old Monthly subscriber). */
        fun of(plan: SubscriptionPlan): PremiumPlan? = entries.firstOrNull { it.plan == plan }
    }
}

private val PremiumFeatures = listOf(
    "Unlimited Collages",
    "Premium Templates",
    "High-Quality Exports",
    "Remove Ads",
    "Exclusive Fonts & Filters",
)

/** One card of the in-app paywall's carousel: a picture, with its icon and label drawn over it. */
internal class FeatureCard(val image: DrawableResource, val icon: ImageVector, val englishLabel: String)

/** Shown in place of [PremiumFeatures] when the paywall is opened from inside the app. */
internal val FeatureCards = listOf(
    FeatureCard(Res.drawable.img_premium_scrolliing_card_sticker, Icons.Outlined.EmojiEmotions, "500+ Stickers"),
    FeatureCard(Res.drawable.img_premium_scrolliing_card_layout, Icons.Outlined.SpaceDashboard, "2000+ Layouts"),
    FeatureCard(Res.drawable.img_premium_scrolliing_card_filter, Icons.Outlined.FilterAlt, "100+ Filters"),
    FeatureCard(Res.drawable.img_premium_scrolling_card_text, Icons.Outlined.TextFields, "2000+ Fonts"),
    FeatureCard(Res.drawable.img_premium_scrolling_card_overlay, Icons.Outlined.Layers, "100+ Overlays"),
    FeatureCard(Res.drawable.img_premium_scrolling_card_background, Icons.Outlined.Wallpaper, "200+ Backgrounds"),
    FeatureCard(Res.drawable.img_premium_scrolliing_card_watermark, Icons.Outlined.BrandingWatermark, "No Watermark"),
)

/** Width ÷ height of the 607 × 573 card pictures. */
internal const val FeatureCardAspect = 607f / 573f

/** The centred card's width, as a fraction of the content's width; the neighbours peek in beside it. */
internal const val FeatureCardWidthFraction = 0.58f

/** How small the neighbouring cards are drawn, beside the centred one. */
internal const val FeatureCardSideScale = 0.86f

/** The pictures' own corner rounding (≈ 40 px of their 573 px height), so the shadow follows it. */
private val FeatureCardShape = RoundedCornerShape(percent = 7)

/** Room above and below the cards for their shadow, which the pager would otherwise clip. */
internal val FeatureCardShadowRoom = 10.dp

/**
 * @param fromSplash the paywall is part of the launch flow: closing it the first time offers the
 *   limited-time launch deal ([LaunchOfferDialog]) while its two hours last. Opened from inside the
 *   app, it just closes.
 */
@Composable
fun PremiumScreen(
    fromSplash: Boolean,
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

    // When the launch offer ends, while it is showing; null when it isn't.
    var offerEndsAt by rememberSaveable { mutableStateOf<Long?>(null) }
    // The offer is made once per paywall: closing past it (or on a second close) really closes.
    var offerMade by rememberSaveable { mutableStateOf(false) }
    val requestClose: () -> Unit = {
        when {
            offerEndsAt != null -> onClose()
            fromSplash && !offerMade -> {
                offerMade = true
                val endsAt = viewModel.claimLaunchOffer()
                if (endsAt != null) offerEndsAt = endsAt else onClose()
            }
            else -> onClose()
        }
    }

    // Back does exactly what the close button does (and, over the offer, what its close does).
    // NavDisplay only handles back while there is an entry to pop, so without this the launch
    // paywall (alone on the stack) would close the app.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = requestClose,
    )

    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PremiumEvent.Message -> showMessage(event.text)
                PremiumEvent.Unlocked -> currentOnClose()
            }
        }
    }

    PremiumContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onClose = requestClose,
        featureCards = !fromSplash,
        onStart = { plan -> viewModel.purchase(plan.plan) },
        offerEndsAt = offerEndsAt,
        onOfferTryNow = { viewModel.purchase(SubscriptionPlan.Monthly) },
        // Leaving the offer (its close, or the clock running out) leaves the paywall too.
        onOfferClose = onClose,
        onRestore = viewModel::restore,
        onTermsAndPrivacy = {
            val url = SettingsLinks.PrivacyPolicyUrl.ifBlank { SettingsLinks.TermsOfUseUrl }
            if (url.isBlank()) showMessage(tr("Terms & Privacy is coming soon")) else uriHandler.openUri(url)
        },
        modifier = modifier,
    )
}

@Composable
private fun PremiumContent(
    uiState: PremiumUiState,
    snackbarHostState: SnackbarHostState,
    onClose: (() -> Unit)?,
    onStart: (PremiumPlan) -> Unit,
    onRestore: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    featureCards: Boolean = false,
    offerEndsAt: Long? = null,
    onOfferTryNow: () -> Unit = {},
    onOfferClose: () -> Unit = {},
) {
    // Saved by name: an enum is not saveable on every platform, a String is.
    var selectedName by rememberSaveable { mutableStateOf(PremiumPlan.Weekly.name) }
    val selected = PremiumPlan.valueOf(selectedName)
    val subscribed = uiState.billing.activePlan != null
    val activePlan = uiState.billing.activePlan?.let(PremiumPlan::of)
    // A subscriber opens on the plan they already have.
    LaunchedEffect(activePlan) {
        if (activePlan != null) selectedName = activePlan.name
    }
    val products = uiState.billing.products
    val savings = savingsOverWeeklyPercent(products[SubscriptionPlan.Weekly], products[SubscriptionPlan.Monthly])

    val liquidState = rememberLiquidState()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // A phone on its side: stacked, the hero alone is taller than the screen, so it takes the
        // left side with the features card over its faded bottom, and the plans and button sit
        // beside it, all on the first screen. The launch offer goes wide there for the same reason.
        val landscape = isSideBySideEditor(maxWidth, maxHeight)
        val plans: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PremiumPlan.entries.forEach { plan ->
                    PlanPill(
                        plan = plan,
                        product = products[plan.plan],
                        loading = uiState.billing.status == BillingStatus.Connecting,
                        badge = when {
                            plan == activePlan -> tr("CURRENT")
                            plan == PremiumPlan.Monthly && savings != null -> tr("SAVE {0}%", savings)
                            else -> null
                        },
                        selected = plan == selected,
                        onSelect = { selectedName = plan.name },
                    )
                }
            }
        }
        val footer: @Composable () -> Unit = {
            PremiumFooter(
                startLabel = when {
                    selected == activePlan -> tr("Current Plan")
                    subscribed -> tr("Switch Plan")
                    products[selected.plan]?.freeTrial != null -> tr("Start Free Trial")
                    else -> tr("Continue")
                },
                startEnabled = selected != activePlan,
                busy = uiState.busy,
                onStart = { onStart(selected) },
                onTermsAndPrivacy = onTermsAndPrivacy,
            )
        }
        val onRestoreClick = { if (!uiState.busy) onRestore() }
        if (landscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .liquefiable(liquidState),
            ) {
                PremiumSideHero(
                    onClose = onClose,
                    onRestore = onRestoreClick,
                    featureCards = featureCards,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // The notch is at the left or right edge; the hero is under the left one.
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        plans()
                    }
                    footer()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .liquefiable(liquidState)
                    .navigationBarsPadding(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PremiumTop(onClose = onClose, onRestore = onRestoreClick, featureCards = featureCards)
                    Spacer(Modifier.height(14.dp))
                    Box(modifier = Modifier.readableWidth().padding(horizontal = 20.dp)) { plans() }
                    Spacer(Modifier.height(16.dp))
                }
                footer()
            }
        }

        LaunchOfferHost(
            endsAt = offerEndsAt,
            liquidState = liquidState,
            monthly = products[SubscriptionPlan.Monthly],
            weekly = products[SubscriptionPlan.Weekly],
            busy = uiState.busy,
            onTryNow = onOfferTryNow,
            onClose = onOfferClose,
            wide = landscape,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp),
        )
    }
}

/**
 * The looping hero pager over [imageCount] images, advancing on its own every [HeroAutoAdvanceMillis].
 * Its page modulo [imageCount] is the image on show.
 */
@Composable
internal fun rememberHeroPagerState(imageCount: Int = HeroImages.size): PagerState {
    val pagerState = rememberPagerState(
        initialPage = HeroLoopPageCount / 2 - (HeroLoopPageCount / 2) % imageCount,
        pageCount = { HeroLoopPageCount },
    )
    // Keyed on the settled page, so a user swipe restarts the countdown instead of being followed
    // by an immediate jump; and paused while a finger is on the hero.
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(pagerState.settledPage, dragged) {
        if (dragged) return@LaunchedEffect
        delay(HeroAutoAdvanceMillis)
        pagerState.animateScrollToPage(
            page = pagerState.currentPage + 1,
            animationSpec = tween(HeroSlideMillis, easing = FastOutSlowInEasing),
        )
    }
    return pagerState
}

/**
 * The swiping hero, with the features card riding up over its faded-out bottom edge, or with
 * [featureCards] the carousel of [FeatureCards] in its place. Then the hero doesn't swipe: two
 * things sliding by at once read as clutter, so the header is the centred card's blurred colours
 * (as in the Shop's Premium tab) under the title, and only the cards move.
 */
@Composable
private fun PremiumTop(onClose: (() -> Unit)?, onRestore: () -> Unit, featureCards: Boolean) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Full width, but no taller than the hero is at the content's capped width: on a tablet the
        // width-derived height would push the plans and the button below the fold.
        val heroHeight = minOf(maxWidth, ReadableContentMaxWidth) * HeroAspect
        val cardsPagerState = if (featureCards) rememberHeroPagerState(FeatureCards.size) else null
        if (cardsPagerState != null) {
            CardBackdrop(
                backdrops = rememberBlurredBackdrops(FeatureCards),
                current = cardsPagerState.currentPage % FeatureCards.size,
                modifier = Modifier.fillMaxWidth().height(heroHeight).clipToBounds(),
            )
            // Centred in the part of the header the cards leave clear.
            Box(
                modifier = Modifier.fillMaxWidth().height(heroHeight - CardOverlap),
                contentAlignment = Alignment.Center,
            ) {
                GetPremiumHeading(compact = false)
            }
        } else {
            val pagerState = rememberHeroPagerState()
            HeroPager(pagerState, Modifier.fillMaxWidth().height(heroHeight))
            HeroIndicator(
                pagerState = pagerState,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = heroHeight - CardOverlap - 18.dp),
            )
        }
        // Top-left, below the status bar, where the design places it (≈ 8.7% across, 21.6% down the hero).
        if (onClose != null) CloseButton(
            onClose = onClose,
            modifier = Modifier.offset(x = maxWidth * 0.087f - 20.dp, y = heroHeight * 0.216f - 20.dp),
        )
        RestoreChip(
            onClick = onRestore,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-16).dp, y = heroHeight * 0.216f - 16.dp),
        )
        if (cardsPagerState != null) {
            FeatureCarousel(
                pagerState = cardsPagerState,
                cardWidth = minOf(maxWidth, ReadableContentMaxWidth) * FeatureCardWidthFraction,
                modifier = Modifier.padding(top = heroHeight - CardOverlap - FeatureCardShadowRoom),
            )
        } else {
            FeaturesCard(
                modifier = Modifier
                    .padding(top = heroHeight - CardOverlap)
                    .readableWidth()
                    .padding(horizontal = 20.dp),
            )
        }
    }
}

/**
 * The hero beside the plans, on a phone on its side: the photo at full height, faded at the bottom
 * where the features card sits over it (the stacked layout's overlap, turned into an overlay), with
 * close and Restore in its top corners.
 */
@Composable
private fun PremiumSideHero(
    onClose: (() -> Unit)?,
    onRestore: () -> Unit,
    featureCards: Boolean,
    modifier: Modifier = Modifier,
) {
    // The photo runs under the notch; what sits on it keeps clear of it.
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
    if (featureCards) {
        FeatureCardsSideHero(onClose = onClose, onRestore = onRestore, insets = insets, modifier = modifier)
        return
    }
    val pagerState = rememberHeroPagerState()
    Box(modifier = modifier) {
        HeroPager(pagerState, Modifier.fillMaxSize())
        if (onClose != null) CloseButton(
            onClose = onClose,
            modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(insets).padding(16.dp),
        )
        RestoreChip(
            onClick = onRestore,
            modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(insets).padding(top = 20.dp, end = 16.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(insets)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroIndicator(pagerState = pagerState)
            Spacer(Modifier.height(12.dp))
            // Small enough that the photo still shows above it.
            FeaturesCard(modifier = Modifier.padding(horizontal = 20.dp), compact = true)
        }
    }
}

/**
 * [PremiumSideHero] opened from inside the app: the centred card's blurred colours at full height,
 * the title in the middle of what the cards leave clear, and the cards at the foot.
 */
@Composable
private fun FeatureCardsSideHero(
    onClose: (() -> Unit)?,
    onRestore: () -> Unit,
    insets: WindowInsets,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberHeroPagerState(FeatureCards.size)
    Box(modifier = modifier.clipToBounds()) {
        CardBackdrop(
            backdrops = rememberBlurredBackdrops(FeatureCards),
            current = pagerState.currentPage % FeatureCards.size,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(insets).padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                GetPremiumHeading(compact = true)
            }
            FeatureCarousel(pagerState = pagerState, cardWidth = 170.dp)
        }
        if (onClose != null) CloseButton(
            onClose = onClose,
            modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(insets).padding(16.dp),
        )
        RestoreChip(
            onClick = onRestore,
            modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(insets).padding(top = 20.dp, end = 16.dp),
        )
    }
}

/**
 * The in-app paywall's header: an "Unlock Unlimited Power" chip, the title and a line on what it
 * unlocks. Fixed colours: it always sits on the blurred card. [compact] beside the plans, where the
 * cards leave it less room.
 */
@Composable
private fun GetPremiumHeading(compact: Boolean) {
    val textShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), blurRadius = 8f)
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(Color.White.copy(alpha = 0.16f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), PillShape)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFFFC15E), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = tr("Unlock Unlimited Power").uppercase(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Text(
            text = tr("Get Premium"),
            style = TextStyle(color = Color.White, shadow = textShadow),
            fontSize = if (compact) 28.sp else 34.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(if (compact) 4.dp else 6.dp))
        Text(
            text = tr("Access all pro tools, fonts, stickers, backgrounds, filters and assets with no watermark"),
            style = TextStyle(color = Color.White.copy(alpha = 0.85f), shadow = textShadow),
            fontSize = if (compact) 12.sp else 14.sp,
            lineHeight = if (compact) 16.sp else 19.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HeroPager(pagerState: PagerState, modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier = modifier) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                Image(
                    painter = painterResource(HeroImages[page % HeroImages.size]),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // -1 (next page, fully right) … 0 (centered) … 1 (previous, fully left).
                            val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                .coerceIn(-1f, 1f)
                            // The photo slides slower than its page, and eases in from a slight zoom.
                            translationX = offset * size.width * HeroParallax
                            val scale = 1f + 0.12f * offset.absoluteValue
                            scaleX = scale
                            scaleY = scale
                        },
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                )
            }
        }
        // Fades the photo into the page, so the features card sits on it rather than on a hard edge.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.38f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, background))),
        )
    }
}

@Composable
private fun HeroIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    val current = pagerState.currentPage % HeroImages.size
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HeroImages.indices.forEach { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 20.dp else 7.dp)
            val color by animateColorAsState(if (active) PremiumRed else Color.White.copy(alpha = 0.75f))
            Box(Modifier.size(width = width, height = 7.dp).clip(PillShape).background(color))
        }
    }
}

@Composable
internal fun CloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    // The close button only shows up a while after the paywall opens, so the offer gets seen first.
    var closeDelayElapsed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(CloseButtonDelayMillis)
        closeDelayElapsed = true
    }
    AnimatedVisibility(
        visible = closeDelayElapsed,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        modifier = modifier,
    ) {
        val bubble = rememberBubbleClick()
        // Fixed colours: it always sits on a photo, whatever the theme.
        Box(
            modifier = Modifier
                .size(40.dp)
                .bubbleClick(bubble, Color.White)
                .clip(CircleShape)
                .background(Color(0xFFD9D4D6).copy(alpha = 0.9f))
                .clickable(
                    interactionSource = bubble.interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = { bubble.tap(onClose) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = tr("Close"), tint = Color.Black, modifier = Modifier.size(24.dp))
        }
    }
}

/** Restore Purchase: both stores require a way to restore from the paywall itself. */
@Composable
internal fun RestoreChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = tr("Restore"),
        modifier = modifier
            .clip(PillShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun FeaturesCard(modifier: Modifier = Modifier, compact: Boolean = false) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = CardShape, spotColor = PremiumRed.copy(alpha = 0.5f))
            .clip(CardShape)
            .background(Brush.verticalGradient(listOf(surface, PremiumRed.copy(alpha = 0.08f).compositeOver(surface))))
            .border(1.dp, PremiumRed.copy(alpha = 0.7f), CardShape),
    ) {
        Column(
            // Compact over the side hero, so the photo still shows above it.
            modifier = Modifier.padding(horizontal = 20.dp, vertical = if (compact) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 13.dp),
        ) {
            PremiumFeatures.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PremiumRed, modifier = Modifier.size(if (compact) 20.dp else 22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = tr(feature),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = if (compact) 15.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        // Clipped by the card, so its top-right corner follows the card's rounding.
        Text(
            text = "PRO",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(PremiumRed, RoundedCornerShape(bottomStart = 12.dp))
                .padding(horizontal = 14.dp, vertical = 3.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/**
 * The in-app paywall's cards, [cardWidth] wide and centred, sliding on by themselves like the hero
 * (and looping the same way, through [pagerState] from [rememberHeroPagerState]), with the
 * neighbours shrunk a little and peeking in at the sides.
 */
@Composable
private fun FeatureCarousel(pagerState: PagerState, cardWidth: Dp, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = ((maxWidth - cardWidth) / 2).coerceAtLeast(0.dp)),
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            FeatureCardItem(
                card = FeatureCards[page % FeatureCards.size],
                cardWidth = cardWidth,
                modifier = Modifier
                    .padding(vertical = FeatureCardShadowRoom)
                    .graphicsLayer {
                        // 0 centred … 1 a whole page to either side.
                        val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .absoluteValue
                            .coerceAtMost(1f)
                        val scale = lerp(1f, FeatureCardSideScale, offset)
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
    }
}

@Composable
internal fun FeatureCardItem(card: FeatureCard, cardWidth: Dp, modifier: Modifier = Modifier) {
    // Everything on the card is sized from its width, so the side-by-side carousel's smaller cards
    // look the same.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(FeatureCardAspect)
            .shadow(elevation = 8.dp, shape = FeatureCardShape, spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(FeatureCardShape),
    ) {
        Image(
            painter = painterResource(card.image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        // Darkens the foot of the picture, so the white label reads on the lighter ones.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))),
        )
        // Fixed colours: they always sit on the card's picture, whatever the theme.
        Box(
            modifier = Modifier
                .padding(cardWidth * 0.06f)
                .size(cardWidth * 0.17f)
                .clip(RoundedCornerShape(percent = 30))
                .background(Color.White.copy(alpha = 0.28f))
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(percent = 30)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(card.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(cardWidth * 0.1f))
        }
        // Shrinks to fit on one line: at the full size "200+ Backgrounds" (and longer translations)
        // would wrap, and one line shows only the first word.
        BasicText(
            text = tr(card.englishLabel),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = cardWidth * 0.05f)
                .padding(bottom = cardWidth * 0.07f),
            style = TextStyle(
                color = Color.White,
                shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 2f), blurRadius = 6f),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(
                minFontSize = (cardWidth.value * 0.05f).sp,
                maxFontSize = (cardWidth.value * 0.105f).sp,
                stepSize = 0.5.sp,
            ),
        )
    }
}

@Composable
internal fun PlanPill(
    plan: PremiumPlan,
    product: SubscriptionProduct?,
    loading: Boolean,
    badge: String?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val trial = product?.freeTrial?.localizedTrialLabel()
    val unit = product?.billingPeriod?.localizedUnitLabel() ?: plan.fallbackUnit
    val subtitle = when {
        product == null -> if (loading) tr("Loading price…") else tr("Price unavailable")
        trial != null -> tr("{0}, then every {1}", trial, unit)
        else -> tr("Renews every {0}", unit)
    }
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val unselectedFill = Brush.verticalGradient(listOf(surface, onSurface.copy(alpha = 0.06f).compositeOver(surface)))
    // The red fill fades in over the plain one, since a Brush itself can't be animated.
    val selectedFraction by animateFloatAsState(if (selected) 1f else 0f, tween(280))
    val scale by animateFloatAsState(if (selected) 1f else 0.97f, tween(280))
    val titleColor by animateColorAsState(if (selected) Color.White else onSurface)
    val subtitleColor by animateColorAsState(if (selected) Color.White.copy(alpha = 0.85f) else onSurface.copy(alpha = 0.7f))
    val borderColor by animateColorAsState(if (selected) Color.Transparent else onSurface.copy(alpha = 0.22f))

    // Top padding leaves room for the half of the badge that sits above the pill's top edge.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(
                    elevation = if (selected) 10.dp else 3.dp,
                    shape = PillShape,
                    spotColor = if (selected) PremiumRed else Color.Black,
                )
                .clip(PillShape)
                .background(unselectedFill)
                .background(SelectedPlanGradient, alpha = selectedFraction)
                .border(1.dp, borderColor, PillShape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.title, color = titleColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = subtitleColor, fontSize = 13.sp)
            }
            Text(
                text = product?.formattedPrice ?: "—",
                color = titleColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (badge != null) {
            Text(
                text = badge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 26.dp)
                    // Centers the badge on the pill's top edge, whatever its measured height.
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, -placeable.height / 2)
                        }
                    }
                    .background(PlanBadgeColor, PillShape)
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun PremiumFooter(
    startLabel: String,
    startEnabled: Boolean,
    busy: Boolean,
    onStart: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
) {
    val terms = rememberPremiumTerms(onTermsAndPrivacy)
    Column(
        modifier = Modifier.readableWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = terms,
            color = PremiumSubtext,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        ContinueButton(label = startLabel, enabled = startEnabled && !busy, busy = busy, onClick = onStart)
        Spacer(Modifier.height(8.dp))
        Text(tr("Auto Renewable. Cancel Anytime"), color = PremiumSubtext, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * "By continuing you agree to our Terms & Privacy policies. Subscription will auto-renew…", with the
 * policies as a link. Both stores require the auto-renewal terms beside the purchase button.
 */
@Composable
internal fun rememberPremiumTerms(onTermsAndPrivacy: () -> Unit): AnnotatedString {
    val linkColor = PremiumText
    // Keyed on the language too: the translated text is built inside the remembered value.
    return remember(linkColor, onTermsAndPrivacy, AppLocale.code) {
        buildAnnotatedString {
            append(tr("By continuing you agree to our "))
            withLink(
                LinkAnnotation.Clickable(
                    tag = "terms",
                    styles = TextLinkStyles(SpanStyle(color = linkColor, fontWeight = FontWeight.SemiBold)),
                ) { onTermsAndPrivacy() },
            ) {
                append(tr("Terms & Privacy policies"))
            }
            append(tr(". Subscription will auto-renew. Cancel anytime."))
        }
    }
}

@Composable
internal fun ContinueButton(
    label: String,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            // Dimmed rather than greyed, so the brand red stays recognisable.
            .graphicsLayer { alpha = if (enabled || busy) 1f else 0.55f }
            .shadow(elevation = 12.dp, shape = PillShape, spotColor = PremiumRed)
            .clip(PillShape)
            .background(PremiumRed)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (trailingIcon != null) {
                    Spacer(Modifier.width(10.dp))
                    Icon(trailingIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Preview
@Composable
private fun PremiumScreenPreview() {
    ThemePreviews {
        PremiumContent(
            uiState = PremiumUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onClose = {},
            onStart = {},
            onRestore = {},
            onTermsAndPrivacy = {},
        )
    }
}

@Preview
@Composable
private fun PremiumScreenInAppPreview() {
    ThemePreviews {
        PremiumContent(
            uiState = PremiumUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onClose = {},
            onStart = {},
            onRestore = {},
            onTermsAndPrivacy = {},
            featureCards = true,
        )
    }
}
