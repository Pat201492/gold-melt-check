package com.goldmeltcheck.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goldmeltcheck.core.Catalog
import com.goldmeltcheck.core.GRAMS_PER_TROY_OUNCE
import com.goldmeltcheck.core.MeltMath
import com.goldmeltcheck.core.Metal
import com.goldmeltcheck.data.PriceSnapshot
import kotlinx.coroutines.delay
import java.text.NumberFormat

/** Every currency at once: its rate against the dollar and what a gram of pure gold costs in it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatesScreen(vm: GoldViewModel, contentPadding: PaddingValues, onPick: (String) -> Unit) {
    val p by vm.prices.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(60_000) }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Metal Rates", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Gold, silver and exchange rates saved on this phone. Tap a currency to use it in the calculator.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Panel {
                Eyebrow("Spot prices")
                Metal.entries.forEach { m ->
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            Text(m.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(usd(p.spot(m)) + " /oz", fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Status(m.label, fetchedAt = p.fetchedAt(m), overridden = p.overrideFor(m) != null, now = now)
                            }
                            Text(usd(p.spot(m) / GRAMS_PER_TROY_OUNCE) + " /g", fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Status("Exchange rates", fetchedAt = p.fxFetchedAt, overridden = false, now = now)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = vm::refreshNow, enabled = !refreshing) {
                        if (refreshing) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Waiting for connection…")
                        } else {
                            Text("Refresh now")
                        }
                    }
                }
                p.lastError?.let { err ->
                    Text(
                        "Last refresh ${relative(p.lastAttemptAt, now)} failed:\n$err",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalVerdictColors.current.bad,
                    )
                }
            }

            Panel {
                RatesTable(p, selected = vm.inputs.currency, onPick = onPick)
            }

            Text(
                "Per-gram prices are for the pure metal (24k gold, .999 fine silver). Rates marked est are " +
                    "bundled estimates, not yet refreshed. Gold and silver prices from gold-api.com. Rates by " +
                    "Exchange Rate API (exchangerate-api.com).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun Status(label: String, fetchedAt: Long, overridden: Boolean, now: Long) {
    val colors = LocalVerdictColors.current
    val (text, tone) = when {
        overridden -> "$label: your number" to MaterialTheme.colorScheme.onSurfaceVariant
        fetchedAt == 0L -> "$label: starting estimate, not live yet" to colors.warn
        now - fetchedAt > STALE_AFTER_MS -> "$label: updated ${relative(fetchedAt, now)}" to colors.warn
        else -> "$label: live · updated ${relative(fetchedAt, now)}" to colors.good
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = tone)
}

@Composable
private fun RatesTable(p: PriceSnapshot, selected: String, onPick: (String) -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            HeaderCell("Currency", Modifier.weight(1.05f), TextAlign.Start)
            HeaderCell("1 USD =", Modifier.weight(1f), TextAlign.End)
            HeaderCell("Gold / g", Modifier.weight(1f), TextAlign.End)
            HeaderCell("Silver / g", Modifier.weight(0.95f), TextAlign.End)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Catalog.currencies.forEach { c ->
            val rate = p.effectiveRate(c.code)
            val estimate = c.code != "USD" && !p.fxOverrides.containsKey(c.code) &&
                (p.fxFetchedAt == 0L || !p.unitsPerUsd.containsKey(c.code))
            val isSelected = c.code == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(c.code) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1.05f)) {
                    Text(
                        c.code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    )
                    Text(c.name, style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 1)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(amount(rate), fontFamily = FontFamily.Monospace)
                    when {
                        p.fxOverrides.containsKey(c.code) ->
                            Text("yours", style = MaterialTheme.typography.labelSmall, color = muted)
                        estimate ->
                            Text("est", style = MaterialTheme.typography.labelSmall, color = LocalVerdictColors.current.warn)
                    }
                }
                Text(
                    amount(MeltMath.pricePerGram(p.spot(Metal.GOLD), rate)),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    amount(MeltMath.pricePerGram(p.spot(Metal.SILVER), rate)),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.95f),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
internal fun HeaderCell(text: String, modifier: Modifier, align: TextAlign) = Text(
    text.uppercase(),
    modifier = modifier,
    textAlign = align,
    style = MaterialTheme.typography.labelSmall,
    letterSpacing = 1.sp,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** Phone-formatted number with precision that suits its size: 26,300 / 151.20 / 0.8610. */
private fun amount(v: Double): String {
    val decimals = when {
        v >= 1000 -> 0
        v >= 1 -> 2
        else -> 4
    }
    return NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(v)
}
