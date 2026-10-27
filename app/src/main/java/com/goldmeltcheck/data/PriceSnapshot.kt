package com.goldmeltcheck.data

import com.goldmeltcheck.core.Catalog

/** Everything the calculator needs about prices, as last saved on the phone. */
data class PriceSnapshot(
    val spotUsdPerOz: Double = Catalog.FALLBACK_SPOT_USD_PER_OZ,
    /** Epoch millis of the last successful gold fetch; 0 = still on the bundled estimate. */
    val spotFetchedAt: Long = 0,
    val unitsPerUsd: Map<String, Double> = emptyMap(),
    /** Epoch millis of the last successful rates fetch; 0 = still on the bundled estimates. */
    val fxFetchedAt: Long = 0,
    val lastAttemptAt: Long = 0,
    val lastError: String? = null,
    val spotOverride: Double? = null,
    val fxOverrides: Map<String, Double> = emptyMap(),
) {
    val effectiveSpot: Double get() = spotOverride ?: spotUsdPerOz

    fun liveRate(code: String): Double =
        if (code == "USD") 1.0 else unitsPerUsd[code] ?: Catalog.currency(code).fallbackPerUsd

    fun effectiveRate(code: String): Double =
        if (code == "USD") 1.0 else fxOverrides[code] ?: liveRate(code)
}

data class SavedInputs(
    val currency: String = "USD",
    val grams: String = "10",
    val purityIndex: Int = Catalog.DEFAULT_PURITY_INDEX,
    val price: String = "1500",
)
