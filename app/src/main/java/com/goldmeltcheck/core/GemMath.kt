package com.goldmeltcheck.core

/** A price-per-carat ballpark in USD. */
data class PriceRange(val low: Double, val high: Double) {
    operator fun contains(perCaratUsd: Double): Boolean = perCaratUsd in low..high
}

/** Carat-size brackets. Price per carat climbs with size, so each bracket has its own ballpark. */
enum class Bracket(val label: String, val fromCt: Double) {
    UNDER_1("Under 1 ct", 0.0),
    CT_1_2("1–2 ct", 1.0),
    CT_2_3("2–3 ct", 2.0),
    CT_3_5("3–5 ct", 3.0),
    CT_5_10("5–10 ct", 5.0),
    CT_10_UP("10 ct +", 10.0);

    companion object {
        /** The bracket a weight falls in; a stone of exactly 2.00 ct is in "2–3 ct". */
        fun of(carats: Double): Bracket = entries.last { carats >= it.fromCt }
    }
}

enum class Heat(val label: String) { HEATED("Heated"), UNHEATED("Unheated"), UNKNOWN("Don't know") }

enum class DealVerdict(val label: String) {
    TOO_GOOD("Too good to be true"),
    GOOD("Good deal"),
    FAIR("Fair price"),
    PRICEY("Pricey"),
    OVERPRICED("Overpriced"),
    NO_REFERENCE("No ballpark yet"),
}

object GemMath {
    /** Below this share of the range's low end, a price is suspicious rather than a bargain. */
    const val TOO_GOOD_SHARE = 0.5

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

    /**
     * Where an asking price per carat sits in a ballpark: well below it is suspicious, the lower third is a
     * good deal, the middle third fair, the top third pricey, and above it overpriced.
     */
    fun verdict(perCaratUsd: Double, range: PriceRange?): DealVerdict {
        if (range == null) return DealVerdict.NO_REFERENCE
        if (perCaratUsd < range.low * TOO_GOOD_SHARE) return DealVerdict.TOO_GOOD
        if (perCaratUsd > range.high) return DealVerdict.OVERPRICED
        if (perCaratUsd <= range.low) return DealVerdict.GOOD
        val position = (perCaratUsd - range.low) / (range.high - range.low).coerceAtLeast(1e-9)
        return when {
            position <= 1.0 / 3 -> DealVerdict.GOOD
            position <= 2.0 / 3 -> DealVerdict.FAIR
            else -> DealVerdict.PRICEY
        }
    }
}
