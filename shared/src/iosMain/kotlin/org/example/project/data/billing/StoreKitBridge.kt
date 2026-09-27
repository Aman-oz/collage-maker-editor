package org.example.project.data.billing

/**
 * The StoreKit 2 calls the iOS [BillingClientAdapter] needs. StoreKit 2 is a Swift-only API, so
 * this is implemented in Swift (`iosApp/iosApp/StoreKitBridge.swift`) and handed to
 * `MainViewController`. It is callback-based because Kotlin `suspend` functions can't be
 * implemented from Swift; [StoreKitBillingAdapter] turns the callbacks back into coroutines.
 * Callbacks may arrive on any thread.
 */
interface StoreKitBridge {

    /** Loads [productIds]; calls back with the products, or null and an error message. */
    fun fetchProducts(productIds: List<String>, completion: (List<StoreKitProduct>?, String?) -> Unit)

    /** Runs the App Store purchase sheet; the String is an error message for [StoreKitPurchaseOutcome.Failed]. */
    fun purchase(productId: String, completion: (StoreKitPurchaseOutcome, String?) -> Unit)

    /** Product IDs of the subscriptions the user currently holds (`Transaction.currentEntitlements`). */
    fun activeSubscriptions(completion: (List<String>) -> Unit)

    /** `AppStore.sync()`: re-syncs with the user's App Store account; calls back with an error or null. */
    fun sync(completion: (String?) -> Unit)

    /** Starts listening to `Transaction.updates`, calling [onChange] with the active product IDs. */
    fun observeEntitlements(onChange: (List<String>) -> Unit)
}

/**
 * A StoreKit product, flattened for Kotlin.
 *
 * @param billingPeriod the subscription period as an ISO 8601 duration ("P1W"), like Play reports.
 * @param freeTrialPeriod the introductory free trial, only when this user is eligible for it.
 */
data class StoreKitProduct(
    val productId: String,
    val displayPrice: String,
    val priceMicros: Long,
    val currencyCode: String,
    val billingPeriod: String,
    val freeTrialPeriod: String?,
)

enum class StoreKitPurchaseOutcome { Purchased, Pending, Cancelled, Failed }
