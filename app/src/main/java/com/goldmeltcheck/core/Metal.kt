package com.goldmeltcheck.core

/**
 * A precious metal the calculator can price. Everything metal-specific lives here so gold and
 * silver share the same math, storage and screens.
 */
enum class Metal(
    val label: String,
    /** Symbol in the spot-price API: gold-api.com/price/<symbol>. */
    val symbol: String,
    /** Rough spot estimate, USD per troy oz, used only until the first successful refresh. */
    val fallbackSpotUsdPerOz: Double,
    val purities: List<Purity>,
    val defaultPurityIndex: Int,
    /** Highest percent of melt still counted as "close to melt". */
    val nearMeltMax: Double,
    /** Highest percent of melt counted as a "moderate" markup; above is "high". */
    val moderateMax: Double,
    /** One line on what a normal premium over melt looks like for this metal. */
    val premiumNote: String,
) {
    GOLD(
        label = "Gold",
        symbol = "XAU",
        fallbackSpotUsdPerOz = 4350.0, // late Sept 2026
        purities = listOf(
            Purity("24k · .9999", 0.9999),
            Purity("24k · .999", 0.999),
            Purity("22k · .916", 0.9167),
            Purity("21k · .875", 0.875),
            Purity("18k · .750", 0.75),
            Purity("14k · .585", 0.585),
            Purity("10k · .417", 0.417),
            Purity("Coin · .900", 0.9),
        ),
        defaultPurityIndex = 1,
        nearMeltMax = 108.0,
        moderateMax = 125.0,
        premiumNote = "Gold bullion bars and coins commonly sell a few percent over melt; jewelry much more.",
    ),
    SILVER(
        label = "Silver",
        symbol = "XAG",
        fallbackSpotUsdPerOz = 62.0, // mid Oct 2026
        purities = listOf(
            Purity("Fine · .9999", 0.9999),
            Purity("Fine · .999", 0.999),
            Purity("Britannia · .958", 0.958),
            Purity("Sterling · .925", 0.925),
            Purity("Coin · .900", 0.9),
            Purity(".800", 0.8),
        ),
        defaultPurityIndex = 1,
        nearMeltMax = 120.0,
        moderateMax = 150.0,
        premiumNote = "Silver carries bigger premiums than gold: bullion often sells 10–30% over melt, and small " +
            "coins and jewelry more.",
    );

    fun purity(index: Int): Purity = purities.getOrElse(index) { purities[defaultPurityIndex] }

    companion object {
        fun from(name: String?): Metal = entries.firstOrNull { it.name == name } ?: GOLD
    }
}
