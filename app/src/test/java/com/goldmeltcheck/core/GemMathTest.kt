package com.goldmeltcheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GemMathTest {
    @Test fun pricePerCaratConvertsToUsd() {
        // Rs 330,000 for 2 ct at 330 LKR per USD = 500 USD per carat
        assertEquals(500.0, GemMath.pricePerCaratUsd(330_000.0, 330.0, 2.0)!!, 1e-9)
        assertEquals(500.0, GemMath.pricePerCaratUsd(250.0, 1.0, 0.5)!!, 1e-9)
    }

    @Test fun missingOrInvalidInputsGiveNoPrice() {
        assertNull(GemMath.pricePerCaratUsd(null, 1.0, 1.0))
        assertNull(GemMath.pricePerCaratUsd(100.0, 1.0, 0.0))
        assertNull(GemMath.pricePerCaratUsd(100.0, 0.0, 1.0))
        assertNull(GemMath.pricePerCaratUsd(-1.0, 1.0, 1.0))
    }

    @Test fun bracketsFollowWeight() {
        assertEquals(Bracket.UNDER_1, Bracket.of(0.4))
        assertEquals(Bracket.CT_1_2, Bracket.of(1.0))
        assertEquals(Bracket.CT_1_2, Bracket.of(1.99))
        assertEquals(Bracket.CT_2_3, Bracket.of(2.0))
        assertEquals(Bracket.CT_3_5, Bracket.of(4.2))
        assertEquals(Bracket.CT_5_10, Bracket.of(9.99))
        assertEquals(Bracket.CT_10_UP, Bracket.of(10.0))
        assertEquals(Bracket.CT_10_UP, Bracket.of(42.0))
    }

    @Test fun verdictsAcrossARange() {
        val r = PriceRange(300.0, 1_200.0)
        assertEquals(DealVerdict.TOO_GOOD, GemMath.verdict(100.0, r))      // under half the low end
        assertEquals(DealVerdict.GOOD, GemMath.verdict(200.0, r))          // below range but not absurd
        assertEquals(DealVerdict.GOOD, GemMath.verdict(500.0, r))          // lower third
        assertEquals(DealVerdict.FAIR, GemMath.verdict(800.0, r))          // middle third
        assertEquals(DealVerdict.PRICEY, GemMath.verdict(1_100.0, r))      // top third
        assertEquals(DealVerdict.PRICEY, GemMath.verdict(1_200.0, r))      // at the top
        assertEquals(DealVerdict.OVERPRICED, GemMath.verdict(1_500.0, r))
        assertEquals(DealVerdict.NO_REFERENCE, GemMath.verdict(500.0, null))
    }

    @Test fun heatSelectsTheRightPrices() {
        val blue = Gems.byId("blue_sapphire")
        assertTrue(blue.heatMatters)
        val heated = blue.range(Bracket.CT_1_2, Heat.HEATED)!!
        val unheated = blue.range(Bracket.CT_1_2, Heat.UNHEATED)!!
        assertTrue(unheated.high > heated.high)
        // Unknown treatment is judged against heated prices
        assertEquals(heated, blue.range(Bracket.CT_1_2, Heat.UNKNOWN))
        // Stones where heat doesn't matter ignore the choice
        val moon = Gems.byId("moonstone")
        assertEquals(moon.range(Bracket.CT_1_2, Heat.HEATED), moon.range(Bracket.CT_1_2, Heat.UNHEATED))
    }

    @Test fun everyStoneHasSignsAndSaneBrackets() {
        assertTrue(Gems.all.size >= 14)
        assertEquals(Gems.all.size, Gems.all.map { it.id }.toSet().size)
        Gems.all.forEach { g ->
            assertTrue("${g.id} has no signs", g.signs.isNotEmpty())
            assertEquals("${g.id} prices", Bracket.entries.size, g.prices.size)
            g.unheatedPrices?.let { assertEquals("${g.id} unheated", Bracket.entries.size, it.size) }
            (g.prices + g.unheatedPrices.orEmpty()).filterNotNull().forEach { r ->
                assertTrue("${g.id} range ${r.low}-${r.high}", r.low > 0 && r.low < r.high)
            }
        }
        // Sri Lanka focus: the non-Sri Lankan stones are gone
        listOf("diamond", "emerald", "moissanite", "opal").forEach { id -> assertTrue(Gems.all.none { it.id == id }) }
    }

    @Test fun ballparksGoStaleAfterSixMonths() {
        val y = Gems.PRICES_YEAR
        val m = Gems.PRICES_MONTH
        assertEquals(0, Gems.ageInMonths(y, m))
        assertEquals(false, Gems.isStale(y, m))
        val fiveOn = m - 1 + 5
        assertEquals(false, Gems.isStale(y + fiveOn / 12, fiveOn % 12 + 1))
        val sixOn = m - 1 + 6
        assertEquals(true, Gems.isStale(y + sixOn / 12, sixOn % 12 + 1))
        assertEquals(12, Gems.ageInMonths(y + 1, m))
    }
}
