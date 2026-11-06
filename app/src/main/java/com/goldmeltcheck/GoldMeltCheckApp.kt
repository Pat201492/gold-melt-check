package com.goldmeltcheck

import android.app.Application
import com.goldmeltcheck.data.HistorySeed
import com.goldmeltcheck.data.PriceStore
import com.goldmeltcheck.data.PriceSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GoldMeltCheckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            HistorySeed.load(this@GoldMeltCheckApp, PriceStore(this@GoldMeltCheckApp))
        }
        PriceSync.schedule(this)
        PriceSync.scheduleHourlySpot(this)
        PriceSync.refreshOnWifi(this)
    }
}
