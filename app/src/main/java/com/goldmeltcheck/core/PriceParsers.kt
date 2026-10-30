package com.goldmeltcheck.core

import org.json.JSONObject

object PriceParsers {
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
