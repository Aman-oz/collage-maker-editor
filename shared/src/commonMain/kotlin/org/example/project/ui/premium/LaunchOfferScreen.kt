package org.example.project.ui.premium

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import photocollagemaker.shared.generated.resources.ic_premium_icon
import photocollagemaker.shared.generated.resources.img_offer_bg
import org.example.project.ui.theme.AppTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.data.billing.BillingPeriod
import org.example.project.data.billing.PeriodUnit
import org.example.project.data.billing.IntroPrice
import org.example.project.data.billing.SubscriptionPlan
import org.example.project.data.billing.SubscriptionProduct
import org.example.project.i18n.tr
import org.example.project.ui.common.isSideBySideEditor
import org.example.project.ui.common.readableWidth
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.img_offer_box_1
import photocollagemaker.shared.generated.resources.img_offer_box_2
import photocollagemaker.shared.generated.resources.img_offer_box_3

/** "Congratulations!" fill: the offer's red, as in the design. */
private val OfferTitleGradient = Brush.horizontalGradient(listOf(Color(0xFFF2454B), Color(0xFFD3222B)))

/** The screen's pink wash, laid over the theme background so it reads in dark mode too. */
private val OfferWashTop = Color(0xFFFF8A80)
private val OfferWashBottom = Color(0xFFFF5C6C)

private val OfferFeatureColumns = listOf(
    listOf("Unlimited Collages", "No Watermark", "HD Exports"),
    listOf("Premium Templates", "No Ads", "All Filters & Stickers"),
)

/**
 * The plan this screen sells: its own monthly product, shown against the main paywall's Monthly
 * price struck through. Until it is set up in the store, the main Monthly is sold instead.
 */
internal val LaunchOfferPlan = SubscriptionPlan.MonthlyOffer

private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * The startup flow's `offer` step: the limited-time Monthly deal, full screen, with its countdown.
 *
 * The clock starts the first time the offer is shown and runs for two hours across launches
 * ([launchOfferEndsAt]). Once it has run out (or for a subscriber) there is no offer to make, so
 * the screen calls [onClose] straight away and the flow moves on; it does the same when the clock
 * runs out while it is open, or when the purchase goes through.
 */
@Composable
fun LaunchOfferScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PremiumViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Claimed once per visit (0 when there is nothing to offer); saved so rotating doesn't claim again.
    val endsAt = rememberSaveable { viewModel.claimLaunchOffer() ?: 0L }
    val currentOnClose by rememberUpdatedState(onClose)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PremiumEvent.Message -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.text)
                }
                PremiumEvent.Unlocked -> currentOnClose()
            }
        }
    }

    // Back does what the close button does. In the startup flow nothing is under this screen, so
    // without it Back would close the app.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = onClose,
    )

    // Only the countdown reads the clock (through a lambda), so the rest of the screen doesn't
    // recompose on each tick.
    var now by remember { mutableLongStateOf(nowMillis()) }
    LaunchedEffect(endsAt) {
        while (true) {
            now = nowMillis()
            if (now >= endsAt) {
                currentOnClose()
                break
            }
            // Wakes on the next whole second, so the seconds box never skips or lingers.
            delay(1_000 - (endsAt - now) % 1_000)
        }
    }
    val expired by remember { derivedStateOf { now >= endsAt } }

    // The design is light whatever the app theme, so the screen always uses the light colours
    // (dark text on the light background image).
    val appDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    AppTheme(darkTheme = false) {
        Box(modifier = modifier.fillMaxSize()) {
            OfferBackdrop(statusBarScrim = appDark)
            if (!expired) {
                // The offer's own monthly product; the main Monthly until it is set up in the store.
                val offer = uiState.billing.productFor(LaunchOfferPlan)
                LaunchOfferContent(
                    regular = uiState.billing.products[SubscriptionPlan.Monthly],
                    offer = offer,
                    remainingMillis = { endsAt - now },
                    busy = uiState.busy,
                    onTryNow = { viewModel.purchase(offer?.plan ?: LaunchOfferPlan) },
                    onClose = onClose,
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp),
            )
        }
    }
}

/**
 * The offer's background image, cropped to fill any screen shape, over the pink gradient (which
 * shows until the image has loaded). The image is a 720 px JPEG, small enough to decode on any
 * device. [statusBarScrim]: the app is in dark mode, so the status bar icons are white; a soft
 * dark fade under them keeps them readable on the light image.
 */
