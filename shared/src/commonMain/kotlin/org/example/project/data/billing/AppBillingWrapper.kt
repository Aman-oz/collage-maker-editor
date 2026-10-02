package org.example.project.data.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.data.AppSettings
import org.example.project.i18n.tr

/**
 * The app's single entry point for subscriptions: it loads the plans' store prices, tracks which
 * plan the user holds, and runs purchase / restore. A Koin singleton created at startup, so the
 * entitlement check runs on every launch and store-side changes (renewals, expiries, a pending
 * payment clearing) are picked up while the app is open.
 *
 * Premium itself lives in [AppSettings.isPremium], which the rest of the app already reads: it is
 * only rewritten from a successful store answer, so a launch without connectivity keeps the last
 * known state instead of locking a paying user out.
 */
class AppBillingWrapper(
    private val client: BillingClientAdapter,
    private val settings: AppSettings,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    /** Whether premium is unlocked; the same flow as [AppSettings.isPremium]. */
    val isPremium: StateFlow<Boolean> = settings.isPremium

    private val refreshMutex = Mutex()

    init {
        scope.launch { client.entitlementUpdates.collect(::applyActivePlans) }
        refresh()
    }

    /** Reconnects if needed, then reloads prices (until they're known) and the user's active plans. */
    fun refresh(): Job = scope.launch {
        // A second caller while one refresh runs just waits for it rather than querying twice.
        refreshMutex.withLock {
            if (!client.connect()) {
                _state.update { it.copy(status = BillingStatus.Unavailable) }
                return@withLock
            }
            val productsLoaded = if (_state.value.products.size == SubscriptionPlan.entries.size) {
                true
            } else {
                runCatching { client.queryProducts(SubscriptionPlan.entries) }
                    .onSuccess { products -> _state.update { it.copy(products = products.associateBy(SubscriptionProduct::plan)) } }
                    .isSuccess
            }
            val entitlementsLoaded = runCatching { client.queryActivePlans() }
                .onSuccess(::applyActivePlans)
                .isSuccess
            _state.update {
                it.copy(status = if (productsLoaded && entitlementsLoaded) BillingStatus.Ready else BillingStatus.Unavailable)
            }
        }
    }

    suspend fun purchase(plan: SubscriptionPlan): PurchaseResult {
        if (!client.connect()) {
            _state.update { it.copy(status = BillingStatus.Unavailable) }
            return PurchaseResult.Failed(tr("Couldn't reach the store. Check your connection and try again."))
        }
        val result = client.purchase(plan, currentPlan = _state.value.activePlan?.takeIf { it != plan })
        when (result) {
            // Unlock straight away (a plan switch replaces the old plan), then reconcile with the store.
            is PurchaseResult.Purchased -> {
                applyActivePlans(setOf(result.plan))
                refresh()
            }
            PurchaseResult.AlreadyOwned -> refresh()
            else -> Unit
        }
        return result
    }

    suspend fun restore(): RestoreResult {
        if (!client.connect()) return RestoreResult.Failed(tr("Couldn't reach the store. Check your connection and try again."))
        return try {
            val plans = client.restore()
            applyActivePlans(plans)
            if (plans.isEmpty()) RestoreResult.NothingToRestore else RestoreResult.Restored(plans)
        } catch (e: BillingException) {
            RestoreResult.Failed(e.message ?: tr("Couldn't restore purchases"))
        }
    }

    private fun applyActivePlans(plans: Set<SubscriptionPlan>) {
        _state.update { it.copy(activePlans = plans) }
        settings.setPremium(plans.isNotEmpty())
    }
}
