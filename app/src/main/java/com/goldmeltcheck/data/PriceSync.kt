package com.goldmeltcheck.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Fetches gold spot and exchange rates and saves them on the phone. */
class PriceSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val store = PriceStore(applicationContext)
        val now = System.currentTimeMillis()
        val errors = mutableListOf<String>()

        runCatching { PriceApi.fetchGoldUsdPerOz() }
            .onSuccess { store.saveSpot(it, now) }
            .onFailure { errors += "Gold price: ${it.message ?: it.javaClass.simpleName}" }
        runCatching { PriceApi.fetchUnitsPerUsd() }
            .onSuccess { store.saveRates(it, now) }
            .onFailure { errors += "Exchange rates: ${it.message ?: it.javaClass.simpleName}" }

        store.recordAttempt(now, errors.takeIf { it.isNotEmpty() }?.joinToString("\n"))
        when {
            errors.isEmpty() -> Result.success()
            runAttemptCount < 2 -> Result.retry()
            else -> Result.failure()
        }
    }
}

object PriceSync {
    private const val PERIODIC = "price-sync-wifi"
    private const val ON_OPEN = "price-sync-on-open"
    private const val MANUAL = "price-sync-now"

    private val wifiOnly = Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()
    private val anyNetwork = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Background refresh every 6 hours, only on WiFi (unmetered) so it never uses mobile data. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PriceSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(wifiOnly)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Opening the app refreshes too, but still waits for WiFi. */
    fun refreshOnWifi(context: Context) {
        val request = OneTimeWorkRequestBuilder<PriceSyncWorker>().setConstraints(wifiOnly).build()
        WorkManager.getInstance(context).enqueueUniqueWork(ON_OPEN, ExistingWorkPolicy.KEEP, request)
    }

    /** The "Refresh now" button: the person asked, so any connection is fine. */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<PriceSyncWorker>().setConstraints(anyNetwork).build()
        WorkManager.getInstance(context).enqueueUniqueWork(MANUAL, ExistingWorkPolicy.REPLACE, request)
    }

    /** True while a manual refresh is queued or running. */
    fun manualRefreshPending(context: Context): Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(MANUAL).map { infos ->
            infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
}
