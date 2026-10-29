package com.goldmeltcheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GemMathTest {
    private val natural = PriceRange(3500.0, 6000.0)
    private val lab = PriceRange(250.0, 1500.0)

    @Test fun pricePerCaratConvertsToUsd() {
        // 1,720 EUR for 2 ct at 0.86 EUR per USD = 1,000 USD per carat
        assertEquals(1000.0, GemMath.pricePerCaratUsd(1720.0, 0.86, 2.0)!!, 1e-9)
        assertEquals(500.0, GemMath.pricePerCaratUsd(250.0, 1.0, 0.5)!!, 1e-9)
    }

    @Test fun missingOrInvalidInputsGiveNoPrice() {
        assertNull(GemMath.pricePerCaratUsd(null, 1.0, 1.0))
        assertNull(GemMath.pricePerCaratUsd(100.0, 1.0, 0.0))
        assertNull(GemMath.pricePerCaratUsd(100.0, 0.0, 1.0))
        assertNull(GemMath.pricePerCaratUsd(-1.0, 1.0, 1.0))
    }

    @Test fun bandsAgainstBothRanges() {
        assertEquals(PriceBand.BELOW_ALL, GemMath.band(100.0, natural, lab))
        assertEquals(PriceBand.LAB, GemMath.band(250.0, natural, lab))
        assertEquals(PriceBand.LAB, GemMath.band(1500.0, natural, lab))
        assertEquals(PriceBand.BETWEEN, GemMath.band(2000.0, natural, lab))
        assertEquals(PriceBand.NATURAL, GemMath.band(4000.0, natural, lab))
        assertEquals(PriceBand.ABOVE_ALL, GemMath.band(9000.0, natural, lab))
    }

    @Test fun bandsWithOneOrNoRange() {
        assertEquals(PriceBand.NO_REFERENCE, GemMath.band(100.0, null, null))
        assertEquals(PriceBand.LAB, GemMath.band(300.0, null, PriceRange(50.0, 800.0)))
        assertEquals(PriceBand.ABOVE_ALL, GemMath.band(900.0, null, PriceRange(50.0, 800.0)))
        assertEquals(PriceBand.BELOW_ALL, GemMath.band(1.0, PriceRange(2.0, 60.0), null))
        assertEquals(PriceBand.BOTH, GemMath.band(500.0, PriceRange(300.0, 900.0), PriceRange(100.0, 600.0)))
    }

    @Test fun everyStoneHasSignsAndSaneDefaults() {
        assertTrue(Gems.all.size >= 9)
        assertEquals(Gems.all.size, Gems.all.map { it.id }.toSet().size)
        Gems.all.forEach { g ->
            assertTrue("${g.id} has no signs", g.signs.isNotEmpty())
            listOfNotNull(g.natural, g.lab).forEach { r -> assertTrue("${g.id} range", r.low in 0.0..r.high) }
        }
    }
}
