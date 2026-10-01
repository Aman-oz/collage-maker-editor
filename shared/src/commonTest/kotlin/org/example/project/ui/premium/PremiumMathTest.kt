package org.example.project.ui.premium

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.example.project.data.billing.BillingPeriod
import org.example.project.data.billing.PeriodUnit
import org.example.project.data.billing.SubscriptionPlan
import org.example.project.data.billing.SubscriptionProduct

class PremiumMathTest {

    private fun product(
        plan: SubscriptionPlan,
        micros: Long,
        period: BillingPeriod,
        currency: String = "PKR",
    ) = SubscriptionProduct(plan, "", micros, currency, period)

    private val weekly = product(SubscriptionPlan.Weekly, 900_000_000, BillingPeriod(1, PeriodUnit.Week))

    @Test
    fun annualSavingsPercent_comparesAgainstAYearOfWeekly() {
        // 900 × 52 = 46,800; 9,360 is 20% of that, so the saving is 80%.
        val yearly = product(SubscriptionPlan.Yearly, 9_360_000_000, BillingPeriod(1, PeriodUnit.Year))
        assertEquals(80, annualSavingsPercent(weekly, yearly))
    }

    @Test
    fun annualSavingsPercent_nullWhenPriceMissingOrCurrenciesDiffer() {
        val yearly = product(SubscriptionPlan.Yearly, 1_200_000_000, BillingPeriod(1, PeriodUnit.Year), "USD")
        assertNull(annualSavingsPercent(null, yearly))
        assertNull(annualSavingsPercent(weekly, null))
        assertNull(annualSavingsPercent(weekly, yearly))
    }

    @Test
    fun annualSavingsPercent_nullWhenThereIsNoSaving() {
        val yearly = product(SubscriptionPlan.Yearly, 50_000_000_000, BillingPeriod(1, PeriodUnit.Year))
        assertNull(annualSavingsPercent(weekly, yearly))
    }

    @Test
    fun annualSavingsPercent_nullForUnexpectedPeriods() {
        val twoWeeks = weekly.copy(billingPeriod = BillingPeriod(2, PeriodUnit.Week))
        val yearly = product(SubscriptionPlan.Yearly, 9_360_000_000, BillingPeriod(1, PeriodUnit.Year))
        assertNull(annualSavingsPercent(twoWeeks, yearly))
    }
}

class LaunchOfferMathTest {

    @Test
    fun launchOfferEndsAt_twoHoursAfterStart() {
        assertEquals(LaunchOfferDurationMillis, launchOfferEndsAt(startedAt = 0, now = 1_000))
        assertNull(launchOfferEndsAt(startedAt = 0, now = LaunchOfferDurationMillis))
    }

    @Test
    fun countdown_splitsAndRoundsUp() {
        assertEquals(Countdown(0, 56, 28), countdown((56 * 60 + 28) * 1000L))
        assertEquals(Countdown(1, 59, 59), countdown(LaunchOfferDurationMillis - 1_000))
        assertEquals(Countdown(0, 0, 1), countdown(1))
        assertEquals(Countdown(0, 0, 0), countdown(-5))
    }

    @Test
    fun formatPriceLike_keepsTheStoreFormat() {
        assertEquals("PKR 46,800.00", formatPriceLike("PKR 900.00", 46_800_000_000))
        assertEquals("Rs 46,800", formatPriceLike("Rs 900", 46_800_000_000))
        assertEquals("$259.48", formatPriceLike("$4.99", 259_480_000))
        assertEquals("46.800,00 €", formatPriceLike("900,00 €", 46_800_000_000))
        assertEquals("PKR 62,400", formatPriceLike("PKR 1,200", 62_400_000_000))
        assertNull(formatPriceLike("Free", 1))
    }
}
