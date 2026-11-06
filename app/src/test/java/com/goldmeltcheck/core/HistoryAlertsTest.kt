package com.goldmeltcheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryAlertsTest {
    private val t0 = 1_800_000_000_000L

    /** One point a day for [days] days at the given prices (cycled), ending at t0 + days. */
    private fun daily(vararg prices: Double, days: Int = prices.size): List<PricePoint> =
        (0 until days).map { PricePoint(t0 + it * DAY_MS, prices[it % prices.size]) }

    @Test fun appendKeepsRecentDetailAndThinsOldToOnePerDay() {
        var h = emptyList<PricePoint>()
        // 30 days of hourly points
        for (i in 0 until 30 * 24) h = History.append(h, PricePoint(t0 + i * HOUR_MS, 100.0 + i))
        val now = h.last().at
        val recent = h.filter { it.at >= now - History.DETAIL_MS }
        val old = h.filter { it.at < now - History.DETAIL_MS }
        assertTrue(recent.size >= 14 * 24)
        assertEquals(old.size, old.map { it.at / DAY_MS }.toSet().size) // one per day
        assertEquals(h.sortedBy { it.at }, h)
    }

    @Test fun appendDropsPointsOlderThanTwoYears() {
        val old = PricePoint(t0, 1.0)
        val h = History.append(listOf(old), PricePoint(t0 + History.KEEP_MS + DAY_MS, 2.0))
        assertEquals(1, h.size)
    }

    @Test fun seedFillsOnlyBeforeTheFirstSavedPrice() {
        val seed = daily(100.0, 101.0, 102.0, 103.0, 104.0)          // days 0..4
        val saved = listOf(PricePoint(t0 + 3 * DAY_MS + HOUR_MS, 200.0)) // the phone's own check on day 3
        val merged = History.mergeSeed(saved, seed, now = t0 + 5 * DAY_MS)
        assertEquals(listOf(100.0, 101.0, 102.0, 103.0, 200.0), merged.map { it.usdPerOz })
        // running it again adds nothing
        assertEquals(merged, History.mergeSeed(merged, seed, now = t0 + 5 * DAY_MS))
        // empty phone history takes the whole seed
        assertEquals(5, History.mergeSeed(emptyList(), seed, now = t0 + 5 * DAY_MS).size)
    }

    @Test fun changePercentNeedsHistoryThatFarBack() {
        val h = daily(100.0, 110.0)
        assertEquals(10.0, History.changePercent(h, DAY_MS)!!, 1e-9)
        assertNull(History.changePercent(h, WEEK_MS))
    }

    @Test fun rangeOverWindow() {
        val h = daily(100.0, 90.0, 120.0, 105.0, days = 29)
        val (low, high) = History.range(h, 4 * WEEK_MS)!!
        assertEquals(90.0, low, 0.0)
        assertEquals(120.0, high, 0.0)
        assertNull(History.range(h.takeLast(10), 4 * WEEK_MS))
    }

    @Test fun newLowFiresOnlyWithFullWindowAndNewLow() {
        val rule = AlertRule("r", Metal.GOLD, AlertType.NEW_LOW, 4.0)
        val flat = daily(100.0, days = 29)
        assertNull(Alerts.evaluate(rule, flat, null)) // not lower
        val dropped = flat + PricePoint(flat.last().at + HOUR_MS, 95.0)
        val hit = Alerts.evaluate(rule, dropped, null)
        assertNotNull(hit)
        assertEquals("Gold hit a 4-week low", hit!!.title)
        // too little history: no alert even though it's the lowest seen
        assertNull(Alerts.evaluate(rule, flat.takeLast(5) + PricePoint(flat.last().at + HOUR_MS, 95.0), null))
    }

    @Test fun newHighFires() {
        val rule = AlertRule("r", Metal.SILVER, AlertType.NEW_HIGH, 1.0)
        val h = daily(60.0, days = 8) + PricePoint(t0 + 8 * DAY_MS, 61.0)
        assertEquals("Silver hit a 1-week high", Alerts.evaluate(rule, h, null)!!.title)
    }

    @Test fun targetsFireOnTheCrossingOnly() {
        val below = AlertRule("b", Metal.GOLD, AlertType.BELOW, 4000.0)
        val crossing = listOf(PricePoint(t0, 4010.0), PricePoint(t0 + HOUR_MS, 3990.0))
        assertNotNull(Alerts.evaluate(below, crossing, null))
        val stillBelow = crossing + PricePoint(t0 + 2 * HOUR_MS, 3980.0)
        assertNull(Alerts.evaluate(below, stillBelow, null))

        val above = AlertRule("a", Metal.SILVER, AlertType.ABOVE, 65.0)
        assertNotNull(Alerts.evaluate(above, listOf(PricePoint(t0, 64.0), PricePoint(t0 + HOUR_MS, 65.5)), null))
        assertNull(Alerts.evaluate(above, listOf(PricePoint(t0, 66.0), PricePoint(t0 + HOUR_MS, 67.0)), null))
    }

    @Test fun dailyMoveUsesThePriceAboutADayAgo() {
        val rule = AlertRule("d", Metal.GOLD, AlertType.DAILY_MOVE, 2.0)
        val up3 = listOf(PricePoint(t0, 100.0), PricePoint(t0 + DAY_MS, 103.0))
        assertEquals("Gold up 3% in a day", Alerts.evaluate(rule, up3, null)!!.title)
        val up1 = listOf(PricePoint(t0, 100.0), PricePoint(t0 + DAY_MS, 101.0))
        assertNull(Alerts.evaluate(rule, up1, null))
        val down = listOf(PricePoint(t0, 100.0), PricePoint(t0 + DAY_MS, 97.5))
        assertEquals("Gold down 2.5% in a day", Alerts.evaluate(rule, down, null)!!.title)
    }

    @Test fun cooldownAndDisabledRulesStayQuiet() {
        val rule = AlertRule("b", Metal.GOLD, AlertType.BELOW, 4000.0)
        val h = listOf(PricePoint(t0, 4010.0), PricePoint(t0 + HOUR_MS, 3990.0))
        assertNull(Alerts.evaluate(rule, h, lastFiredAt = t0))
        assertNotNull(Alerts.evaluate(rule, h, lastFiredAt = t0 + HOUR_MS - Alerts.COOLDOWN_MS - 1))
        assertNull(Alerts.evaluate(rule.copy(enabled = false), h, null))
    }

    @Test fun descriptionsReadNaturally() {
        assertEquals("Gold below $4,000/oz", AlertRule("x", Metal.GOLD, AlertType.BELOW, 4000.0).describe())
        assertEquals("Silver above $65.50/oz", AlertRule("x", Metal.SILVER, AlertType.ABOVE, 65.5).describe())
        assertEquals("Gold hits a new 4-week low", Alerts.defaults.first().describe())
        assertFalse(Alerts.defaults.isEmpty())
    }
}
