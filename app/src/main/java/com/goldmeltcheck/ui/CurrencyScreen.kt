package com.goldmeltcheck.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goldmeltcheck.core.Catalog
import com.goldmeltcheck.core.MeltMath
import com.goldmeltcheck.data.PriceSnapshot
import kotlinx.coroutines.delay
import java.text.NumberFormat

/** Pick a currency and an amount; see what it is worth in every other currency. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencyScreen(vm: GoldViewModel, contentPadding: PaddingValues) {
    val p by vm.prices.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(60_000) }
    }
    val inputs = vm.inputs
    val from = Catalog.currency(inputs.convertFrom)
    val amount = MeltMath.parseAmount(inputs.convertAmount, phoneDecimal())

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Currency Rates", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Pick a currency to see what it's worth in every other one. Tap a row to convert from that currency.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Panel {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = "Amount",
                        value = inputs.convertAmount,
                        onValueChange = { v -> vm.update { it.copy(convertAmount = v) } },
                        supporting = readBack(inputs.convertAmount, from.code, 2),
                        modifier = Modifier.weight(1f),
                    )
                    Picker(
                        label = "Currency",
                        options = Catalog.currencies.map { "${it.code} · ${it.name}" },
                        selected = Catalog.currencies.indexOf(from),
                        onSelect = { i -> vm.update { it.copy(convertFrom = Catalog.currencies[i].code) } },
                        modifier = Modifier.weight(1.4f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Status("Exchange rates", fetchedAt = p.fxFetchedAt, overridden = false, now = now)
                    }
                    TextButton(onClick = vm::refreshNow, enabled = !refreshing) {
                        Text(if (refreshing) "Refreshing…" else "Refresh now")
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
                if (amount == null || amount < 0) {
                    Text("Enter an amount to convert.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ConversionTable(p, from.code, amount, onPick = { code ->
                        vm.update { it.copy(convertFrom = code) }
                    })
                }
            }

            Text(
                "Every conversion goes through the US dollar rate saved on this phone. Rates marked est are " +
                    "bundled estimates, not yet refreshed; yours are numbers you typed on the Check tab. " +
                    "Rates by Exchange Rate API (exchangerate-api.com).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConversionTable(p: PriceSnapshot, fromCode: String, amount: Double, onPick: (String) -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val fromRate = p.effectiveRate(fromCode)
    Column {
        Text(
            "${money(amount)} $fromCode is worth",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            HeaderCell("Currency", Modifier.weight(1f), TextAlign.Start)
            HeaderCell("Amount", Modifier.weight(1.2f), TextAlign.End)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Catalog.currencies.filter { it.code != fromCode }.forEach { c ->
            val toRate = p.effectiveRate(c.code)
            val note = when {
                p.fxOverrides.containsKey(c.code) || p.fxOverrides.containsKey(fromCode) -> "yours"
                isEstimate(p, c.code) || isEstimate(p, fromCode) -> "est"
                else -> null
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(c.code) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(c.code, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                    Text(c.name, style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 1)
                }
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                    Text(
                        money(MeltMath.convert(amount, fromRate, toRate)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp,
                    )
                    note?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (it == "est") LocalVerdictColors.current.warn else muted,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

private fun isEstimate(p: PriceSnapshot, code: String): Boolean =
    code != "USD" && !p.fxOverrides.containsKey(code) && (p.fxFetchedAt == 0L || !p.unitsPerUsd.containsKey(code))

/** Money in the phone's format: two decimals, or four for amounts under 1 (0.0038 USD per VND). */
private fun money(v: Double): String = NumberFormat.getNumberInstance().apply {
    val decimals = if (v != 0.0 && kotlin.math.abs(v) < 1) 4 else 2
    minimumFractionDigits = decimals
    maximumFractionDigits = decimals
}.format(v)
