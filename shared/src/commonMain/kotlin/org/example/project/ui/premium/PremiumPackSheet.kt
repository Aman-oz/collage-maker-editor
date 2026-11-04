package org.example.project.ui.premium

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.AppSettings
import org.example.project.data.billing.AppBillingWrapper
import org.example.project.data.billing.BillingPeriod
import org.example.project.data.billing.BillingStatus
import org.example.project.data.billing.PeriodUnit
import org.example.project.data.billing.PremiumPack
import org.example.project.data.billing.PurchaseResult
import org.example.project.data.billing.SubscriptionPlan
import org.example.project.data.billing.SubscriptionProduct
import org.example.project.i18n.tr
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.settings.SettingsLinks
import org.example.project.ui.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.img_premium_background_dialog
import photocollagemaker.shared.generated.resources.img_premium_frames_dialog
import photocollagemaker.shared.generated.resources.img_premium_sticker_dialog
import photocollagemaker.shared.generated.resources.img_premium_templates_dialog

/**
 * The pack sheet's queue of one: which [PremiumPack] to offer, over whatever screen is showing. A
 * Koin singleton, because the offer is made as a screen is *left* (the Shop's Premium tab, or the
 * paywall opened for a template or frame) and has to show on whatever is underneath, which is
 * another nav entry. [PremiumPackSheetHost], around the whole `NavDisplay`, shows it.
 */
class PremiumPackOffers(private val settings: AppSettings) {
    private val _shown = MutableStateFlow<PremiumPack?>(null)
    val shown: StateFlow<PremiumPack?> = _shown.asStateFlow()

    /** Offers [pack], unless it is already the user's (subscribed, or the pack bought). */
    fun show(pack: PremiumPack) {
        if (!settings.hasUnlocked(pack)) _shown.value = pack
    }

    fun dismiss() {
        _shown.value = null
    }
}

/**
 * Makes the pack offer when the paywall it is scoped to is popped without a subscription. Scoped
 * to a `Destination.Premium` entry opened for a pack's item, so system back and ✕ are alike:
 * either clears the entry, and with it this.
 */
class PremiumPackOfferOnLeave(private val pack: PremiumPack, private val offers: PremiumPackOffers) : ViewModel() {
    override fun onCleared() {
        offers.show(pack)
    }
}

/**
 * Whether [pack]'s premium items are the user's: [subscribed] (the tool's own premium flag), or that
 * pack bought on its own. Read by the tools whose items a pack unlocks.
 */
@Composable
internal fun unlockedFor(pack: PremiumPack, subscribed: Boolean): Boolean {
    val packs by koinInject<AppSettings>().unlockedPacks.collectAsStateWithLifecycle()
    return subscribed || pack in packs
}

/** What the sheet shows for each pack: its header picture and the two lines beside the price. */
private val PremiumPack.header: DrawableResource
    get() = when (this) {
        PremiumPack.Backgrounds -> Res.drawable.img_premium_background_dialog
        PremiumPack.Templates -> Res.drawable.img_premium_templates_dialog
        PremiumPack.Frames -> Res.drawable.img_premium_frames_dialog
        // No picture of fonts was drawn; the stickers' collage reads as "creative assets" well enough.
        PremiumPack.Stickers, PremiumPack.Fonts -> Res.drawable.img_premium_sticker_dialog
    }

private val PremiumPack.features: List<String>
    get() = when (this) {
        PremiumPack.Stickers -> listOf(tr("1000+ Stickers"), tr("5000+ Stickers Pack"))
        PremiumPack.Fonts -> listOf(tr("1000+ Fonts"), tr("Exclusive Designs"))
        PremiumPack.Backgrounds -> listOf(tr("1000+ Background"), tr("Exclusive Designs"))
        PremiumPack.Templates -> listOf(tr("1000+ Templates"), tr("5000+ Frames"))
        PremiumPack.Frames -> listOf(tr("5000+ Frames"), tr("1000+ Templates"))
    }

/**
 * Where the header pictures draw their ✕ (they come with it), as fractions of the picture: the
 * sheet puts its close target there rather than drawing a second one.
 */
private const val HeaderCloseCenterX = 103f / 1206f
private const val HeaderCloseCenterY = 123f / 585f
private const val HeaderCloseSize = 120f / 1206f
private const val HeaderAspectRatio = 1206f / 585f

