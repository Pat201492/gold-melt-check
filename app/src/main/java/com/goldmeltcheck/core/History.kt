package com.goldmeltcheck.core

/** One saved spot price: epoch millis and USD per troy oz. */
data class PricePoint(val at: Long, val usdPerOz: Double)

const val HOUR_MS = 60L * 60 * 1000
const val DAY_MS = 24 * HOUR_MS
const val WEEK_MS = 7 * DAY_MS

/** Price history kept on the phone, built from the app's own checks. Pure functions; lists are sorted by time. */
object History {
    /** Full detail is kept this long; older points thin to one per day (the last of the day). */
    const val DETAIL_MS = 14 * DAY_MS
    /** Nothing older than this is kept. */
    const val KEEP_MS = 2 * 365 * DAY_MS

    /** Adds [point] and thins old detail so storage stays small (about 1,100 points a metal at most). */
    fun append(points: List<PricePoint>, point: PricePoint): List<PricePoint> =
        compact((points.filter { it.at != point.at } + point).sortedBy { it.at }, now = point.at)

    fun compact(points: List<PricePoint>, now: Long): List<PricePoint> {
        val kept = points.filter { it.at >= now - KEEP_MS }
        val (old, recent) = kept.partition { it.at < now - DETAIL_MS }
        val dailyOld = old.groupBy { it.at / DAY_MS }.values.map { day -> day.maxBy { it.at } }
        return (dailyOld + recent).sortedBy { it.at }
    }

    /**
     * Adds bundled daily history to what the phone has saved. Only days before the earliest saved price are
     * added, so the app's own checks always win and running this again adds nothing.
     */
    fun mergeSeed(saved: List<PricePoint>, seed: List<PricePoint>, now: Long): List<PricePoint> {
        val firstSaved = saved.firstOrNull()?.at ?: Long.MAX_VALUE
        val older = seed.filter { it.at < firstSaved && it.at <= now }
        if (older.isEmpty()) return saved
        return compact((older + saved).sortedBy { it.at }, now)
    }

    /** Points from [from] (inclusive) to the end. */
    fun since(points: List<PricePoint>, from: Long): List<PricePoint> = points.filter { it.at >= from }

    /** The saved price closest to [target], if one lies within [tolerance] of it. */
    fun nearest(points: List<PricePoint>, target: Long, tolerance: Long): PricePoint? =
        points.minByOrNull { kotlin.math.abs(it.at - target) }
            ?.takeIf { kotlin.math.abs(it.at - target) <= tolerance }

    /** Percent change from about [ago] before the latest point, or null without history that far back. */
    fun changePercent(points: List<PricePoint>, ago: Long): Double? {
        val latest = points.lastOrNull() ?: return null
        val tolerance = maxOf(3 * HOUR_MS, ago / 7)
        val then = nearest(points.dropLast(1), latest.at - ago, tolerance) ?: return null
        return (latest.usdPerOz - then.usdPerOz) / then.usdPerOz * 100
    }

    /** True when the history reaches back at least [window] before the latest point (within a day). */
    fun covers(points: List<PricePoint>, window: Long): Boolean {
        val first = points.firstOrNull() ?: return false
        val last = points.last()
        return last.at - first.at >= window - DAY_MS
    }

    /** Lowest and highest price within [window] before the latest point, or null without enough history. */
    fun range(points: List<PricePoint>, window: Long): Pair<Double, Double>? {
        if (!covers(points, window)) return null
        val inWindow = since(points, points.last().at - window)
        return inWindow.minOf { it.usdPerOz } to inWindow.maxOf { it.usdPerOz }
    }
}
