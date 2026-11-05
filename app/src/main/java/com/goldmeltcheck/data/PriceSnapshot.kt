package com.goldmeltcheck.data

import com.goldmeltcheck.core.Catalog
import com.goldmeltcheck.core.Metal

/** Everything the calculator needs about prices, as last saved on the phone. */
data class PriceSnapshot(
    /** Gold spot, USD per troy oz. */
    val spotUsdPerOz: Double = Metal.GOLD.fallbackSpotUsdPerOz,
    /** Epoch millis of the last successful gold fetch; 0 = still on the bundled estimate. */
    val spotFetchedAt: Long = 0,
    val silverUsdPerOz: Double = Metal.SILVER.fallbackSpotUsdPerOz,
    /** Epoch millis of the last successful silver fetch; 0 = still on the bundled estimate. */
    val silverFetchedAt: Long = 0,
    val unitsPerUsd: Map<String, Double> = emptyMap(),
    /** Epoch millis of the last successful rates fetch; 0 = still on the bundled estimates. */
    val fxFetchedAt: Long = 0,
    val lastAttemptAt: Long = 0,
    val lastError: String? = null,
    val spotOverride: Double? = null,
    val silverOverride: Double? = null,
    val fxOverrides: Map<String, Double> = emptyMap(),
) {
    fun liveSpot(metal: Metal): Double = when (metal) {
        Metal.GOLD -> spotUsdPerOz
        Metal.SILVER -> silverUsdPerOz
    }

    fun fetchedAt(metal: Metal): Long = when (metal) {
        Metal.GOLD -> spotFetchedAt
        Metal.SILVER -> silverFetchedAt
    }

    fun overrideFor(metal: Metal): Double? = when (metal) {
        Metal.GOLD -> spotOverride
        Metal.SILVER -> silverOverride
    }

    /** The spot the calculator uses: the person's own number if they typed one, else the saved live one. */
    fun spot(metal: Metal): Double = overrideFor(metal) ?: liveSpot(metal)

    fun liveRate(code: String): Double =
        if (code == "USD") 1.0 else unitsPerUsd[code] ?: Catalog.currency(code).fallbackPerUsd

    fun effectiveRate(code: String): Double =
        if (code == "USD") 1.0 else fxOverrides[code] ?: liveRate(code)
}

data class SavedInputs(
    val metal: String = Metal.GOLD.name,
    val currency: String = "USD",
    val grams: String = "10",
    /** Index into the selected metal's purities. */
    val purityIndex: Int = Metal.GOLD.defaultPurityIndex,
    val price: String = "1500",
    /** True when [price] is the store's price per gram rather than the total for the piece. */
    val pricePerGram: Boolean = false,
    val convertFrom: String = "USD",
    val convertAmount: String = "1",
    val gemId: String = "diamond",
    val gemPrice: String = "1000",
    val gemCurrency: String = "USD",
    val gemCarats: String = "1",
)
