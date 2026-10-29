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

    fun result(p: PriceSnapshot): MeltResult? = MeltMath.evaluate(
        grams = MeltMath.parseAmount(inputs.grams, phoneDecimal()),
        purity = Catalog.purities.getOrElse(inputs.purityIndex) { Catalog.purities[Catalog.DEFAULT_PURITY_INDEX] }.fineness,
        price = MeltMath.parseAmount(inputs.price, phoneDecimal()),
        unitsPerUsd = p.effectiveRate(inputs.currency),
        spotUsdPerOz = p.effectiveSpot,
    )

    /** A typed value equal to the live one is not an override. */
    fun setSpotOverride(value: Double?) = viewModelScope.launch {
        store.setSpotOverride(value?.takeIf { it > 0 && it != prices.value.spotUsdPerOz })
    }

    fun setRateOverride(value: Double?) = viewModelScope.launch {
        val code = inputs.currency
        store.setRateOverride(code, value?.takeIf { it > 0 && it != prices.value.liveRate(code) })
    }

    fun clearRateOverride() = viewModelScope.launch { store.setRateOverride(inputs.currency, null) }

    fun refreshNow() = PriceSync.refreshNow(getApplication())
}
