package org.example.project.ui.premium

import kotlin.math.roundToInt
import org.example.project.data.billing.BillingPeriod
import org.example.project.data.billing.PeriodUnit
import org.example.project.data.billing.SubscriptionProduct
import org.example.project.i18n.tr

/**
 * How much cheaper [yearly] is than a year of [weekly], as a whole percentage — the "SAVE 80%" on
 * the Annual plan. It is worked out from the store's own prices rather than hard-coded, so the badge
 * can never promise a discount the store isn't giving. Returns null when either price is missing,
 * the currencies differ, either period isn't the expected one, or there is no saving.
 */
internal fun annualSavingsPercent(weekly: SubscriptionProduct?, yearly: SubscriptionProduct?): Int? {
    if (weekly == null || yearly == null) return null
    if (weekly.currencyCode != yearly.currencyCode) return null
    if (weekly.billingPeriod != BillingPeriod(1, PeriodUnit.Week)) return null
    if (yearly.billingPeriod != BillingPeriod(1, PeriodUnit.Year)) return null
    val weeklyPerYear = weekly.priceMicros.toDouble() * WeeksPerYear
    if (weeklyPerYear <= 0.0) return null
    val percent = ((1.0 - yearly.priceMicros / weeklyPerYear) * 100).roundToInt()
    return percent.takeIf { it > 0 }
}

private const val WeeksPerYear = 52

/** How long the launch-flow limited-time offer stays available after it is first shown. */
internal const val LaunchOfferDurationMillis = 2 * 60 * 60 * 1000L

/** When an offer first shown at [startedAt] runs out, or null if it already has at [now]. */
internal fun launchOfferEndsAt(startedAt: Long, now: Long): Long? =
    (startedAt + LaunchOfferDurationMillis).takeIf { now < it }

/** Hours, minutes and seconds left, for the offer's countdown boxes. */
internal data class Countdown(val hours: Int, val minutes: Int, val seconds: Int)

internal fun countdown(remainingMillis: Long): Countdown {
    // Rounded up, so the last second reads 00:00:01 rather than already showing zero.
    val totalSeconds = ((remainingMillis.coerceAtLeast(0) + 999) / 1000).toInt()
    return Countdown(totalSeconds / 3600, totalSeconds / 60 % 60, totalSeconds % 60)
}

/**
 * Formats [micros] the way the store formatted [template] ("PKR 900.00", "€4,99", "Rs 1,200"): same
 * currency text, decimal separator and digit count. There is no shared currency formatter on every
 * platform, and the store only formats its own prices, so a derived price such as "a year of the
 * weekly plan" borrows the look of one. Grouping is always in threes. Null if [template] has no number.
 */
internal fun formatPriceLike(template: String, micros: Long): String? {
    val match = PriceNumber.find(template) ?: return null
    val number = match.value
    // A final '.' or ',' followed by one or two digits is the decimal separator; any other is grouping.
    val lastSeparator = number.indexOfLast { it == '.' || it == ',' }
    val decimals = (number.length - lastSeparator - 1).takeIf { lastSeparator >= 0 && it in 1..2 } ?: 0
    val decimalSeparator = if (decimals > 0) number[lastSeparator] else null
    val integerPart = if (decimals > 0) number.substring(0, lastSeparator) else number
    val groupSeparator = integerPart.firstOrNull { !it.isDigit() } ?: if (decimalSeparator == ',') '.' else ','

    var scale = 1L
    repeat(decimals) { scale *= 10 }
    val units = (micros * scale + 500_000) / 1_000_000
    val whole = (units / scale).toString().reversed().chunked(3).joinToString(groupSeparator.toString()).reversed()
    val formatted = if (decimalSeparator == null) {
        whole
    } else {
        whole + decimalSeparator + (units % scale).toString().padStart(decimals, '0')
    }
    return template.replaceRange(match.range, formatted)
}

private val PriceNumber = Regex("""\d(?:[\d.,\u00A0\u202F ]*\d)?""")

/**
 * [BillingPeriod.unitLabel] in the app's language: "week" for one unit, "3 months" for several —
 * what follows "every" in a plan's renewal line.
 */
internal fun BillingPeriod.localizedUnitLabel(): String = when (unit) {
    PeriodUnit.Day -> if (count == 1) tr("day") else tr("{0} days", count)
    PeriodUnit.Week -> if (count == 1) tr("week") else tr("{0} weeks", count)
    PeriodUnit.Month -> if (count == 1) tr("month") else tr("{0} months", count)
    PeriodUnit.Year -> if (count == 1) tr("year") else tr("{0} years", count)
}

/** [BillingPeriod.trialLabel] in the app's language: "3-Day Free Trial". */
internal fun BillingPeriod.localizedTrialLabel(): String = when (unit) {
    PeriodUnit.Day -> tr("{0}-Day Free Trial", count)
    PeriodUnit.Week -> tr("{0}-Week Free Trial", count)
    PeriodUnit.Month -> tr("{0}-Month Free Trial", count)
    PeriodUnit.Year -> tr("{0}-Year Free Trial", count)
}
