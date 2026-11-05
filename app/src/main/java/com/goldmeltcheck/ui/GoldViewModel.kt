package com.goldmeltcheck.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goldmeltcheck.core.Catalog
import com.goldmeltcheck.core.MeltMath
import com.goldmeltcheck.core.MeltResult
import com.goldmeltcheck.core.AlertRule
import com.goldmeltcheck.core.Alerts
import com.goldmeltcheck.core.Metal
import com.goldmeltcheck.core.PricePoint
import com.goldmeltcheck.core.PriceRange
import com.goldmeltcheck.data.PriceSnapshot
import com.goldmeltcheck.data.PriceStore
import com.goldmeltcheck.data.PriceSync
import com.goldmeltcheck.data.SavedInputs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoldViewModel(app: Application) : AndroidViewModel(app) {
    private val store = PriceStore(app)

    val prices: StateFlow<PriceSnapshot> =
        store.snapshot.stateIn(viewModelScope, SharingStarted.Eagerly, PriceSnapshot())

    val refreshing: StateFlow<Boolean> =
        PriceSync.manualRefreshPending(app).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val history: StateFlow<Map<Metal, List<PricePoint>>> =
        store.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val alertRules: StateFlow<List<AlertRule>> =
        store.alertRules.stateIn(viewModelScope, SharingStarted.Eagerly, Alerts.defaults)

    fun saveAlertRules(rules: List<AlertRule>) = viewModelScope.launch { store.saveAlertRules(rules) }

    val gemRangeOverrides: StateFlow<Map<String, PriceRange>> =
        store.gemRangeOverrides.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setGemRange(key: String, range: PriceRange?) = viewModelScope.launch { store.setGemRange(key, range) }

    var inputs by mutableStateOf(SavedInputs())
        private set

    init {
        viewModelScope.launch { inputs = store.inputs.first() }
    }

    fun update(transform: (SavedInputs) -> SavedInputs) {
        inputs = transform(inputs)
        val snapshot = inputs
        viewModelScope.launch { store.saveInputs(snapshot) }
    }

    private val grams: Double? get() = MeltMath.parseAmount(inputs.grams, phoneDecimal())

    /** What the piece costs in total in the quoted currency, whether the store quoted a total or per gram. */
    val totalPrice: Double?
        get() = MeltMath.totalPrice(MeltMath.parseAmount(inputs.price, phoneDecimal()), grams, inputs.pricePerGram)

    /** Melt value in USD as soon as a weight is entered, before any price. */
    fun meltUsd(p: PriceSnapshot): Double? =
        grams?.takeIf { it > 0 }?.let { MeltMath.meltValueUsd(it, metal.purity(inputs.purityIndex).fineness, p.spot(metal)) }

    fun result(p: PriceSnapshot): MeltResult? = MeltMath.evaluate(
        grams = grams,
        purity = metal.purity(inputs.purityIndex).fineness,
        price = totalPrice,
        unitsPerUsd = p.effectiveRate(inputs.currency),
        spotUsdPerOz = p.spot(metal),
        metal = metal,
    )

    val metal: Metal get() = Metal.from(inputs.metal)

    /** Switching metal resets purity, since each metal has its own list. */
    fun selectMetal(m: Metal) = update {
        if (it.metal == m.name) it else it.copy(metal = m.name, purityIndex = m.defaultPurityIndex)
    }

    /** A typed value equal to the live one is not an override. */
    fun setSpotOverride(value: Double?) = viewModelScope.launch {
        val m = metal
        store.setSpotOverride(m, value?.takeIf { it > 0 && it != prices.value.liveSpot(m) })
    }

    fun setRateOverride(value: Double?) = viewModelScope.launch {
        val code = inputs.currency
        store.setRateOverride(code, value?.takeIf { it > 0 && it != prices.value.liveRate(code) })
    }

    fun clearRateOverride() = viewModelScope.launch { store.setRateOverride(inputs.currency, null) }

    fun refreshNow() = PriceSync.refreshNow(getApplication())
}
