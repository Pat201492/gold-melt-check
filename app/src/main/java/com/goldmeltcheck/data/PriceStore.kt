package com.goldmeltcheck.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

