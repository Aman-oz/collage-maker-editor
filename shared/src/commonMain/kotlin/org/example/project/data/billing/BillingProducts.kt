package org.example.project.data.billing

/**
 * Store product IDs of the premium subscriptions. They must match, character for character, the
 * subscription product IDs created in Google Play Console (Monetize → Subscriptions) and App Store
 * Connect (Subscriptions) — a mismatch shows up as a plan whose price never loads.
 */
object BillingProductIds {
    const val Weekly = "weekly_subscription"
    const val Monthly = "monthly_subscription"
    const val Yearly = "yearly_subscription"
}

/** The premium subscription plans on sale, each backed by one store product. */
enum class SubscriptionPlan(val productId: String) {
    Weekly(BillingProductIds.Weekly),
    Monthly(BillingProductIds.Monthly),
    Yearly(BillingProductIds.Yearly),
    ;

    companion object {
        fun fromProductId(productId: String): SubscriptionPlan? = entries.firstOrNull { it.productId == productId }
    }
}
