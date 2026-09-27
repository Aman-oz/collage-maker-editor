package org.example.project.ui.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.project.data.billing.AppBillingWrapper
import org.example.project.data.billing.BillingState
import org.example.project.data.billing.BillingStatus
import org.example.project.data.billing.PurchaseResult
import org.example.project.data.billing.RestoreResult
import org.example.project.data.billing.SubscriptionPlan

data class PremiumUiState(
    val billing: BillingState = BillingState(),
    val isPremium: Boolean = false,
    /** A purchase or restore is in flight; the buttons ignore taps until it finishes. */
    val busy: Boolean = false,
)

sealed interface PremiumEvent {
    data class Message(val text: String) : PremiumEvent

    /** Premium was just unlocked (purchase or restore); the paywall closes. */
    data object Unlocked : PremiumEvent
}

/** Runs the paywall's purchase and restore through [AppBillingWrapper] and reports the outcome. */
class PremiumViewModel(private val billing: AppBillingWrapper) : ViewModel() {

    private val busy = MutableStateFlow(false)

    val uiState: StateFlow<PremiumUiState> = combine(billing.state, billing.isPremium, busy, ::PremiumUiState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PremiumUiState(billing.state.value, billing.isPremium.value),
        )

    private val _events = Channel<PremiumEvent>(Channel.BUFFERED)
    val events: Flow<PremiumEvent> = _events.receiveAsFlow()

    init {
        // Prices may not have loaded at launch (offline, store still connecting); retry on open.
        if (billing.state.value.status != BillingStatus.Ready) billing.refresh()
    }

    fun purchase(plan: SubscriptionPlan) = runBusy {
        when (val result = billing.purchase(plan)) {
            is PurchaseResult.Purchased -> _events.send(PremiumEvent.Unlocked)
            PurchaseResult.Pending -> message("Your payment is pending. Premium unlocks as soon as it's confirmed.")
            PurchaseResult.AlreadyOwned -> message("You're already subscribed to this plan")
            is PurchaseResult.Failed -> message(result.message)
            // Backing out of the store sheet is a choice, not an error; say nothing.
            PurchaseResult.Cancelled -> Unit
        }
    }

    fun restore() = runBusy {
        when (val result = billing.restore()) {
            is RestoreResult.Restored -> _events.send(PremiumEvent.Unlocked)
            RestoreResult.NothingToRestore -> message("No active subscription was found for this account")
            is RestoreResult.Failed -> message(result.message)
        }
    }

    private suspend fun message(text: String) = _events.send(PremiumEvent.Message(text))

    private fun runBusy(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                busy.value = false
            }
        }
    }
}
