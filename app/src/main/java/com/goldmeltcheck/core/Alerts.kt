package com.goldmeltcheck.core

enum class AlertType(val label: String) {
    BELOW("Price drops below"),
    ABOVE("Price rises above"),
    NEW_LOW("New N-week low"),
    NEW_HIGH("New N-week high"),
    DAILY_MOVE("Moves more than X% in a day"),
}

/**
 * One alert the person set up. [value] means: a USD/oz price for BELOW/ABOVE, a number of weeks
 * for NEW_LOW/NEW_HIGH, and a percent for DAILY_MOVE.
 */
data class AlertRule(
    val id: String,
    val metal: Metal,
    val type: AlertType,
    val value: Double,
    val enabled: Boolean = true,
) {
    fun describe(): String = when (type) {
        AlertType.BELOW -> "${metal.label} below ${usd(value)}/oz"
        AlertType.ABOVE -> "${metal.label} above ${usd(value)}/oz"
        AlertType.NEW_LOW -> "${metal.label} hits a new ${weeks(value)} low"
        AlertType.NEW_HIGH -> "${metal.label} hits a new ${weeks(value)} high"
        AlertType.DAILY_MOVE -> "${metal.label} moves more than ${trim(value)}% in a day"
    }
}

data class AlertHit(val rule: AlertRule, val title: String, val body: String)

object Alerts {
    /** A rule that fired stays quiet this long, so one move doesn't notify every hour. */
    const val COOLDOWN_MS = 20 * HOUR_MS

    val defaults = listOf(
        AlertRule("default-gold-low", Metal.GOLD, AlertType.NEW_LOW, 4.0),
        AlertRule("default-silver-low", Metal.SILVER, AlertType.NEW_LOW, 4.0),
    )

    /**
     * Checks [rule] against [history] for its metal, whose last point is the price just fetched.
     * @param lastFiredAt when this rule last notified, or null if never
     */
    fun evaluate(rule: AlertRule, history: List<PricePoint>, lastFiredAt: Long?): AlertHit? {
        if (!rule.enabled || rule.value <= 0) return null
        val latest = history.lastOrNull() ?: return null
        if (lastFiredAt != null && latest.at - lastFiredAt < COOLDOWN_MS) return null
        val previous = history.getOrNull(history.lastIndex - 1)
        val now = latest.usdPerOz
        val name = rule.metal.label

        return when (rule.type) {
            // Fire on the crossing, not on every check while the price stays past the target.
            AlertType.BELOW -> if (now < rule.value && (previous == null || previous.usdPerOz >= rule.value))
                AlertHit(rule, "$name dropped below ${usd(rule.value)}", "$name is at ${usd(now)}/oz.") else null

            AlertType.ABOVE -> if (now > rule.value && (previous == null || previous.usdPerOz <= rule.value))
                AlertHit(rule, "$name rose above ${usd(rule.value)}", "$name is at ${usd(now)}/oz.") else null

            AlertType.NEW_LOW, AlertType.NEW_HIGH -> {
                val window = (rule.value * WEEK_MS).toLong()
                if (!History.covers(history, window)) return null
                val prior = History.since(history.dropLast(1), latest.at - window)
                if (prior.isEmpty()) return null
                val low = rule.type == AlertType.NEW_LOW
                val beaten = if (low) now < prior.minOf { it.usdPerOz } else now > prior.maxOf { it.usdPerOz }
                if (!beaten) null else AlertHit(
                    rule,
                    "$name hit a ${weeks(rule.value)} ${if (low) "low" else "high"}",
                    "$name is at ${usd(now)}/oz, its ${if (low) "lowest" else "highest"} price in " +
                        "${weeks(rule.value)}.",
                )
            }

            AlertType.DAILY_MOVE -> {
                val change = History.changePercent(history, DAY_MS) ?: return null
                if (kotlin.math.abs(change) <= rule.value) null else AlertHit(
                    rule,
                    "$name ${if (change > 0) "up" else "down"} ${trim(kotlin.math.abs(change), 1)}% in a day",
                    "$name is at ${usd(now)}/oz.",
                )
            }
        }
    }
}

private fun usd(v: Double): String =
    "$" + if (v >= 100) String.format(java.util.Locale.US, "%,.0f", v) else String.format(java.util.Locale.US, "%,.2f", v)

private fun weeks(v: Double): String = trim(v) + "-week"

private fun trim(v: Double, decimals: Int = 2): String =
    java.math.BigDecimal(v).setScale(decimals, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
