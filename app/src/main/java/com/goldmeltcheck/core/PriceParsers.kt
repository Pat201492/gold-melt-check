package com.goldmeltcheck.core

import org.json.JSONArray
import org.json.JSONObject

object PriceParsers {
    /**
     * gold-api.com `/history?groupBy=day`: `[{"day":"2026-07-01","avg_price":4190.2}, ...]` (the price field is
     * named after the aggregation: avg_price, max_price or min_price). Each day becomes a point at 12:00 UTC.
     * Malformed rows are skipped; the result is oldest first.
     */
    fun dailyHistory(body: String): List<PricePoint> {
        val rows = JSONArray(body)
        val out = mutableListOf<PricePoint>()
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val day = row.optString("day").takeIf { it.length == 10 } ?: continue
            val price = listOf("avg_price", "max_price", "min_price", "price")
                .map { row.optDouble(it, Double.NaN) }
                .firstOrNull { it.isFinite() && it > 0 } ?: continue
            val at = runCatching { java.time.LocalDate.parse(day).toEpochDay() * DAY_MS + 12 * HOUR_MS }.getOrNull()
                ?: continue
            out += PricePoint(at, price)
        }
        return out.sortedBy { it.at }
    }

    /** gold-api.com `/price/XAU` or `/price/XAG`: `{"name":"Gold","price":4350.1,"symbol":"XAU",...}` */
    fun spotUsdPerOz(body: String): Double {
        val json = JSONObject(body)
        val price = json.optDouble("price", Double.NaN)
        require(price.isFinite() && price > 0) { "response had no price" }
        return price
    }

    /** open.er-api.com `/v6/latest/USD`: `{"result":"success","base_code":"USD","rates":{"EUR":0.86,...}}` */
    fun unitsPerUsd(body: String): Map<String, Double> {
        val json = JSONObject(body)
        require(json.optString("result") == "success") { "rate service said: ${json.optString("error-type", "error")}" }
        require(json.optString("base_code", "USD") == "USD") { "rates were not quoted against USD" }
        val rates = json.getJSONObject("rates")
        val out = mutableMapOf<String, Double>()
        for (code in rates.keys()) {
            val v = rates.optDouble(code, Double.NaN)
            if (v.isFinite() && v > 0) out[code] = v
        }
        require(out.isNotEmpty()) { "response had no rates" }
        return out
    }
}
