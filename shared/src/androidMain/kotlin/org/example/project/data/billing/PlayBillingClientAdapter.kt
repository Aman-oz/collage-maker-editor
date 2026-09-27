package org.example.project.data.billing

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * [BillingClientAdapter] on Google Play Billing.
 *
 * Play reports purchase results through one app-wide [PurchasesUpdatedListener] rather than per
 * call, so [purchase] parks a [CompletableDeferred] that the listener completes; updates arriving
 * with no purchase in flight (a pending payment clearing) go out through [entitlementUpdates].
 *
 * Every purchase is acknowledged here, which Play requires within three days or it refunds it.
 * There is no server-side receipt verification yet; add it before relying on this for anything
 * beyond unlocking features on the device.
 */
internal class PlayBillingClientAdapter(application: Application) : BillingClientAdapter {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val activityTracker = ResumedActivityTracker().also(application::registerActivityLifecycleCallbacks)

    private val updates = MutableSharedFlow<Set<SubscriptionPlan>>(extraBufferCapacity = 1)
    override val entitlementUpdates: Flow<Set<SubscriptionPlan>> = updates.asSharedFlow()

    /** Loaded details per plan, with the offer the plan is sold under; needed to launch a purchase. */
    private val offers = mutableMapOf<SubscriptionPlan, Pair<ProductDetails, String>>()

    /** Purchase tokens of the plans the user holds, needed to replace one when switching plans. */
    private val activeTokens = mutableMapOf<SubscriptionPlan, String>()

    private var pendingPurchase: CompletableDeferred<PurchaseResult>? = null

