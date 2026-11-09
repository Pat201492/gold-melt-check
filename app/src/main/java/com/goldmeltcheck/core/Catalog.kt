package com.goldmeltcheck.core

data class Currency(val code: String, val name: String, val fallbackPerUsd: Double)

data class Purity(val label: String, val fineness: Double)

object Catalog {
    /** Fallback rates are rough estimates, used only until the first successful refresh. */
    val currencies = listOf(
        Currency("USD", "US dollar", 1.0),
        Currency("EUR", "Euro", 0.86),
        Currency("GBP", "British pound", 0.75),
        Currency("CAD", "Canadian dollar", 1.38),
        Currency("AUD", "Australian dollar", 1.53),
        Currency("MXN", "Mexican peso", 18.5),
        Currency("INR", "Indian rupee", 88.0),
        Currency("AED", "UAE dirham", 3.6725),
        Currency("SAR", "Saudi riyal", 3.75),
        Currency("HKD", "Hong Kong dollar", 7.78),
        Currency("SGD", "Singapore dollar", 1.29),
        Currency("JPY", "Japanese yen", 150.0),
        Currency("CNY", "Chinese yuan", 7.12),
        Currency("CHF", "Swiss franc", 0.80),
        Currency("TRY", "Turkish lira", 42.0),
        Currency("THB", "Thai baht", 32.5),
        Currency("PHP", "Philippine peso", 58.0),
        Currency("VND", "Vietnamese dong", 26300.0),
        Currency("PKR", "Pakistani rupee", 281.0),
        Currency("ZAR", "South African rand", 17.4),
        Currency("LKR", "Sri Lankan rupee", 330.0),
    )

    fun currency(code: String): Currency = currencies.firstOrNull { it.code == code } ?: currencies.first()
}