@Composable
private fun OfferBackdrop(statusBarScrim: Boolean) {
    Box(modifier = Modifier.fillMaxSize().offerBackground()) {
        Image(
            painter = painterResource(Res.drawable.img_offer_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (statusBarScrim) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))),
            )
        }
    }
}

@Composable
private fun Modifier.offerBackground(): Modifier {
    val base = MaterialTheme.colorScheme.background
    return background(
        Brush.verticalGradient(
            listOf(
                OfferWashTop.copy(alpha = 0.22f).compositeOver(base),
                OfferWashBottom.copy(alpha = 0.12f).compositeOver(base),
                OfferWashBottom.copy(alpha = 0.26f).compositeOver(base),
            ),
        ),
    )
}

@Composable
private fun LaunchOfferContent(
    regular: SubscriptionProduct?,
    offer: SubscriptionProduct?,
    remainingMillis: () -> Long,
    busy: Boolean,
    onTryNow: () -> Unit,
    onClose: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    // The discount is the offer's first charge against the main paywall's Monthly price, both from
    // the store; that Monthly price is the one struck through, so the two always agree.
    val savings = offerDiscountPercent(regular, offer)
    val regularPrice = regular?.takeIf { savings != null }
        ?.let { "${it.formattedPrice}/${it.billingPeriod.localizedUnitLabel()}" }

    val heading: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = tr("Congratulations!"),
                // The design's soft red shadow under the title.
                style = TextStyle(
                    brush = OfferTitleGradient,
                    shadow = Shadow(color = Color(0xFFD3222B).copy(alpha = 0.35f), offset = Offset(0f, 6f), blurRadius = 10f),
                ),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = tr("You've been selected to claim this exclusive offer."),
                modifier = Modifier.padding(horizontal = 32.dp),
                color = onSurface.copy(alpha = 0.75f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
    val deal: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FloatingGifts()
                Discount(savings, modifier = Modifier.padding(vertical = 22.dp))
            }
            // No price until the store answers (offline, or products not set up yet).
            Text(
                text = offer?.let(::offerPriceLine) ?: tr("Monthly Plan"),
                color = onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (offer == null) {
                Text(tr("Price unavailable"), color = onSurface.copy(alpha = 0.55f), fontSize = 14.sp)
            }
            if (regularPrice != null) {
                Text(
                    text = regularPrice,
                    color = onSurface.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }
    }
    val action: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ContinueButton(label = tr("Try Now"), enabled = !busy, busy = busy, onClick = onTryNow)
            Spacer(Modifier.height(8.dp))
            Text(
                // The design's line for a free trial; the price after an intro price; else the plain renewal note.
                text = when {
                    offer?.freeTrial != null -> tr("Subscription is billed after the trial ends and will auto-renew.")
                    offer?.introPrice != null -> tr(
                        "Then {0}/{1}. Renews automatically until you cancel.",
                        offer.formattedPrice,
                        offer.billingPeriod.localizedUnitLabel(),
                    )
                    else -> tr("Billed monthly. Renews automatically until you cancel.")
                },
                color = onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        if (isSideBySideEditor(maxWidth, maxHeight)) {
            // A phone on its side: the stacked offer is taller than the screen, so the title,
            // features and clock go on the left, the deal and its button on the right.
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 64.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    heading()
                    Spacer(Modifier.height(12.dp))
                    OfferFeatures()
                    Spacer(Modifier.height(16.dp))
                    CountdownRow(remainingMillis)
                }
                Spacer(Modifier.width(24.dp))
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    deal()
                    Spacer(Modifier.height(16.dp))
                    action()
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .readableWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        // Just under the close button, as in the design; spare height goes above Try Now.
                        .padding(top = 44.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    heading()
                    Spacer(Modifier.height(20.dp))
                    OfferFeatures()
                    deal()
                    Spacer(Modifier.height(20.dp))
                    CountdownRow(remainingMillis)
                }
                Box(modifier = Modifier.readableWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp)) {
                    action()
                }
            }
        }
        // The paywall's own close button: it shows up after a moment, so the offer is seen first.
        CloseButton(onClose = onClose, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
    }
}

