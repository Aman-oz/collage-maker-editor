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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fletchmckee.liquid.LiquidState
import kotlin.time.Clock
import kotlinx.coroutines.delay
import org.example.project.data.billing.BillingPeriod
import org.example.project.data.billing.PeriodUnit
import org.example.project.data.billing.SubscriptionPlan
import org.example.project.data.billing.SubscriptionProduct
import org.example.project.i18n.tr
import org.example.project.ui.common.GlassDialogHost
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.img_offer_box_1
import photocollagemaker.shared.generated.resources.img_offer_box_2
import photocollagemaker.shared.generated.resources.img_offer_box_3

/** "LIMITED TIME OFFER!" fill: warm orange, as in the design. */
private val OfferTitleGradient = Brush.horizontalGradient(listOf(Color(0xFFFFA23D), Color(0xFFFF6A2B)))

private val OfferFeatureColumns = listOf(
    listOf("Unlimited Collages", "Premium Templates", "HD Exports"),
    listOf("No Ads", "Exclusive Fonts", "All Filters & Stickers"),
)

private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * The launch-flow limited-time offer over the paywall: the Annual plan, with its countdown to
 * [endsAt]. Shown while [endsAt] is set; when the clock runs out it closes like its close button.
 */
@Composable
internal fun LaunchOfferHost(
    endsAt: Long?,
    liquidState: LiquidState,
    yearly: SubscriptionProduct?,
    weekly: SubscriptionProduct?,
    busy: Boolean,
    onTryNow: () -> Unit,
    onClose: () -> Unit,
) {
    val currentOnClose by rememberUpdatedState(onClose)
    var now by remember { mutableLongStateOf(nowMillis()) }
    LaunchedEffect(endsAt) {
        if (endsAt == null) return@LaunchedEffect
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

    GlassDialogHost(
        visible = endsAt != null,
        liquidState = liquidState,
        onDismiss = onClose,
        // Only the close button below the panel closes it, as in the design.
        dismissOnScrimClick = false,
        footer = { OfferCloseButton(onClose) },
    ) {
        if (endsAt != null) {
            LaunchOfferContent(
                yearly = yearly,
                weekly = weekly,
                remainingMillis = endsAt - now,
                busy = busy,
                onTryNow = onTryNow,
            )
        }
    }
}

@Composable
private fun LaunchOfferContent(
    yearly: SubscriptionProduct?,
    weekly: SubscriptionProduct?,
    remainingMillis: Long,
    busy: Boolean,
    onTryNow: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    // The discount is the Annual plan against a year of Weekly, from the store's own prices; the
    // struck-through price is that year of Weekly, so the two always agree.
    val savings = annualSavingsPercent(weekly, yearly)
    val regularPrice = if (savings != null && weekly != null) {
        formatPriceLike(weekly.formattedPrice, weekly.priceMicros * 52)
    } else {
        null
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tr("LIMITED TIME OFFER!"),
            style = TextStyle(brush = OfferTitleGradient),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(16.dp))
        OfferFeatures()
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FloatingGifts()
            Column(
                modifier = Modifier.padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (savings != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$savings%", color = PremiumRed, fontSize = 58.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tr("OFF"),
                            modifier = Modifier.padding(bottom = 10.dp),
                            color = PremiumRed.copy(alpha = 0.55f),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Text(tr("BEST VALUE"), color = PremiumRed, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    // No price until the store answers (offline, or products not set up yet).
                    Text(
                        text = yearly?.formattedPrice ?: tr("Annual Plan"),
                        color = onSurface,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (yearly != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tr("per year"),
                            modifier = Modifier.padding(bottom = 4.dp),
                            color = onSurface.copy(alpha = 0.75f),
                            fontSize = 14.sp,
                        )
                    }
                }
                if (yearly == null) {
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
        Spacer(Modifier.height(4.dp))
        CountdownRow(countdown(remainingMillis))
        Spacer(Modifier.height(24.dp))
        ContinueButton(
            label = tr("Try Now"),
            enabled = !busy,
            busy = busy,
            onClick = onTryNow,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
        )
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
                .background(onSurface.copy(alpha = 0.04f))
                .border(1.dp, onSurface.copy(alpha = 0.22f), shape)
                .padding(start = 14.dp, end = 10.dp, top = 20.dp, bottom = 14.dp),
        ) {
            OfferFeatureColumns.forEach { column ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    column.forEach { feature ->
                        Text("•  ${tr(feature)}", color = onSurface.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1)
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
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFB020), modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text("PRO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** The three gift boxes around the discount, bobbing gently out of step with each other. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.FloatingGifts() {
    val transition = rememberInfiniteTransition()
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    )
    Gift(
        image = Res.drawable.img_offer_box_3,
        size = 30.dp,
        rotation = -14f,
        bobPx = { bob * -5f },
        modifier = Modifier.align(Alignment.TopStart).offset(x = 22.dp, y = 2.dp),
    )
    Gift(
        image = Res.drawable.img_offer_box_2,
        size = 40.dp,
        rotation = -8f,
        bobPx = { bob * 7f },
        modifier = Modifier.align(Alignment.BottomStart).offset(x = 2.dp, y = (-12).dp),
    )
    Gift(
        image = Res.drawable.img_offer_box_1,
        size = 72.dp,
        rotation = 8f,
        bobPx = { bob * -8f },
        modifier = Modifier.align(Alignment.CenterEnd).offset(x = 8.dp, y = 10.dp),
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
            // Read in the draw phase, so the bobbing never recomposes the dialog.
            .graphicsLayer { translationY = bobPx() * density },
    )
}

@Composable
private fun CountdownRow(time: Countdown) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(time.hours, time.minutes, time.seconds).forEachIndexed { index, value ->
            if (index > 0) {
                Text(
                    text = ":",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(6.dp, RoundedCornerShape(10.dp), spotColor = PremiumRed)
                    .background(PremiumRed, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value.toString().padStart(2, '0'),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun OfferCloseButton(onClose: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    // Fixed colours: it always sits on the dark scrim, whatever the theme.
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.25f))
            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            .clickable(role = Role.Button, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Close, contentDescription = tr("Close offer"), tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Preview
@Composable
private fun LaunchOfferPreview() {
    val weekly = SubscriptionProduct(SubscriptionPlan.Weekly, "PKR 900.00", 900_000_000, "PKR", BillingPeriod(1, PeriodUnit.Week))
    val yearly = SubscriptionProduct(SubscriptionPlan.Yearly, "PKR 9,360.00", 9_360_000_000, "PKR", BillingPeriod(1, PeriodUnit.Year))
    ThemePreviews {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            LaunchOfferContent(
                yearly = yearly,
                weekly = weekly,
                remainingMillis = (56 * 60 + 28) * 1_000L,
                busy = false,
                onTryNow = {},
            )
        }
    }
}
