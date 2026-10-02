package org.example.project.data.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.module.Module
import org.example.project.i18n.tr

/**
 * The platform store behind [AppBillingWrapper]: Google Play Billing on Android, StoreKit 2 (through
 * a Swift bridge) on iOS. Calls throw [BillingException] when the store can't be reached.
 */
interface BillingClientAdapter {

    /**
     * The active plans, pushed whenever the store changes them outside a purchase call: a renewal,
     * an expiry, a pending payment clearing, or a purchase made on another device.
     */
    val entitlementUpdates: Flow<Set<SubscriptionPlan>>

    /** Connects to the store if needed; false when it isn't available (no Play Store, offline). */
    suspend fun connect(): Boolean

    /** The plans the store sells, with this user's prices and trial eligibility. */
    suspend fun queryProducts(plans: List<SubscriptionPlan>): List<SubscriptionProduct>

    /** Plans the user currently holds: paid, not expired, not revoked or on hold. */
    suspend fun queryActivePlans(): Set<SubscriptionPlan>

    /** Runs the store's purchase sheet for [plan]; [currentPlan] is replaced when switching plans. */
    suspend fun purchase(plan: SubscriptionPlan, currentPlan: SubscriptionPlan?): PurchaseResult

    /** Re-syncs purchases with the store account (App Store sign-in prompt on iOS). */
    suspend fun restore(): Set<SubscriptionPlan>
}

/** Stand-in when no store is wired up (e.g. an iOS host that didn't pass a StoreKit bridge). */
internal class UnavailableBillingClientAdapter : BillingClientAdapter {
    override val entitlementUpdates: Flow<Set<SubscriptionPlan>> = emptyFlow()
    override suspend fun connect(): Boolean = false
    override suspend fun queryProducts(plans: List<SubscriptionPlan>): List<SubscriptionProduct> = throw unavailable()
    override suspend fun queryActivePlans(): Set<SubscriptionPlan> = throw unavailable()
    override suspend fun purchase(plan: SubscriptionPlan, currentPlan: SubscriptionPlan?): PurchaseResult =
        PurchaseResult.Failed(tr("In-app purchases aren't available on this device"))
    override suspend fun restore(): Set<SubscriptionPlan> = throw unavailable()

    private fun unavailable() = BillingException(tr("In-app purchases aren't available on this device"))
}

/** Koin module binding the platform's [BillingClientAdapter]. */
internal expect fun billingPlatformModule(): Module
