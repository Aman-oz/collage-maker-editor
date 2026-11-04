package org.example.project.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrandingWatermark
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch
import org.example.project.data.billing.BillingStatus
import org.example.project.data.billing.SubscriptionPlan
import org.example.project.i18n.tr
import org.example.project.ui.common.ReadableContentMaxWidth
import org.example.project.ui.common.isSideBySideEditor
import org.example.project.ui.common.readableWidth
import org.example.project.ui.premium.CardBackdrop
import org.example.project.ui.premium.FeatureCard
import org.example.project.ui.premium.FeatureCardItem
import org.example.project.ui.premium.FeatureCardShadowRoom
import org.example.project.ui.premium.FeatureCardSideScale
import org.example.project.ui.premium.FeatureCardWidthFraction
import org.example.project.ui.premium.PlanPill
import org.example.project.ui.premium.PremiumEvent
import org.example.project.ui.premium.PremiumFooter
import org.example.project.ui.premium.PremiumPlan
import org.example.project.ui.premium.PremiumUiState
import org.example.project.ui.premium.PremiumViewModel
import org.example.project.ui.premium.RestoreChip
import org.example.project.ui.premium.rememberBlurredBackdrops
import org.example.project.ui.premium.rememberHeroPagerState
import org.example.project.ui.premium.savingsOverWeeklyPercent
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.settings.SettingsLinks
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.img_premium_scrolliing_card_filter
import photocollagemaker.shared.generated.resources.img_premium_scrolliing_card_layout
import photocollagemaker.shared.generated.resources.img_premium_scrolliing_card_sticker
import photocollagemaker.shared.generated.resources.img_premium_scrolliing_card_watermark
import photocollagemaker.shared.generated.resources.img_premium_scrolling_card_background
import photocollagemaker.shared.generated.resources.img_premium_scrolling_card_overlay
import photocollagemaker.shared.generated.resources.img_premium_scrolling_card_text

/**
 * The Shop's own cards. Kept apart from the paywall's [org.example.project.ui.premium.FeatureCards]
 * so the Shop's Premium tab can change without touching the paywall.
 */
private val ShopPremiumCards = listOf(
    FeatureCard(Res.drawable.img_premium_scrolliing_card_sticker, Icons.Outlined.EmojiEmotions, "500+ Stickers"),
    FeatureCard(Res.drawable.img_premium_scrolliing_card_layout, Icons.Outlined.SpaceDashboard, "2000+ Layouts"),
    FeatureCard(Res.drawable.img_premium_scrolliing_card_filter, Icons.Outlined.FilterAlt, "100+ Filters"),
    FeatureCard(Res.drawable.img_premium_scrolling_card_text, Icons.Outlined.TextFields, "2000+ Fonts"),
    FeatureCard(Res.drawable.img_premium_scrolling_card_overlay, Icons.Outlined.Layers, "100+ Overlays"),
    FeatureCard(Res.drawable.img_premium_scrolling_card_background, Icons.Outlined.Wallpaper, "200+ Backgrounds"),
    FeatureCard(Res.drawable.img_premium_scrolliing_card_watermark, Icons.Outlined.BrandingWatermark, "No Watermark"),
)

/** Height of the header behind the cards, past the cards themselves. */
private val HeaderPadding = 28.dp

/**
 * The Shop's Premium tab: the paywall's cards sliding by on their own over a blurred copy of the
 * card in the middle, which fades to the next one as the cards move, then the plans and the
 * button. It sits under the Shop's own top bar, so it has no close button and no back handling;
 * [onUnlocked] is called once a purchase or restore unlocks premium.
 */
@Composable
internal fun ShopPremiumTab(
    onUnlocked: () -> Unit,
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
    val currentOnUnlocked by rememberUpdatedState(onUnlocked)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PremiumEvent.Message -> showMessage(event.text)
                PremiumEvent.Unlocked -> currentOnUnlocked()
            }
        }
    }
    ShopPremiumContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onStart = { plan -> viewModel.purchase(plan.plan) },
        onRestore = viewModel::restore,
        onTermsAndPrivacy = {
            val url = SettingsLinks.PrivacyPolicyUrl.ifBlank { SettingsLinks.TermsOfUseUrl }
            if (url.isBlank()) showMessage(tr("Terms & Privacy is coming soon")) else uriHandler.openUri(url)
        },
        modifier = modifier,
    )
}

@Composable
private fun ShopPremiumContent(
    uiState: PremiumUiState,
    snackbarHostState: SnackbarHostState,
    onStart: (PremiumPlan) -> Unit,
    onRestore: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
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
    val onRestoreClick = { if (!uiState.busy) onRestore() }

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

    val pagerState = rememberHeroPagerState(ShopPremiumCards.size)
    val backdrops = rememberBlurredBackdrops(ShopPremiumCards)
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // A phone on its side: the header takes the left side at full height, the plans and the
        // button the right, as the paywall does.
        if (isSideBySideEditor(maxWidth, maxHeight)) {
            Row(modifier = Modifier.fillMaxSize()) {
                CardsHeader(
                    pagerState = pagerState,
                    backdrops = backdrops,
                    cardWidth = 200.dp,
                    onRestore = onRestoreClick,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.End)),
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
            val cardWidth = minOf(maxWidth, ReadableContentMaxWidth) * FeatureCardWidthFraction
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CardsHeader(
                        pagerState = pagerState,
                        backdrops = backdrops,
                        cardWidth = cardWidth,
                        onRestore = onRestoreClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.readableWidth().padding(horizontal = 20.dp)) { plans() }
                    Spacer(Modifier.height(16.dp))
                }
                footer()
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp),
        )
    }
}

/**
 * The cards carousel over the blurred picture of the card in the middle. The backdrop changes
 * with [PagerState.currentPage], which flips halfway through each slide, and crossfades over
 * the slide's length, so it fades from one card to the next while they move.
 */
@Composable
private fun CardsHeader(
    pagerState: PagerState,
    backdrops: List<ImageBitmap>?,
    cardWidth: Dp,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = pagerState.currentPage % ShopPremiumCards.size
    Box(modifier = modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        CardBackdrop(backdrops = backdrops, current = current, modifier = Modifier.matchParentSize())
        CardsCarousel(
            pagerState = pagerState,
            cardWidth = cardWidth,
            modifier = Modifier.padding(vertical = HeaderPadding),
        )
        RestoreChip(
            onClick = onRestore,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 12.dp),
        )
    }
}

/** [ShopPremiumCards], [cardWidth] wide and centred, with the neighbours shrunk and peeking in. */
@Composable
private fun CardsCarousel(pagerState: PagerState, cardWidth: Dp, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = ((maxWidth - cardWidth) / 2).coerceAtLeast(0.dp)),
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            FeatureCardItem(
                card = ShopPremiumCards[page % ShopPremiumCards.size],
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

@Preview
@Composable
private fun ShopPremiumTabPreview() {
    ThemePreviews {
        ShopPremiumContent(
            uiState = PremiumUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onStart = {},
            onRestore = {},
            onTermsAndPrivacy = {},
        )
    }
}
