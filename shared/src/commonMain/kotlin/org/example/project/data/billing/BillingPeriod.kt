package org.example.project.data.billing

enum class PeriodUnit { Day, Week, Month, Year }

/**
 * A subscription or trial length. Both stores are normalized to this: Play reports ISO 8601
 * durations ("P1W", "P3D"), and the StoreKit bridge converts its unit + value into the same form.
 */
data class BillingPeriod(val count: Int, val unit: PeriodUnit) {

    /** "week" for one unit, "3 months" for several — the tail of "$4.99 / week". */
    val unitLabel: String
        get() {
            val name = unit.name.lowercase()
            return if (count == 1) name else "$count ${name}s"
        }

    /** "3-Day Free Trial", "1-Week Free Trial". */
    val trialLabel: String get() = "$count-${unit.name} Free Trial"

    companion object {
        private val IsoPeriod = Regex("""^P(\d+)([DWMY])$""")

        /**
         * Parses a single-unit ISO 8601 period such as "P1W" or "P3D", the forms Play and the
         * StoreKit bridge produce. Returns null for anything else rather than guessing.
         */
        fun parse(iso: String): BillingPeriod? {
            val match = IsoPeriod.matchEntire(iso.trim().uppercase()) ?: return null
            val count = match.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
            val unit = when (match.groupValues[2]) {
                "D" -> PeriodUnit.Day
                "W" -> PeriodUnit.Week
                "M" -> PeriodUnit.Month
                else -> PeriodUnit.Year
            }
            // Play reports a one-week trial as "P7D"; show it the way people say it.
            return if (unit == PeriodUnit.Day && count % 7 == 0) {
                BillingPeriod(count / 7, PeriodUnit.Week)
            } else {
                BillingPeriod(count, unit)
            }
        }
    }
}
