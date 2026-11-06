package com.goldmeltcheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class PriceParsersTest {
    @Test fun readsGoldPrice() {
        val body = """{"currency":"USD","name":"Gold","price":4351.25,"symbol":"XAU","updatedAt":"2026-10-27T14:00:00Z"}"""
        assertEquals(4351.25, PriceParsers.spotUsdPerOz(body), 0.0)
    }

    @Test fun readsDailyHistoryOldestFirst() {
        // Shape from gold-api.com's API docs (newest first by default)
        val body = """[{"day":"2026-07-01","max_price":4200.5},{"day":"2026-06-30","max_price":4185.0}]"""
        val h = PriceParsers.dailyHistory(body)
        assertEquals(2, h.size)
        assertEquals(4185.0, h[0].usdPerOz, 0.0)
        assertEquals(4200.5, h[1].usdPerOz, 0.0)
        assertEquals(DAY_MS, h[1].at - h[0].at)
        // 2026-06-30 at 12:00 UTC
        assertEquals(java.time.LocalDate.parse("2026-06-30").toEpochDay() * DAY_MS + 12 * HOUR_MS, h[0].at)
    }

    @Test fun dailyHistoryReadsAvgAndSkipsBadRows() {
        val body = """[{"day":"2026-01-02","avg_price":60.5},{"day":"bad","avg_price":1},{"day":"2026-01-03"},{"x":1}]"""
        val h = PriceParsers.dailyHistory(body)
        assertEquals(1, h.size)
        assertEquals(60.5, h[0].usdPerOz, 0.0)
        assertEquals(0, PriceParsers.dailyHistory("[]").size)
    }

    @Test fun readsSilverPrice() {
        val body = """{"currency":"USD","name":"Silver","price":61.45,"symbol":"XAG","updatedAt":"2026-10-27T14:00:00Z"}"""
        assertEquals(61.45, PriceParsers.spotUsdPerOz(body), 0.0)
    }

    @Test fun rejectsGoldResponseWithoutPrice() {
        assertThrows(IllegalArgumentException::class.java) { PriceParsers.spotUsdPerOz("""{"error":"rate limited"}""") }
        assertThrows(IllegalArgumentException::class.java) { PriceParsers.spotUsdPerOz("""{"price":0}""") }
    }

    @Test fun readsRates() {
        val body = """{"result":"success","base_code":"USD","rates":{"USD":1,"EUR":0.861,"JPY":151.2,"BAD":0}}"""
        val rates = PriceParsers.unitsPerUsd(body)
        assertEquals(0.861, rates.getValue("EUR"), 0.0)
        assertEquals(151.2, rates.getValue("JPY"), 0.0)
        assertFalse(rates.containsKey("BAD"))
    }

    @Test fun rejectsFailedRatesResponse() {
        assertThrows(IllegalArgumentException::class.java) {
            PriceParsers.unitsPerUsd("""{"result":"error","error-type":"unsupported-code"}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PriceParsers.unitsPerUsd("""{"result":"success","base_code":"EUR","rates":{"USD":1.16}}""")
        }
    }
}
