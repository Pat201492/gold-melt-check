package com.goldmeltcheck.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.goldmeltcheck.core.PriceRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "gold_melt_check")

/** On-device cache of prices, manual overrides, and the last inputs. Works fully offline. */
class PriceStore(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val spot = doublePreferencesKey("spot_usd_per_oz")
        val spotFetchedAt = longPreferencesKey("spot_fetched_at")
        val fx = stringPreferencesKey("fx_units_per_usd")
        val fxFetchedAt = longPreferencesKey("fx_fetched_at")
        val lastAttemptAt = longPreferencesKey("last_attempt_at")
        val lastError = stringPreferencesKey("last_error")
        val spotOverride = doublePreferencesKey("spot_override")
        val fxOverrides = stringPreferencesKey("fx_overrides")
        val inCurrency = stringPreferencesKey("input_currency")
        val inGrams = stringPreferencesKey("input_grams")
        val inPurity = intPreferencesKey("input_purity")
        val inPrice = stringPreferencesKey("input_price")
        val convertFrom = stringPreferencesKey("convert_from")
        val convertAmount = stringPreferencesKey("convert_amount")
        val gemId = stringPreferencesKey("gem_id")
        val gemPrice = stringPreferencesKey("gem_price")
        val gemCurrency = stringPreferencesKey("gem_currency")
        val gemCarats = stringPreferencesKey("gem_carats")
        val gemRanges = stringPreferencesKey("gem_range_overrides")
    }

    val snapshot: Flow<PriceSnapshot> = store.data.map { p ->
        val defaults = PriceSnapshot()
        PriceSnapshot(
            spotUsdPerOz = p[Keys.spot] ?: defaults.spotUsdPerOz,
            spotFetchedAt = p[Keys.spotFetchedAt] ?: 0,
            unitsPerUsd = decode(p[Keys.fx]),
            fxFetchedAt = p[Keys.fxFetchedAt] ?: 0,
            lastAttemptAt = p[Keys.lastAttemptAt] ?: 0,
            lastError = p[Keys.lastError],
            spotOverride = p[Keys.spotOverride],
            fxOverrides = decode(p[Keys.fxOverrides]),
        )
    }

    val inputs: Flow<SavedInputs> = store.data.map { p ->
        val d = SavedInputs()
        SavedInputs(
            currency = p[Keys.inCurrency] ?: d.currency,
            grams = p[Keys.inGrams] ?: d.grams,
            purityIndex = p[Keys.inPurity] ?: d.purityIndex,
            price = p[Keys.inPrice] ?: d.price,
            convertFrom = p[Keys.convertFrom] ?: d.convertFrom,
            convertAmount = p[Keys.convertAmount] ?: d.convertAmount,
            gemId = p[Keys.gemId] ?: d.gemId,
            gemPrice = p[Keys.gemPrice] ?: d.gemPrice,
            gemCurrency = p[Keys.gemCurrency] ?: d.gemCurrency,
            gemCarats = p[Keys.gemCarats] ?: d.gemCarats,
        )
    }

    suspend fun saveSpot(usdPerOz: Double, at: Long) = store.edit {
        it[Keys.spot] = usdPerOz
        it[Keys.spotFetchedAt] = at
    }

    suspend fun saveRates(unitsPerUsd: Map<String, Double>, at: Long) = store.edit {
        it[Keys.fx] = encode(unitsPerUsd)
        it[Keys.fxFetchedAt] = at
    }

    suspend fun recordAttempt(at: Long, error: String?) = store.edit {
        it[Keys.lastAttemptAt] = at
        if (error == null) it.remove(Keys.lastError) else it[Keys.lastError] = error
    }

    suspend fun setSpotOverride(value: Double?) = store.edit {
        if (value == null) it.remove(Keys.spotOverride) else it[Keys.spotOverride] = value
    }

    suspend fun setRateOverride(code: String, value: Double?) = store.edit { p ->
        val current = decode(p[Keys.fxOverrides]).toMutableMap()
        if (value == null) current.remove(code) else current[code] = value
        p[Keys.fxOverrides] = encode(current)
    }

    suspend fun saveInputs(inputs: SavedInputs) = store.edit {
        it[Keys.inCurrency] = inputs.currency
        it[Keys.inGrams] = inputs.grams
        it[Keys.inPurity] = inputs.purityIndex
        it[Keys.inPrice] = inputs.price
        it[Keys.convertFrom] = inputs.convertFrom
        it[Keys.convertAmount] = inputs.convertAmount
        it[Keys.gemId] = inputs.gemId
        it[Keys.gemPrice] = inputs.gemPrice
        it[Keys.gemCurrency] = inputs.gemCurrency
        it[Keys.gemCarats] = inputs.gemCarats
    }

    /** Price ranges the person edited, keyed "gemId:natural" or "gemId:lab". */
    val gemRangeOverrides: Flow<Map<String, PriceRange>> = store.data.map { p ->
        decodeRanges(p[Keys.gemRanges])
    }

    suspend fun setGemRange(key: String, range: PriceRange?) = store.edit { p ->
        val current = decodeRanges(p[Keys.gemRanges]).toMutableMap()
        if (range == null) current.remove(key) else current[key] = range
        val json = JSONObject()
        current.forEach { (k, r) -> json.put(k, org.json.JSONArray(listOf(r.low, r.high))) }
        p[Keys.gemRanges] = json.toString()
    }

    private fun decodeRanges(raw: String?): Map<String, PriceRange> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { k ->
                val a = json.getJSONArray(k)
                PriceRange(a.getDouble(0), a.getDouble(1))
            }
        }.getOrDefault(emptyMap())
    }

    private fun encode(map: Map<String, Double>): String = JSONObject(map as Map<*, *>).toString()

    private fun decode(raw: String?): Map<String, Double> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.getDouble(it) }
        }.getOrDefault(emptyMap())
    }
}

