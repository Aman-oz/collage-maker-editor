package org.example.project.data.billing

import android.app.Application
import android.content.Context
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun billingPlatformModule(): Module = module {
    // The Application comes from androidContext(), set in CollageApplication before modules load.
    single<BillingClientAdapter> { PlayBillingClientAdapter(get<Context>().applicationContext as Application) }
}