/** "95 % OFF": the number large, the percent sign and OFF stacked beside it. */
@Composable
private fun Discount(savings: Int?, modifier: Modifier = Modifier) {
    if (savings == null) {
        Text(tr("BEST VALUE"), modifier = modifier, color = PremiumRed, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        return
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$savings",
            color = PremiumRed,
            fontSize = 108.sp,
            lineHeight = 108.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text("%", color = PremiumRed, fontSize = 46.sp, lineHeight = 46.sp, fontWeight = FontWeight.ExtraBold)
            Text(tr("OFF"), color = PremiumRed, fontSize = 34.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** The feature list in an outlined box, with the PRO chip sitting on its top border. */
@Composable
private fun OfferFeatures() {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(16.dp)
    // Top padding leaves room for the half of the chip that sits above the box.
    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .border(1.dp, onSurface.copy(alpha = 0.45f), shape)
                .padding(start = 14.dp, end = 10.dp, top = 20.dp, bottom = 14.dp),
        ) {
            OfferFeatureColumns.forEach { column ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    column.forEach { feature ->
                        Text(
                            text = "•  ${tr(feature)}",
                            color = onSurface.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                // Centers the chip on the box's top border, whatever its measured height.
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, -placeable.height / 2)
                    }
                }
                .background(PlanBadgeColor, RoundedCornerShape(percent = 50))
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The app's own premium crown, as on premium items.
            Image(painterResource(Res.drawable.ic_premium_icon), contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("PRO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** The three gift boxes around the discount, bobbing gently out of step with each other. */
@Composable
private fun BoxScope.FloatingGifts() {
    val transition = rememberInfiniteTransition()
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    )
    Gift(
        image = Res.drawable.img_offer_box_3,
        size = 40.dp,
        rotation = -14f,
        bobPx = { bob * -5f },
        modifier = Modifier.align(Alignment.TopStart).offset(x = 4.dp, y = 0.dp),
    )
    Gift(
        image = Res.drawable.img_offer_box_2,
        size = 48.dp,
        rotation = -8f,
        bobPx = { bob * 7f },
        modifier = Modifier.align(Alignment.BottomStart).offset(x = (-4).dp, y = (-8).dp),
    )
    Gift(
        image = Res.drawable.img_offer_box_1,
        size = 92.dp,
        rotation = 8f,
        bobPx = { bob * -8f },
        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 16.dp),
    )
}

@Composable
private fun Gift(image: DrawableResource, size: Dp, rotation: Float, bobPx: () -> Float, modifier: Modifier) {
    Image(
        painter = painterResource(image),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .rotate(rotation)
            // Read in the draw phase, so the bobbing never recomposes the screen.
            .graphicsLayer { translationY = bobPx() * density },
    )
}

/** Hours : minutes : seconds in red boxes, each labelled underneath. Reads [remainingMillis] itself. */
@Composable
private fun CountdownRow(remainingMillis: () -> Long) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val time = countdown(remainingMillis())
    val parts = listOf(time.hours to tr("Hours"), time.minutes to tr("Minutes"), time.seconds to tr("Seconds"))
    Row(verticalAlignment = Alignment.Top) {
        parts.forEachIndexed { index, (value, label) ->
            if (index > 0) {
                Text(
                    text = ":",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                    color = onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = PremiumRed)
                        .background(PremiumRed, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = value.toString().padStart(2, '0'),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(label, color = onSurface.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview
@Composable
private fun LaunchOfferPreview() {
    val regular = SubscriptionProduct(SubscriptionPlan.Monthly, "Rs 820.00", 820_000_000, "PKR", BillingPeriod(1, PeriodUnit.Month))
    val offer = SubscriptionProduct(
        SubscriptionPlan.MonthlyOffer, "Rs 820.00", 820_000_000, "PKR", BillingPeriod(1, PeriodUnit.Month),
        introPrice = IntroPrice("Rs 41.00", 41_000_000, BillingPeriod(3, PeriodUnit.Day)),
    )
    ThemePreviews {
        Box(Modifier.fillMaxSize()) {
            OfferBackdrop(statusBarScrim = false)
            LaunchOfferContent(
                regular = regular,
                offer = offer,
                remainingMillis = { 56_280L },
                busy = false,
                onTryNow = {},
                onClose = {},
            )
        }
    }
}
