package com.goldmeltcheck.core

const val GRAMS_PER_TROY_OUNCE = 31.1034768

enum class Verdict { BELOW_MELT, NEAR_MELT, MODERATE_MARKUP, HIGH_MARKUP }

data class MeltResult(
    val priceUsd: Double,
    val fineGrams: Double,
    val meltUsd: Double,
    val percentOfMelt: Double,
    val premiumUsd: Double,
    val usdPerGram: Double,
    val verdict: Verdict,
)

object MeltMath {
    /**
     * @param price total asking price, in the quoted currency
     * @param unitsPerUsd how many units of the quoted currency buy 1 USD
     * @param spotUsdPerOz gold spot, USD per troy ounce of pure gold
     * @return null when any input is missing or not positive
     */
    fun evaluate(
        grams: Double?,
        purity: Double,
        price: Double?,
        unitsPerUsd: Double?,
        spotUsdPerOz: Double?,
    ): MeltResult? {
        if (grams == null || price == null || unitsPerUsd == null || spotUsdPerOz == null) return null
        if (grams <= 0 || price < 0 || unitsPerUsd <= 0 || spotUsdPerOz <= 0 || purity <= 0) return null

        val priceUsd = price / unitsPerUsd
        val fineGrams = grams * purity
        val meltUsd = fineGrams / GRAMS_PER_TROY_OUNCE * spotUsdPerOz
        val percent = priceUsd / meltUsd * 100
        return MeltResult(
            priceUsd = priceUsd,
            fineGrams = fineGrams,
            meltUsd = meltUsd,
            percentOfMelt = percent,
            premiumUsd = priceUsd - meltUsd,
            usdPerGram = priceUsd / grams,
            verdict = verdictFor(percent),
        )
    }

    fun verdictFor(percentOfMelt: Double): Verdict = when {
        percentOfMelt < 100 -> Verdict.BELOW_MELT
        percentOfMelt <= 108 -> Verdict.NEAR_MELT
        percentOfMelt <= 125 -> Verdict.MODERATE_MARKUP
        else -> Verdict.HIGH_MARKUP
    }

    /** Reads what a person typed: accepts "1,234.5", "1234,5" and surrounding spaces. */
    fun parseAmount(text: String): Double? {
        val t = text.trim().replace(" ", "")
        if (t.isEmpty()) return null
        val lastComma = t.lastIndexOf(',')
        val lastDot = t.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastDot >= 0 ->
                if (lastComma > lastDot) t.replace(".", "").replace(',', '.') else t.replace(",", "")
            lastComma >= 0 -> {
                // A lone comma is a decimal point, unless exactly three digits follow a non-zero
                // whole part ("1,500"), which reads as a thousands separator. "0,750" is 0.75.
                val whole = t.substring(0, lastComma).trimStart('-')
                val isDecimal = t.count { it == ',' } == 1 &&
                    (t.length - lastComma - 1 != 3 || whole.isEmpty() || whole.all { it == '0' })
                if (isDecimal) t.replace(',', '.') else t.replace(",", "")
            }
            else -> t
        }
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }
}
