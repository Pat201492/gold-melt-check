package com.goldmeltcheck

import android.app.Application
import com.goldmeltcheck.data.PriceSync

class GoldMeltCheckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PriceSync.schedule(this)
        PriceSync.scheduleHourlySpot(this)
        PriceSync.refreshOnWifi(this)
    }
}
