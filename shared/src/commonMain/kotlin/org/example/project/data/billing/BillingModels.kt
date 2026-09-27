package org.example.project.data.billing

/**
 * One plan as the store sells it to this user, with store-localized pricing.
 *
 * @param formattedPrice the recurring price, formatted by the store in the user's currency.
 * @param priceMicros [formattedPrice] in millionths of [currencyCode] (Play's native unit).
 * @param billingPeriod how often [formattedPrice] is charged.
 * @param freeTrial the free trial this user is still eligible for, or null — the stores only offer
 *   an introductory trial once per subscription group, so it is resolved per user, never assumed.
 */
data class SubscriptionProduct(
    val plan: SubscriptionPlan,
    val formattedPrice: String,
    val priceMicros: Long,
    val currencyCode: String,
    val billingPeriod: BillingPeriod,
    val freeTrial: BillingPeriod? = null,
)

/** Outcome of a purchase attempt. */
sealed interface PurchaseResult {
    data class Purchased(val plan: SubscriptionPlan) : PurchaseResult

    /** Paid with a delayed method (e.g. cash); premium unlocks once the store confirms it. */
    data object Pending : PurchaseResult

    data object Cancelled : PurchaseResult

    /** The store says the user already has this plan; entitlements are refreshed instead. */
    data object AlreadyOwned : PurchaseResult

    data class Failed(val message: String) : PurchaseResult
}

/** Outcome of "Restore Purchase". */
sealed interface RestoreResult {
    data class Restored(val plans: Set<SubscriptionPlan>) : RestoreResult
    data object NothingToRestore : RestoreResult
    data class Failed(val message: String) : RestoreResult
}

/** Whether the store can currently be reached. */
enum class BillingStatus { Connecting, Ready, Unavailable }

/** What [AppBillingWrapper] currently knows about the store and the user's subscriptions. */
data class BillingState(
    val status: BillingStatus = BillingStatus.Connecting,
    val products: Map<SubscriptionPlan, SubscriptionProduct> = emptyMap(),
    val activePlans: Set<SubscriptionPlan> = emptySet(),
) {
    /** The plan the user is on; if the store reports several, the longest one. */
    val activePlan: SubscriptionPlan? get() = SubscriptionPlan.entries.lastOrNull { it in activePlans }
}

/** A store call that failed (no connection, store error); [message] is safe to show the user. */
class BillingException(message: String) : Exception(message)
