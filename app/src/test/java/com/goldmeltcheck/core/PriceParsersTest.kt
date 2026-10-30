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
