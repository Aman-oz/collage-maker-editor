package org.example.project.data.billing

import kotlin.coroutines.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/** [BillingClientAdapter] over the Swift [StoreKitBridge]. */
internal class StoreKitBillingAdapter(private val bridge: StoreKitBridge) : BillingClientAdapter {

    private val updates = MutableSharedFlow<Set<SubscriptionPlan>>(extraBufferCapacity = 1)
    override val entitlementUpdates: Flow<Set<SubscriptionPlan>> = updates.asSharedFlow()

    init {
        bridge.observeEntitlements { productIds -> updates.tryEmit(productIds.toPlans()) }
    }

    /** StoreKit 2 has no connection step; availability shows up as a failed call instead. */
    override suspend fun connect(): Boolean = true

    override suspend fun queryProducts(plans: List<SubscriptionPlan>): List<SubscriptionProduct> =
        suspendCancellableCoroutine { continuation ->
            bridge.fetchProducts(plans.map { it.productId }) { products, error ->
                if (products == null) {
                    continuation.resumeWith(Result.failure(BillingException(error ?: "Couldn't load subscription plans")))
                } else {
                    continuation.resume(products.mapNotNull { it.toSubscriptionProduct() })
                }
            }
        }

    override suspend fun queryActivePlans(): Set<SubscriptionPlan> = suspendCancellableCoroutine { continuation ->
        bridge.activeSubscriptions { productIds -> continuation.resume(productIds.toPlans()) }
    }

    // StoreKit replaces a plan in the same subscription group on its own, so currentPlan isn't needed.
    override suspend fun purchase(plan: SubscriptionPlan, currentPlan: SubscriptionPlan?): PurchaseResult =
        suspendCancellableCoroutine { continuation ->
            bridge.purchase(plan.productId) { outcome, error ->
                continuation.resume(
                    when (outcome) {
                        StoreKitPurchaseOutcome.Purchased -> PurchaseResult.Purchased(plan)
                        StoreKitPurchaseOutcome.Pending -> PurchaseResult.Pending
                        StoreKitPurchaseOutcome.Cancelled -> PurchaseResult.Cancelled
                        StoreKitPurchaseOutcome.Failed -> PurchaseResult.Failed(error ?: "The purchase didn't complete")
                    },
                )
            }
        }

    override suspend fun restore(): Set<SubscriptionPlan> {
        val error = suspendCancellableCoroutine { continuation -> bridge.sync { continuation.resume(it) } }
        if (error != null) throw BillingException(error)
        return queryActivePlans()
    }

    private fun List<String>.toPlans(): Set<SubscriptionPlan> = mapNotNull(SubscriptionPlan::fromProductId).toSet()

    private fun StoreKitProduct.toSubscriptionProduct(): SubscriptionProduct? {
        val plan = SubscriptionPlan.fromProductId(productId) ?: return null
        return SubscriptionProduct(
            plan = plan,
            formattedPrice = displayPrice,
            priceMicros = priceMicros,
            currencyCode = currencyCode,
            billingPeriod = BillingPeriod.parse(billingPeriod) ?: return null,
            freeTrial = freeTrialPeriod?.let(BillingPeriod::parse),
        )
    }
}