    private val client: BillingClient = BillingClient.newBuilder(application)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases.orEmpty()) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        // Play reconnects on its own after the service drops, so calls don't fail on a stale link.
        .enableAutoServiceReconnection()
        .build()

    override suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (continuation.isActive) continuation.resume(result.responseCode == BillingResponseCode.OK)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
            )
        }
    }

    override suspend fun queryProducts(plans: List<SubscriptionPlan>): List<SubscriptionProduct> {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                plans.map { plan ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(plan.productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                },
            )
            .build()
        val details = suspendCancellableCoroutine<List<ProductDetails>?> { continuation ->
            client.queryProductDetailsAsync(params) { result, queryResult ->
                if (result.responseCode == BillingResponseCode.OK) {
                    continuation.resume(queryResult.productDetailsList)
                } else {
                    continuation.resume(null)
                }
            }
        } ?: throw BillingException("Couldn't load subscription plans")

        return details.mapNotNull { productDetails ->
            val plan = SubscriptionPlan.fromProductId(productDetails.productId) ?: return@mapNotNull null
            val offer = productDetails.preferredOffer() ?: return@mapNotNull null
            val phases = offer.pricingPhases.pricingPhaseList
            // The last phase is the open-ended recurring price; a free phase before it is the trial.
            val recurring = phases.lastOrNull() ?: return@mapNotNull null
            val period = BillingPeriod.parse(recurring.billingPeriod) ?: return@mapNotNull null
            offers[plan] = productDetails to offer.offerToken
            SubscriptionProduct(
                plan = plan,
                formattedPrice = recurring.formattedPrice,
                priceMicros = recurring.priceAmountMicros,
                currencyCode = recurring.priceCurrencyCode,
                billingPeriod = period,
                freeTrial = phases.dropLast(1).firstOrNull { it.priceAmountMicros == 0L }
                    ?.let { BillingPeriod.parse(it.billingPeriod) },
            )
        }
    }

    override suspend fun queryActivePlans(): Set<SubscriptionPlan> {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        val purchases = suspendCancellableCoroutine<List<Purchase>?> { continuation ->
            client.queryPurchasesAsync(params) { result, purchases ->
                continuation.resume(if (result.responseCode == BillingResponseCode.OK) purchases else null)
            }
        } ?: throw BillingException("Couldn't check your subscription")

        activeTokens.clear()
        val active = mutableSetOf<SubscriptionPlan>()
        for (purchase in purchases) {
            // Pending payments and suspended (on hold) subscriptions don't unlock anything yet.
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isSuspended) continue
            acknowledgeIfNeeded(purchase)
            for (plan in purchase.plans()) {
                active += plan
                activeTokens[plan] = purchase.purchaseToken
            }
        }
        return active
    }

    override suspend fun purchase(plan: SubscriptionPlan, currentPlan: SubscriptionPlan?): PurchaseResult {
        if (plan !in offers) runCatching { queryProducts(SubscriptionPlan.entries) }
        val (details, offerToken) = offers[plan]
            ?: return PurchaseResult.Failed("This plan isn't available right now")
        val activity = activityTracker.current
            ?: return PurchaseResult.Failed("Couldn't open the Play Store purchase screen")

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .apply {
                // Switching plans replaces the old subscription rather than stacking a second one;
                // the unused time of the old plan is credited against the new one.
                val oldToken = currentPlan?.let(activeTokens::get)
                if (oldToken != null) {
                    setSubscriptionUpdateParams(
                        BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                            .setOldPurchaseToken(oldToken)
                            .setSubscriptionReplacementMode(ReplacementMode.WITH_TIME_PRORATION)
                            .build(),
                    )
                }
            }
            .build()

        val deferred = CompletableDeferred<PurchaseResult>()
        pendingPurchase?.cancel()
        pendingPurchase = deferred
        val launch = withContext(Dispatchers.Main) { client.launchBillingFlow(activity, flowParams) }
        if (launch.responseCode != BillingResponseCode.OK) {
            pendingPurchase = null
            return launch.toFailure()
        }
        return deferred.await()
    }

    override suspend fun restore(): Set<SubscriptionPlan> =
        // Play ties purchases to the device's Google account, so restoring is simply re-querying.
        queryActivePlans()

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>) {
        val deferred = pendingPurchase
        pendingPurchase = null
        if (result.responseCode != BillingResponseCode.OK) {
            deferred?.complete(result.toFailure())
            return
        }
        scope.launch {
            val purchased = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            purchased?.let { acknowledgeIfNeeded(it) }
            val outcome = when {
                purchased != null -> purchased.plans().firstOrNull()?.let { PurchaseResult.Purchased(it) }
                purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> PurchaseResult.Pending
                else -> null
            }
            deferred?.complete(outcome ?: PurchaseResult.Failed("The purchase didn't complete"))
            // Also covers updates with no purchase in flight, e.g. a pending payment clearing later.
            runCatching { queryActivePlans() }.onSuccess { updates.tryEmit(it) }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        suspendCancellableCoroutine { continuation ->
            client.acknowledgePurchase(params) { continuation.resume(Unit) }
        }
    }

    private fun Purchase.plans(): List<SubscriptionPlan> = products.mapNotNull(SubscriptionPlan::fromProductId)

    private fun BillingResult.toFailure(): PurchaseResult = when (responseCode) {
        BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled
        BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseResult.AlreadyOwned
        BillingResponseCode.NETWORK_ERROR, BillingResponseCode.SERVICE_UNAVAILABLE, BillingResponseCode.SERVICE_DISCONNECTED ->
            PurchaseResult.Failed("Couldn't reach the Play Store. Check your connection and try again.")
        BillingResponseCode.BILLING_UNAVAILABLE -> PurchaseResult.Failed("Purchases aren't available on this device")
        else -> PurchaseResult.Failed(debugMessage.ifBlank { "The purchase didn't complete" })
    }
}

/**
 * The offer to sell a plan under: a free-trial offer when Play lists one — it only lists offers the
 * user is eligible for, so this is how an already-used trial drops out — otherwise the base plan.
 */
private fun ProductDetails.preferredOffer(): ProductDetails.SubscriptionOfferDetails? {
    val offers = subscriptionOfferDetails.orEmpty()
    return offers.firstOrNull { offer ->
        offer.offerId != null && offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
    } ?: offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()
}

/** Remembers the foreground activity, which Play needs to show its purchase sheet over. */
private class ResumedActivityTracker : Application.ActivityLifecycleCallbacks {
    private var resumed: WeakReference<Activity>? = null
    val current: Activity? get() = resumed?.get()?.takeUnless { it.isFinishing || it.isDestroyed }

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
