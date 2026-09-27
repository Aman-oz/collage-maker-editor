package org.example.project.data.billing

import org.koin.core.module.Module
import org.koin.dsl.module

/** Set by `MainViewController` before Koin starts. */
internal var registeredStoreKitBridge: StoreKitBridge? = null

internal actual fun billingPlatformModule(): Module = module {
    single<BillingClientAdapter> {
        registeredStoreKitBridge?.let(::StoreKitBillingAdapter) ?: UnavailableBillingClientAdapter()
    }
}
