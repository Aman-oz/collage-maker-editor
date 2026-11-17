package org.example.project.data.billing

import kotlinx.serialization.Serializable

/**
 * Store product IDs of the premium subscriptions. They must match, character for character, the
 * subscription product IDs created in Google Play Console (Monetize → Subscriptions) and App Store
 * Connect (Subscriptions) — a mismatch shows up as a plan whose price never loads.
 */
object BillingProductIds {
    /** Main paywall (startup flow, in-app, and the Shop's Premium tab): the two plans it lists. */
    const val Weekly = "item_1week"
    const val Monthly = "item_1m"
    const val Yearly = "yearly_subscription"

    /**
     * The launch offer screen's monthly plan: a cheaper monthly subscription (or one with an
     * introductory price) shown against [Monthly]'s price struck through.
     */
    const val MonthlyOffer = "item_1m_offer"

    /** The single-plan paywall opened from the premium banner on a premium item. */
    const val WeeklyPro = "item_1week_pro"

    // One weekly subscription per premium pack. On iOS each must sit in its own subscription group,
    // apart from the full plans' group, or StoreKit would treat buying one as switching plans.
    const val StickersPack = "stickers_pack_weekly"
    const val FontsPack = "fonts_pack_weekly"
    const val BackgroundsPack = "backgrounds_pack_weekly"
    const val FramesPack = "frames_pack_weekly"
    const val TemplatesPack = "templates_pack_weekly"
}

/**
 * A category that can be unlocked on its own, without the full subscription: what the pack sheet
 * offers a non-subscriber who looked at the paywall for one of its premium items and left it.
 * Rides in `Destination.Premium`, so it is [Serializable].
 */
@Serializable
enum class PremiumPack { Stickers, Fonts, Backgrounds, Frames, Templates }

/**
 * The subscriptions on sale, each backed by one store product: the full premium plans, which unlock
 * everything, and one plan per [PremiumPack], which unlocks only that category ([pack]).
 */
enum class SubscriptionPlan(
    val productId: String,
    val pack: PremiumPack? = null,
    /** For a plan sold only on one offer screen: the main paywall plan it is a variant of. */
    val variantOf: SubscriptionPlan? = null,
) {
    Weekly(BillingProductIds.Weekly),
    WeeklyPro(BillingProductIds.WeeklyPro, variantOf = Weekly),
    Monthly(BillingProductIds.Monthly),
    MonthlyOffer(BillingProductIds.MonthlyOffer, variantOf = Monthly),
    Yearly(BillingProductIds.Yearly),
    StickersPack(BillingProductIds.StickersPack, PremiumPack.Stickers),
    FontsPack(BillingProductIds.FontsPack, PremiumPack.Fonts),
    BackgroundsPack(BillingProductIds.BackgroundsPack, PremiumPack.Backgrounds),
    FramesPack(BillingProductIds.FramesPack, PremiumPack.Frames),
    TemplatesPack(BillingProductIds.TemplatesPack, PremiumPack.Templates),
    ;

    /** Whether this is a full premium plan rather than one category's. */
    val isFull: Boolean get() = pack == null

    /** The main paywall plan this one stands for: itself, or the plan it is a variant of. */
    val basePlan: SubscriptionPlan get() = variantOf ?: this

    companion object {
        /** The full premium plans, shortest first. */
        val FullPlans: List<SubscriptionPlan> = entries.filter { it.isFull }

        fun fromProductId(productId: String): SubscriptionPlan? = entries.firstOrNull { it.productId == productId }

        fun of(pack: PremiumPack): SubscriptionPlan = entries.first { it.pack == pack }
    }
}
