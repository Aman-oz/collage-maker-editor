package org.example.project.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import org.example.project.ui.common.bubbleClick
import org.example.project.ui.common.rememberBubbleClick
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.settings.SettingsLinks
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.img_collage_premium
import photocollagemaker.shared.generated.resources.img_freestyle_premium
import photocollagemaker.shared.generated.resources.img_template_premium

/** Brand red: checkmarks, the PRO tab, the card outline and the Continue button. */
internal val PremiumRed = Color(0xFFE0282E)

/** Selected plan fill: the red → orange sweep of the design. */
private val SelectedPlanGradient = Brush.horizontalGradient(listOf(Color(0xFFDD2A2E), Color(0xFFFF8A3D)))

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

private const val HeroAutoAdvanceMillis = 3_000L
private const val HeroSlideMillis = 750

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
    val title: String,
    val fallbackUnit: String,
) {
    Weekly(SubscriptionPlan.Weekly, "Weekly", "week"),
    Annual(SubscriptionPlan.Yearly, "Annual", "year"),
    ;

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
        onStart = { plan -> viewModel.purchase(plan.plan) },
        offerEndsAt = offerEndsAt,
        onOfferTryNow = { viewModel.purchase(SubscriptionPlan.Yearly) },
        // Leaving the offer (its close, or the clock running out) leaves the paywall too.
        onOfferClose = onClose,
        onRestore = viewModel::restore,
        onTermsAndPrivacy = {
            val url = SettingsLinks.PrivacyPolicyUrl.ifBlank { SettingsLinks.TermsOfUseUrl }
            if (url.isBlank()) showMessage("Terms & Privacy is coming soon") else uriHandler.openUri(url)
        },
        modifier = modifier,
    )
}

@Composable
private fun PremiumContent(
    uiState: PremiumUiState,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onStart: (PremiumPlan) -> Unit,
    onRestore: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
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
    val savings = annualSavingsPercent(products[SubscriptionPlan.Weekly], products[SubscriptionPlan.Yearly])

    val liquidState = rememberLiquidState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
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
                PremiumTop(onClose = onClose, onRestore = { if (!uiState.busy) onRestore() })
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PremiumPlan.entries.forEach { plan ->
                        PlanPill(
                            plan = plan,
                            product = products[plan.plan],
                            loading = uiState.billing.status == BillingStatus.Connecting,
                            badge = when {
                                plan == activePlan -> "CURRENT"
                                plan == PremiumPlan.Annual && savings != null -> "SAVE $savings%"
                                else -> null
                            },
                            selected = plan == selected,
                            onSelect = { selectedName = plan.name },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            PremiumFooter(
                startLabel = when {
                    selected == activePlan -> "Current Plan"
                    subscribed -> "Switch Plan"
                    products[selected.plan]?.freeTrial != null -> "Start Free Trial"
                    else -> "Continue"
                },
                startEnabled = selected != activePlan,
                busy = uiState.busy,
                onStart = { onStart(selected) },
                onTermsAndPrivacy = onTermsAndPrivacy,
            )
        }

        LaunchOfferHost(
            endsAt = offerEndsAt,
            liquidState = liquidState,
            yearly = products[SubscriptionPlan.Yearly],
            weekly = products[SubscriptionPlan.Weekly],
            busy = uiState.busy,
            onTryNow = onOfferTryNow,
            onClose = onOfferClose,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp),
        )
    }
}

/** The swiping hero, with the features card riding up over its faded-out bottom edge. */
@Composable
private fun PremiumTop(onClose: () -> Unit, onRestore: () -> Unit) {
    val pagerState = rememberPagerState(
        initialPage = HeroLoopPageCount / 2 - (HeroLoopPageCount / 2) % HeroImages.size,
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

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val heroHeight = maxWidth * HeroAspect
        HeroPager(pagerState, Modifier.fillMaxWidth().height(heroHeight))
        HeroIndicator(
            pagerState = pagerState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = heroHeight - CardOverlap - 18.dp),
        )
        // Top-left, below the status bar, where the design places it (≈ 8.7% across, 21.6% down the hero).
        CloseButton(
            onClose = onClose,
            modifier = Modifier.offset(x = maxWidth * 0.087f - 20.dp, y = heroHeight * 0.216f - 20.dp),
        )
        RestoreChip(
            onClick = onRestore,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-16).dp, y = heroHeight * 0.216f - 16.dp),
        )
        FeaturesCard(
            modifier = Modifier
                .padding(top = heroHeight - CardOverlap)
                .padding(horizontal = 20.dp),
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
private fun CloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
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
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Black, modifier = Modifier.size(24.dp))
        }
    }
}

/** Restore Purchase: both stores require a way to restore from the paywall itself. */
@Composable
private fun RestoreChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = "Restore",
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
private fun FeaturesCard(modifier: Modifier = Modifier) {
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
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            PremiumFeatures.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PremiumRed, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = feature,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
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

@Composable
private fun PlanPill(
    plan: PremiumPlan,
    product: SubscriptionProduct?,
    loading: Boolean,
    badge: String?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val trial = product?.freeTrial?.trialLabel
    val unit = product?.billingPeriod?.unitLabel ?: plan.fallbackUnit
    val subtitle = when {
        product == null -> if (loading) "Loading price…" else "Price unavailable"
        trial != null -> "$trial, then every $unit"
        else -> "Renews every $unit"
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
private fun PremiumFooter(
    startLabel: String,
    startEnabled: Boolean,
    busy: Boolean,
    onStart: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
) {
    val linkColor = PremiumText
    // Both stores require the auto-renewal terms beside the purchase button.
    val terms = remember(linkColor, onTermsAndPrivacy) {
        buildAnnotatedString {
            append("By continuing you agree to our ")
            withLink(
                LinkAnnotation.Clickable(
                    tag = "terms",
                    styles = TextLinkStyles(SpanStyle(color = linkColor, fontWeight = FontWeight.SemiBold)),
                ) { onTermsAndPrivacy() },
            ) {
                append("Terms & Privacy policies")
            }
            append(". Subscription will auto-renew. Cancel anytime.")
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
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
        Text("Auto Renewable. Cancel Anytime", color = PremiumSubtext, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
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
