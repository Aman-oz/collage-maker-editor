package org.example.project.data.billing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.example.project.data.AppSettings
import org.example.project.data.KeyValueStore

class AppBillingWrapperTest {

    @Test
    fun refresh_loadsPricesAndUnlocksAnActiveSubscriber() = runTest {
        val store = FakeBillingClient(active = setOf(SubscriptionPlan.Monthly))
        val settings = settings()
        val billing = wrapper(store, settings)
        advanceUntilIdle()

        assertEquals(BillingStatus.Ready, billing.state.value.status)
        assertEquals(SubscriptionPlan.entries.toSet(), billing.state.value.products.keys)
        assertEquals(SubscriptionPlan.Monthly, billing.state.value.activePlan)
        assertTrue(settings.isPremium.value)
    }

    @Test
    fun refresh_revokesPremiumOnceTheStoreSaysNothingIsActive() = runTest {
        val settings = settings(premium = true)
        wrapper(FakeBillingClient(active = emptySet()), settings)
        advanceUntilIdle()

        assertFalse(settings.isPremium.value)
    }

    @Test
    fun refresh_keepsCachedPremiumWhenTheStoreIsUnreachable() = runTest {
        val settings = settings(premium = true)
        val billing = wrapper(FakeBillingClient(connects = false), settings)
        advanceUntilIdle()

        assertEquals(BillingStatus.Unavailable, billing.state.value.status)
        assertTrue(settings.isPremium.value)
    }

    @Test
    fun refresh_keepsCachedPremiumWhenTheEntitlementQueryFails() = runTest {
        val settings = settings(premium = true)
        val billing = wrapper(FakeBillingClient(entitlementsFail = true), settings)
        advanceUntilIdle()

        assertEquals(BillingStatus.Unavailable, billing.state.value.status)
        assertTrue(settings.isPremium.value)
    }

    @Test
    fun purchase_unlocksPremium() = runTest {
        val settings = settings()
        val billing = wrapper(FakeBillingClient(), settings)
        advanceUntilIdle()

        val result = billing.purchase(SubscriptionPlan.Yearly)

        assertEquals(PurchaseResult.Purchased(SubscriptionPlan.Yearly), result)
        assertTrue(settings.isPremium.value)
        assertEquals(SubscriptionPlan.Yearly, billing.state.value.activePlan)
    }

    @Test
    fun purchase_passesTheCurrentPlanWhenSwitching() = runTest {
        val store = FakeBillingClient(active = setOf(SubscriptionPlan.Weekly))
        val billing = wrapper(store, settings())
        advanceUntilIdle()

        billing.purchase(SubscriptionPlan.Yearly)

        assertEquals(SubscriptionPlan.Weekly, store.lastReplacedPlan)
    }

    @Test
    fun purchase_cancelledLeavesPremiumLocked() = runTest {
        val settings = settings()
        val billing = wrapper(FakeBillingClient(purchaseResult = PurchaseResult.Cancelled), settings)
        advanceUntilIdle()

        assertEquals(PurchaseResult.Cancelled, billing.purchase(SubscriptionPlan.Weekly))
        assertFalse(settings.isPremium.value)
    }

    @Test
    fun entitlementUpdates_fromTheStoreApply() = runTest {
        val store = FakeBillingClient(active = setOf(SubscriptionPlan.Weekly))
        val settings = settings()
        wrapper(store, settings)
        advanceUntilIdle()
        assertTrue(settings.isPremium.value)

        store.updates.emit(emptySet())
        advanceUntilIdle()

        assertFalse(settings.isPremium.value)
    }

    @Test
    fun restore_reportsWhetherAnythingWasFound() = runTest {
        val billing = wrapper(FakeBillingClient(), settings())
        advanceUntilIdle()
        assertIs<RestoreResult.NothingToRestore>(billing.restore())

        val restoring = wrapper(FakeBillingClient(restorable = setOf(SubscriptionPlan.Monthly)), settings())
        advanceUntilIdle()
        assertEquals(RestoreResult.Restored(setOf(SubscriptionPlan.Monthly)), restoring.restore())
    }

    // Unconfined so the wrapper's launches run eagerly (the fake store never really suspends), and
    // in backgroundScope so its never-ending entitlement collection doesn't hold the test open.
    private fun TestScope.wrapper(client: BillingClientAdapter, settings: AppSettings) =
        AppBillingWrapper(client, settings, CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)))

    private fun settings(premium: Boolean = false) = AppSettings(InMemoryStore()).apply { setPremium(premium) }
}

private class InMemoryStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()
    override fun getString(key: String): String? = values[key]
    override fun putString(key: String, value: String) {
        values[key] = value
    }
}

private class FakeBillingClient(
    private val connects: Boolean = true,
    private var active: Set<SubscriptionPlan> = emptySet(),
    private val entitlementsFail: Boolean = false,
    private val purchaseResult: PurchaseResult? = null,
    private val restorable: Set<SubscriptionPlan> = emptySet(),
) : BillingClientAdapter {
    val updates = MutableSharedFlow<Set<SubscriptionPlan>>()
    override val entitlementUpdates = updates
    var lastReplacedPlan: SubscriptionPlan? = null

    override suspend fun connect() = connects

    override suspend fun queryProducts(plans: List<SubscriptionPlan>) = plans.map { plan ->
        SubscriptionProduct(plan, "$1.00", 1_000_000, "USD", BillingPeriod(1, PeriodUnit.Week))
    }

    override suspend fun queryActivePlans(): Set<SubscriptionPlan> {
        if (entitlementsFail) throw BillingException("offline")
        return active
    }

    override suspend fun purchase(plan: SubscriptionPlan, currentPlan: SubscriptionPlan?): PurchaseResult {
        lastReplacedPlan = currentPlan
        val result = purchaseResult ?: PurchaseResult.Purchased(plan)
        if (result is PurchaseResult.Purchased) active = setOf(plan)
        return result
    }

    override suspend fun restore(): Set<SubscriptionPlan> = restorable.also { active = it }
}
