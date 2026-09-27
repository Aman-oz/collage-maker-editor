package org.example.project.data.billing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BillingPeriodTest {

    @Test
    fun parse_readsEachUnit() {
        assertEquals(BillingPeriod(1, PeriodUnit.Week), BillingPeriod.parse("P1W"))
        assertEquals(BillingPeriod(1, PeriodUnit.Month), BillingPeriod.parse("P1M"))
        assertEquals(BillingPeriod(1, PeriodUnit.Year), BillingPeriod.parse("P1Y"))
        assertEquals(BillingPeriod(3, PeriodUnit.Day), BillingPeriod.parse("P3D"))
    }

    @Test
    fun parse_turnsWholeWeeksOfDaysIntoWeeks() {
        assertEquals(BillingPeriod(1, PeriodUnit.Week), BillingPeriod.parse("P7D"))
        assertEquals(BillingPeriod(2, PeriodUnit.Week), BillingPeriod.parse("P14D"))
    }

    @Test
    fun parse_rejectsWhatItCannotRepresent() {
        assertNull(BillingPeriod.parse(""))
        assertNull(BillingPeriod.parse("P0W"))
        assertNull(BillingPeriod.parse("P1Y2M"))
        assertNull(BillingPeriod.parse("1W"))
    }

    @Test
    fun labels_matchThePaywallCopy() {
        assertEquals("week", BillingPeriod(1, PeriodUnit.Week).unitLabel)
        assertEquals("3 months", BillingPeriod(3, PeriodUnit.Month).unitLabel)
        assertEquals("3-Day Free Trial", BillingPeriod(3, PeriodUnit.Day).trialLabel)
        assertEquals("1-Week Free Trial", BillingPeriod(1, PeriodUnit.Week).trialLabel)
    }
}
