package com.goldmeltcheck.data

import android.content.Context
import android.util.Log
import com.goldmeltcheck.core.Metal
import com.goldmeltcheck.core.PriceParsers

/**
 * A year of daily gold and silver prices bundled into the APK at release time (assets/history_XAU.json,
 * history_XAG.json; see release.yml). Loaded into the saved history on launch so trends and alerts work
 * from the first day. Builds without the files (CI, or a release with no API key) simply skip this.
 */
object HistorySeed {
    suspend fun load(context: Context, store: PriceStore) {
        val now = System.currentTimeMillis()
        for (metal in Metal.entries) {
            val body = runCatching {
                context.assets.open("history_${metal.symbol}.json").bufferedReader().use { it.readText() }
            }.getOrNull() ?: continue
            val seed = runCatching { PriceParsers.dailyHistory(body) }
                .onFailure { Log.w("HistorySeed", "Bundled ${metal.label} history unreadable", it) }
                .getOrNull() ?: continue
            if (seed.isNotEmpty()) store.seedHistory(metal, seed, now)
        }
    }
}