/**
 * Shows [PremiumPackOffers.shown] as a bottom sheet over the app: the pack's header picture, "Unlock
 * Premium Pack", its weekly plan with the store's price and trial, and the purchase button. A
 * purchase unlocks only that category ([SubscriptionPlan.of]); buying the full plan elsewhere, or
 * this one, closes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPackSheetHost() {
    val offers = koinInject<PremiumPackOffers>()
    val billing = koinInject<AppBillingWrapper>()
    val pack by offers.shown.collectAsStateWithLifecycle()
    val shownPack = pack ?: return
    val billingState by billing.state.collectAsStateWithLifecycle()
    val isPremium by billing.isPremium.collectAsStateWithLifecycle()
    LaunchedEffect(isPremium) { if (isPremium) offers.dismiss() }
    LaunchedEffect(Unit) { if (billing.state.value.status != BillingStatus.Ready) billing.refresh() }

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var busy by remember(shownPack) { mutableStateOf(false) }
    var message by remember(shownPack) { mutableStateOf<String?>(null) }
    // Slides down before it leaves, rather than vanishing.
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { offers.dismiss() } }
    val plan = SubscriptionPlan.of(shownPack)

    // The pictures are drawn on white (the wave under each), so the sheet is light in both themes.
    AppTheme(darkTheme = false) {
        ModalBottomSheet(
            onDismissRequest = { offers.dismiss() },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            PremiumPackContent(
                pack = shownPack,
                product = billingState.products[plan],
                busy = busy,
                message = message,
                onClose = close,
                onStart = {
                    busy = true
                    message = null
                    scope.launch {
                        when (val result = billing.purchase(plan)) {
                            is PurchaseResult.Purchased -> close()
                            PurchaseResult.Pending -> message = tr("Your payment is pending. Premium unlocks as soon as it's confirmed.")
                            PurchaseResult.AlreadyOwned -> close()
                            is PurchaseResult.Failed -> message = result.message
                            // Backing out of the store sheet is a choice, not an error; say nothing.
                            PurchaseResult.Cancelled -> Unit
                        }
                        busy = false
                    }
                },
                onTermsAndPrivacy = {
                    val url = SettingsLinks.PrivacyPolicyUrl.ifBlank { SettingsLinks.TermsOfUseUrl }
                    if (url.isBlank()) message = tr("Terms & Privacy is coming soon") else uriHandler.openUri(url)
                },
            )
        }
    }
}

@Composable
private fun PremiumPackContent(
    pack: PremiumPack,
    product: SubscriptionProduct?,
    busy: Boolean,
    message: String?,
    onClose: () -> Unit,
    onStart: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PackHeader(pack = pack, onClose = onClose)
        Spacer(Modifier.height(4.dp))
        PackTitle()
        Spacer(Modifier.height(14.dp))
        PackPlanCard(pack = pack, product = product, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(14.dp))
        if (message != null) {
            Text(
                text = message,
                color = PremiumRed,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
        PremiumFooter(
            startLabel = when {
                product == null -> tr("Continue")
                // "Try for PKR 0.00": nothing is charged until the trial ends, in the store's own currency.
                product.freeTrial != null -> formatPriceLike(product.formattedPrice, 0)?.let { tr("Try for {0}", it) }
                    ?: tr("Start Free Trial")
                else -> tr("Continue")
            },
            startEnabled = product != null,
            busy = busy,
            onStart = onStart,
            onTermsAndPrivacy = onTermsAndPrivacy,
        )
    }
}

/** The pack's picture, with a tap target over the ✕ it already draws. */
@Composable
private fun PackHeader(pack: PremiumPack, onClose: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(HeaderAspectRatio)) {
        Image(
            painter = painterResource(pack.header),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        val target = maxWidth * HeaderCloseSize
        Box(
            modifier = Modifier
                .offset(x = maxWidth * HeaderCloseCenterX - target / 2, y = maxHeight * HeaderCloseCenterY - target / 2)
                .size(target)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = tr("Close"), onClick = onClose),
        )
    }
}

/** "Unlock Premium Pack", the middle word in the brand red. */
@Composable
private fun PackTitle() {
    val onSurface = MaterialTheme.colorScheme.onSurface
    // The highlighted word is placed by the translation's own {0}, so a language can move it.
    val template = tr("Unlock {0} Pack")
    val highlighted = tr("Premium")
    val title = buildAnnotatedString {
        val at = template.indexOf("{0}")
        if (at < 0) {
            append(template)
        } else {
            append(template.substring(0, at))
            withStyle(SpanStyle(color = PremiumRed)) { append(highlighted) }
            append(template.substring(at + 3))
        }
    }
    Text(text = title, color = onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

/** The weekly plan, "Best Value" on its corner: price and trial on the left, the pack's lines on the right. */
@Composable
private fun PackPlanCard(pack: PremiumPack, product: SubscriptionProduct?, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(14.dp)
    Box(modifier = modifier.fillMaxWidth().padding(top = BestValueHeight / 2)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(shape)
                .background(PremiumRed.copy(alpha = 0.04f))
                .border(1.5.dp, PremiumRed, shape)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product?.billingPeriod?.planName() ?: tr("Weekly Plan"),
                    color = onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                if (product == null) {
                    Text(tr("Loading price…"), color = onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                } else {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) { append(product.formattedPrice) }
                            withStyle(SpanStyle(fontSize = 14.sp)) { append("/" + product.billingPeriod.localizedUnitLabel()) }
                        },
                        color = onSurface,
                    )
                }
                product?.freeTrial?.let { trial ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "🎁 " + trial.localizedTrialLabel(),
                        color = PremiumRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(PremiumRed.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(onSurface.copy(alpha = 0.12f)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pack.features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PremiumRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(feature, color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        // A full pill, centred on the card's top border.
        Text(
            text = tr("Best Value"),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-14).dp, y = (-BestValueHeight / 2))
                .height(BestValueHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(SelectedPlanGradient)
                .padding(horizontal = 14.dp)
                .wrapContentHeight(Alignment.CenterVertically),
        )
    }
}

private val BestValueHeight = 26.dp

/** "Weekly Plan", "Monthly Plan", …, from what the store says the pack renews by. */
private fun BillingPeriod.planName(): String = when (unit) {
    PeriodUnit.Day -> tr("Daily Plan")
    PeriodUnit.Week -> tr("Weekly Plan")
    PeriodUnit.Month -> tr("Monthly Plan")
    PeriodUnit.Year -> tr("Yearly Plan")
}

@Preview
@Composable
private fun PremiumPackSheetPreview() {
    val weekly = SubscriptionProduct(
        SubscriptionPlan.StickersPack, "PKR 900", 900_000_000, "PKR", BillingPeriod(1, PeriodUnit.Week), freeTrial = BillingPeriod(3, PeriodUnit.Day),
    )
    ThemePreviews {
        AppTheme(darkTheme = false) {
            Box(Modifier.background(Color.White)) {
                PremiumPackContent(PremiumPack.Stickers, weekly, busy = false, message = null, onClose = {}, onStart = {}, onTermsAndPrivacy = {})
            }
        }
    }
}
