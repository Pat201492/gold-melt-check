package com.goldmeltcheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeltMathTest {
    private fun eval(grams: Double = 31.1034768, purity: Double = 1.0, price: Double = 4000.0, rate: Double = 1.0, spot: Double = 4000.0) =
        MeltMath.evaluate(grams, purity, price, rate, spot)

    @Test fun oneOunceAtSpotIsExactlyMelt() {
        val r = eval()!!
        assertEquals(4000.0, r.meltUsd, 1e-9)
        assertEquals(100.0, r.percentOfMelt, 1e-9)
        assertEquals(0.0, r.premiumUsd, 1e-9)
    }

    @Test fun convertsQuotedCurrencyToUsd() {
        // 860 EUR at 0.86 EUR per USD = 1000 USD
        val r = eval(price = 860.0, rate = 0.86)!!
        assertEquals(1000.0, r.priceUsd, 1e-9)
        assertEquals(25.0, r.percentOfMelt, 1e-9)
    }

    @Test fun purityScalesMeltValue() {
        // 10 g of 18k at $4350/oz
        val r = MeltMath.evaluate(10.0, 0.75, 1500.0, 1.0, 4350.0)!!
        assertEquals(7.5, r.fineGrams, 1e-9)
        assertEquals(7.5 / GRAMS_PER_TROY_OUNCE * 4350.0, r.meltUsd, 1e-9)
        assertEquals(150.0, r.usdPerGram, 1e-9)
    }

    @Test fun pricePerGramInAnyCurrency() {
        // $3110.34768/oz is exactly $100 per gram of pure gold
        val spot = GRAMS_PER_TROY_OUNCE * 100
        assertEquals(100.0, MeltMath.pricePerGram(spot, 1.0), 1e-9)
        assertEquals(86.0, MeltMath.pricePerGram(spot, 0.86), 1e-9)
        assertEquals(75.0, MeltMath.pricePerGram(spot, 1.0, purity = 0.75), 1e-9)
    }

    @Test fun convertsBetweenAnyTwoCurrencies() {
        // USD -> EUR at 0.86 EUR per USD
        assertEquals(0.86, MeltMath.convert(1.0, 1.0, 0.86), 1e-12)
        // EUR -> GBP goes through USD: 100 EUR = 100/0.86 USD = 87.209... GBP at 0.75
        assertEquals(100 / 0.86 * 0.75, MeltMath.convert(100.0, 0.86, 0.75), 1e-9)
        // Same currency is unchanged; round trip returns the start
        assertEquals(42.0, MeltMath.convert(42.0, 151.2, 151.2), 1e-12)
        assertEquals(42.0, MeltMath.convert(MeltMath.convert(42.0, 0.86, 151.2), 151.2, 0.86), 1e-9)
    }

    @Test fun silverHasWiderMarkupBands() {
        assertEquals(Verdict.BELOW_MELT, MeltMath.verdictFor(99.0, Metal.SILVER))
        assertEquals(Verdict.NEAR_MELT, MeltMath.verdictFor(115.0, Metal.SILVER))
        assertEquals(Verdict.MODERATE_MARKUP, MeltMath.verdictFor(140.0, Metal.SILVER))
        assertEquals(Verdict.HIGH_MARKUP, MeltMath.verdictFor(151.0, Metal.SILVER))
        // the same 115% is already a moderate markup for gold
        assertEquals(Verdict.MODERATE_MARKUP, MeltMath.verdictFor(115.0, Metal.GOLD))
    }

    @Test fun silverMeltValue() {
        // 1 troy oz of sterling (.925) at $62/oz spot = $57.35 melt; paying $68.82 is 120%
        val r = MeltMath.evaluate(GRAMS_PER_TROY_OUNCE, 0.925, 68.82, 1.0, 62.0, Metal.SILVER)!!
        assertEquals(57.35, r.meltUsd, 1e-9)
        assertEquals(120.0, r.percentOfMelt, 1e-9)
        assertEquals(Verdict.NEAR_MELT, r.verdict)
    }

    @Test fun everyMetalHasAValidDefaultPurity() {
        Metal.entries.forEach { m ->
            assertEquals(m.purities[m.defaultPurityIndex], m.purity(m.defaultPurityIndex))
            assertEquals(m.purities[m.defaultPurityIndex], m.purity(99))
            m.purities.forEach { assertTrue(it.fineness in 0.0..1.0) }
        }
        assertEquals(Metal.GOLD, Metal.from(null))
        assertEquals(Metal.SILVER, Metal.from("SILVER"))
    }

    @Test fun meltValueForAnyWeight() {
        // 10 g of 18k (.750) at $4,000/oz = 7.5 g fine = 0.24113 oz = $964.52
        assertEquals(7.5 / GRAMS_PER_TROY_OUNCE * 4000.0, MeltMath.meltValueUsd(10.0, 0.75, 4000.0), 1e-9)
        assertEquals(4000.0, MeltMath.meltValueUsd(GRAMS_PER_TROY_OUNCE, 1.0, 4000.0), 1e-9)
    }

    @Test fun perGramPriceMultipliesByWeight() {
        assertEquals(850.0, MeltMath.totalPrice(85.0, 10.0, perGram = true)!!, 1e-9)
        assertEquals(85.0, MeltMath.totalPrice(85.0, 10.0, perGram = false)!!, 1e-9)
        assertEquals(85.0, MeltMath.totalPrice(85.0, null, perGram = false)!!, 1e-9)
        assertNull(MeltMath.totalPrice(85.0, null, perGram = true))
        assertNull(MeltMath.totalPrice(85.0, 0.0, perGram = true))
        assertNull(MeltMath.totalPrice(null, 10.0, perGram = true))
        // Per-gram quote feeds the same percent-of-melt result as the equivalent total
        val total = MeltMath.evaluate(10.0, 0.75, MeltMath.totalPrice(100.0, 10.0, true), 1.0, 4000.0)!!
        val direct = MeltMath.evaluate(10.0, 0.75, 1000.0, 1.0, 4000.0)!!
        assertEquals(direct.percentOfMelt, total.percentOfMelt, 1e-9)
    }

    @Test fun verdictBands() {
        assertEquals(Verdict.BELOW_MELT, MeltMath.verdictFor(99.9))
        assertEquals(Verdict.NEAR_MELT, MeltMath.verdictFor(100.0))
        assertEquals(Verdict.NEAR_MELT, MeltMath.verdictFor(108.0))
        assertEquals(Verdict.MODERATE_MARKUP, MeltMath.verdictFor(125.0))
        assertEquals(Verdict.HIGH_MARKUP, MeltMath.verdictFor(125.1))
    }

    @Test fun missingOrInvalidInputsGiveNoResult() {
        assertNull(MeltMath.evaluate(null, 1.0, 1.0, 1.0, 1.0))
        assertNull(MeltMath.evaluate(0.0, 1.0, 1.0, 1.0, 1.0))
        assertNull(MeltMath.evaluate(1.0, 1.0, -1.0, 1.0, 1.0))
        assertNull(MeltMath.evaluate(1.0, 1.0, 1.0, 0.0, 1.0))
        assertNull(MeltMath.evaluate(1.0, 1.0, 1.0, 1.0, null))
    }

    @Test fun parsesUnambiguousAmountsTheSameEverywhere() {
        for (phone in listOf('.', ',')) {
            assertEquals(1500.0, MeltMath.parseAmount("1500", phone)!!, 0.0)
            assertEquals(1234.5, MeltMath.parseAmount("1,234.5", phone)!!, 0.0)
            assertEquals(1234.5, MeltMath.parseAmount("1.234,5", phone)!!, 0.0)
            assertEquals(1234567.0, MeltMath.parseAmount("1,234,567", phone)!!, 0.0)
            assertEquals(1234567.0, MeltMath.parseAmount("1.234.567", phone)!!, 0.0)
            assertEquals(12.5, MeltMath.parseAmount(" 12,5 ", phone)!!, 0.0)
            assertEquals(12.5, MeltMath.parseAmount("12.5", phone)!!, 0.0)
            assertEquals(0.75, MeltMath.parseAmount("0,750", phone)!!, 0.0)
            assertEquals(0.75, MeltMath.parseAmount("0.750", phone)!!, 0.0)
            assertEquals(0.75, MeltMath.parseAmount(",750", phone)!!, 0.0)
            assertEquals(1.2345, MeltMath.parseAmount("1,2345", phone)!!, 0.0)
            assertNull(MeltMath.parseAmount("", phone))
            assertNull(MeltMath.parseAmount("abc", phone))
        }
    }

    @Test fun phoneSettingDecidesTheAmbiguousCase() {
        // US-style phone: comma groups thousands, dot is decimal
        assertEquals(1500.0, MeltMath.parseAmount("1,500", '.')!!, 0.0)
        assertEquals(1.5, MeltMath.parseAmount("1.500", '.')!!, 0.0)
        // European-style phone: the reverse
        assertEquals(1.5, MeltMath.parseAmount("1,500", ',')!!, 0.0)
        assertEquals(1500.0, MeltMath.parseAmount("1.500", ',')!!, 0.0)
        assertEquals(10000.0, MeltMath.parseAmount("10.000", ',')!!, 0.0)
    }
}
