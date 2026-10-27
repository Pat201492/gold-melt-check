package com.goldmeltcheck.data

import com.goldmeltcheck.core.PriceParsers
import java.net.HttpURLConnection
import java.net.URL

/** Free, keyless price sources. Blocking: call from a background thread. */
object PriceApi {
    const val GOLD_URL = "https://api.gold-api.com/price/XAU"
    const val RATES_URL = "https://open.er-api.com/v6/latest/USD"

    fun fetchGoldUsdPerOz(): Double = PriceParsers.goldUsdPerOz(get(GOLD_URL))

    fun fetchUnitsPerUsd(): Map<String, Double> = PriceParsers.unitsPerUsd(get(RATES_URL))

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "GoldMeltCheck/1.0 (Android)")
            val code = conn.responseCode
            check(code == HttpURLConnection.HTTP_OK) { "HTTP $code from ${URL(url).host}" }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
