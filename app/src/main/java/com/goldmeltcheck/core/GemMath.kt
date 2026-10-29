package com.goldmeltcheck.core

/** A price-per-carat ballpark in USD. */
data class PriceRange(val low: Double, val high: Double) {
    operator fun contains(perCaratUsd: Double): Boolean = perCaratUsd in low..high
}

/** Where an asking price per carat sits against a stone's natural and lab-grown ballparks. */
enum class PriceBand { BELOW_ALL, LAB, BETWEEN, NATURAL, ABOVE_ALL, BOTH, NO_REFERENCE }

object GemMath {
    /**
     * @param price total asking price, in the quoted currency
     * @param unitsPerUsd how many units of the quoted currency buy 1 USD
     * @return USD per carat, or null when an input is missing or not positive
     */
    fun pricePerCaratUsd(price: Double?, unitsPerUsd: Double?, carats: Double?): Double? {
        if (price == null || unitsPerUsd == null || carats == null) return null
        if (price < 0 || unitsPerUsd <= 0 || carats <= 0) return null
        return price / unitsPerUsd / carats
    }

    fun band(perCaratUsd: Double, natural: PriceRange?, lab: PriceRange?): PriceBand {
        if (natural == null && lab == null) return PriceBand.NO_REFERENCE
        val inNatural = natural?.contains(perCaratUsd) == true
        val inLab = lab?.contains(perCaratUsd) == true
        return when {
            inNatural && inLab -> PriceBand.BOTH
            inLab -> PriceBand.LAB
            inNatural -> PriceBand.NATURAL
            perCaratUsd < listOfNotNull(natural?.low, lab?.low).min() -> PriceBand.BELOW_ALL
            perCaratUsd > listOfNotNull(natural?.high, lab?.high).max() -> PriceBand.ABOVE_ALL
            else -> PriceBand.BETWEEN
        }
    }
}
