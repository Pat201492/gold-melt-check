package com.goldmeltcheck.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.goldmeltcheck.core.AlertRule
import com.goldmeltcheck.core.AlertType
import com.goldmeltcheck.core.Alerts
import com.goldmeltcheck.core.History
import com.goldmeltcheck.core.Metal
import com.goldmeltcheck.core.PricePoint
import com.goldmeltcheck.core.PriceRange
import org.json.JSONArray
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
        val silver = doublePreferencesKey("silver_usd_per_oz")
        val silverFetchedAt = longPreferencesKey("silver_fetched_at")
        val silverOverride = doublePreferencesKey("silver_override")
        val inMetal = stringPreferencesKey("input_metal")
        val fxOverrides = stringPreferencesKey("fx_overrides")
        val inCurrency = stringPreferencesKey("input_currency")
        val inGrams = stringPreferencesKey("input_grams")
        val inPurity = intPreferencesKey("input_purity")
        val inPrice = stringPreferencesKey("input_price")
        val inPricePerGram = booleanPreferencesKey("input_price_per_gram")
        val convertFrom = stringPreferencesKey("convert_from")
        val convertAmount = stringPreferencesKey("convert_amount")
        val gemId = stringPreferencesKey("gem_id")
        val gemPrice = stringPreferencesKey("gem_price")
        val gemCurrency = stringPreferencesKey("gem_currency")
        val gemCarats = stringPreferencesKey("gem_carats")
        val gemHeat = stringPreferencesKey("gem_heat")
        val gemRanges = stringPreferencesKey("gem_range_overrides")
        fun history(metal: Metal) = stringPreferencesKey("history_${metal.name}")
        val alertRules = stringPreferencesKey("alert_rules")
        val alertFired = stringPreferencesKey("alert_last_fired")
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
            silverUsdPerOz = p[Keys.silver] ?: defaults.silverUsdPerOz,
            silverFetchedAt = p[Keys.silverFetchedAt] ?: 0,
            silverOverride = p[Keys.silverOverride],
            fxOverrides = decode(p[Keys.fxOverrides]),
        )
    }

    val inputs: Flow<SavedInputs> = store.data.map { p ->
        val d = SavedInputs()
        SavedInputs(
            metal = p[Keys.inMetal] ?: d.metal,
            currency = p[Keys.inCurrency] ?: d.currency,
            grams = p[Keys.inGrams] ?: d.grams,
            purityIndex = p[Keys.inPurity] ?: d.purityIndex,
            price = p[Keys.inPrice] ?: d.price,
            pricePerGram = p[Keys.inPricePerGram] ?: d.pricePerGram,
            convertFrom = p[Keys.convertFrom] ?: d.convertFrom,
            convertAmount = p[Keys.convertAmount] ?: d.convertAmount,
            gemId = p[Keys.gemId] ?: d.gemId,
            gemPrice = p[Keys.gemPrice] ?: d.gemPrice,
            gemCurrency = p[Keys.gemCurrency] ?: d.gemCurrency,
            gemCarats = p[Keys.gemCarats] ?: d.gemCarats,
            gemHeat = p[Keys.gemHeat] ?: d.gemHeat,
        )
    }

    suspend fun saveSpot(metal: Metal, usdPerOz: Double, at: Long) = store.edit {
        when (metal) {
            Metal.GOLD -> { it[Keys.spot] = usdPerOz; it[Keys.spotFetchedAt] = at }
            Metal.SILVER -> { it[Keys.silver] = usdPerOz; it[Keys.silverFetchedAt] = at }
        }
    }

    suspend fun saveRates(unitsPerUsd: Map<String, Double>, at: Long) = store.edit {
        it[Keys.fx] = encode(unitsPerUsd)
        it[Keys.fxFetchedAt] = at
    }

    suspend fun recordAttempt(at: Long, error: String?) = store.edit {
        it[Keys.lastAttemptAt] = at
        if (error == null) it.remove(Keys.lastError) else it[Keys.lastError] = error
    }

    suspend fun setSpotOverride(metal: Metal, value: Double?) = store.edit {
        val key = when (metal) {
            Metal.GOLD -> Keys.spotOverride
            Metal.SILVER -> Keys.silverOverride
        }
        if (value == null) it.remove(key) else it[key] = value
    }

    suspend fun setRateOverride(code: String, value: Double?) = store.edit { p ->
        val current = decode(p[Keys.fxOverrides]).toMutableMap()
        if (value == null) current.remove(code) else current[code] = value
        p[Keys.fxOverrides] = encode(current)
    }

    suspend fun saveInputs(inputs: SavedInputs) = store.edit {
        it[Keys.inMetal] = inputs.metal
        it[Keys.inCurrency] = inputs.currency
        it[Keys.inGrams] = inputs.grams
        it[Keys.inPurity] = inputs.purityIndex
        it[Keys.inPrice] = inputs.price
        it[Keys.inPricePerGram] = inputs.pricePerGram
        it[Keys.convertFrom] = inputs.convertFrom
        it[Keys.convertAmount] = inputs.convertAmount
        it[Keys.gemId] = inputs.gemId
        it[Keys.gemPrice] = inputs.gemPrice
        it[Keys.gemCurrency] = inputs.gemCurrency
        it[Keys.gemCarats] = inputs.gemCarats
        it[Keys.gemHeat] = inputs.gemHeat
    }

    /** Price ranges the person edited, keyed "gemId:H:bracket" (heated or any) or "gemId:U:bracket" (unheated). */
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

    /** Saved spot prices for each metal, oldest first. */
    val history: Flow<Map<Metal, List<PricePoint>>> = store.data.map { p ->
        Metal.entries.associateWith { decodeHistory(p[Keys.history(it)]) }
    }

    /** Adds a fetched price to the metal's history and returns the updated history. */
    suspend fun appendHistory(metal: Metal, point: PricePoint): List<PricePoint> {
        var updated = emptyList<PricePoint>()
        store.edit { p ->
            updated = History.append(decodeHistory(p[Keys.history(metal)]), point)
            p[Keys.history(metal)] = updated.joinToString(";") { "${it.at}:${it.usdPerOz}" }
        }
        return updated
    }

    /** Alert rules; a fresh install starts with a 4-week-low alert for each metal. */
    val alertRules: Flow<List<AlertRule>> = store.data.map { p ->
        p[Keys.alertRules]?.let(::decodeRules) ?: Alerts.defaults
    }

    suspend fun saveAlertRules(rules: List<AlertRule>) = store.edit { p ->
        val json = JSONArray()
        rules.forEach { r ->
            json.put(JSONObject().put("id", r.id).put("metal", r.metal.name).put("type", r.type.name)
                .put("value", r.value).put("enabled", r.enabled))
        }
        p[Keys.alertRules] = json.toString()
    }

    /** When each rule last notified, by rule id. */
    val alertLastFired: Flow<Map<String, Long>> = store.data.map { p ->
        decode(p[Keys.alertFired]).mapValues { it.value.toLong() }
    }

    suspend fun markAlertFired(id: String, at: Long) = store.edit { p ->
        val current = decode(p[Keys.alertFired]).toMutableMap()
        current[id] = at.toDouble()
        p[Keys.alertFired] = encode(current)
    }

    private fun decodeHistory(raw: String?): List<PricePoint> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(';').mapNotNull { part ->
            val i = part.indexOf(':')
            if (i < 0) return@mapNotNull null
            val at = part.substring(0, i).toLongOrNull()
            val price = part.substring(i + 1).toDoubleOrNull()
            if (at == null || price == null) null else PricePoint(at, price)
        }.sortedBy { it.at }
    }

    private fun decodeRules(raw: String): List<AlertRule> = runCatching {
        val json = JSONArray(raw)
        (0 until json.length()).mapNotNull { i ->
            val o = json.getJSONObject(i)
            val type = AlertType.entries.firstOrNull { it.name == o.optString("type") } ?: return@mapNotNull null
            AlertRule(
                id = o.getString("id"),
                metal = Metal.from(o.optString("metal")),
                type = type,
                value = o.getDouble("value"),
                enabled = o.optBoolean("enabled", true),
            )
        }
    }.getOrDefault(Alerts.defaults)

    private fun encode(map: Map<String, Double>): String = JSONObject(map as Map<*, *>).toString()

    private fun decode(raw: String?): Map<String, Double> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.getDouble(it) }
        }.getOrDefault(emptyMap())
    }
}

