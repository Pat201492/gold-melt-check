package com.goldmeltcheck.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goldmeltcheck.core.Bracket
import com.goldmeltcheck.core.Catalog
import com.goldmeltcheck.core.DealVerdict
import com.goldmeltcheck.core.Gem
import com.goldmeltcheck.core.GemMath
import com.goldmeltcheck.core.GemSign
import com.goldmeltcheck.core.Gems
import com.goldmeltcheck.core.Heat
import com.goldmeltcheck.core.MeltMath
import com.goldmeltcheck.core.PriceRange
import java.text.NumberFormat
import java.util.Locale

/** Sri Lankan gems: is this a good deal for its size and treatment, and signs it may be lab-made or fake. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GemsScreen(vm: GoldViewModel, contentPadding: PaddingValues) {
    val prices by vm.prices.collectAsStateWithLifecycle()
    val overrides by vm.gemRangeOverrides.collectAsStateWithLifecycle()
    val inputs = vm.inputs
    val gem = Gems.byId(inputs.gemId)
    val heat = Heat.entries.firstOrNull { it.name == inputs.gemHeat } ?: Heat.UNKNOWN
    val lkrPerUsd = prices.effectiveRate("LKR")
    val carats = MeltMath.parseAmount(inputs.gemCarats, phoneDecimal())
    val bracket = carats?.takeIf { it > 0 }?.let { Bracket.of(it) }
    var editing by remember { mutableStateOf<String?>(null) }

    fun key(b: Bracket, h: Heat) = "${gem.id}:${if (gem.heatMatters && h == Heat.UNHEATED) "U" else "H"}:${b.ordinal}"
    fun rangeFor(b: Bracket, h: Heat): PriceRange? = overrides[key(b, h)] ?: gem.range(b, h)

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
                Text("Sri Lankan Gems", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Check whether an asking price is a good deal for the stone's size and treatment, in rupees " +
                        "and dollars, and the signs that a stone is lab-made or fake.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Picker(
                label = "Stone",
                options = Gems.all.map { it.name },
                selected = Gems.all.indexOf(gem),
                onSelect = { i -> vm.update { it.copy(gemId = Gems.all[i].id) } },
                modifier = Modifier.fillMaxWidth(),
            )

            Panel {
                Eyebrow("Is it a good deal?")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = "Carats",
                        value = inputs.gemCarats,
                        suffix = "ct",
                        onValueChange = { v -> vm.update { it.copy(gemCarats = v) } },
                        supporting = bracket?.label ?: readBack(inputs.gemCarats, "ct", 3),
                        modifier = Modifier.weight(1f),
                    )
                    val codes = listOf("LKR", "USD") +
                        Catalog.currencies.map { it.code }.filter { it != "LKR" && it != "USD" }
                    Picker(
                        label = "Currency",
                        options = codes,
                        selected = codes.indexOf(inputs.gemCurrency).coerceAtLeast(0),
                        onSelect = { i -> vm.update { it.copy(gemCurrency = codes[i]) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (gem.heatMatters) {
                    Picker(
                        label = "Heat treatment (what the seller says)",
                        options = Heat.entries.map { it.label },
                        selected = heat.ordinal,
                        onSelect = { i -> vm.update { it.copy(gemHeat = Heat.entries[i].name) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                NumberField(
                    label = "Asking price (total)",
                    value = inputs.gemPrice,
                    suffix = inputs.gemCurrency,
                    onValueChange = { v -> vm.update { it.copy(gemPrice = v) } },
                    supporting = readBack(inputs.gemPrice, inputs.gemCurrency, 0),
                    modifier = Modifier.fillMaxWidth(),
                )

                val perCt = GemMath.pricePerCaratUsd(
                    MeltMath.parseAmount(inputs.gemPrice, phoneDecimal()),
                    prices.effectiveRate(inputs.gemCurrency),
                    carats,
                )
                if (perCt == null || bracket == null) {
                    Text("Enter the weight and the asking price.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        usd(perCt) + " /ct",
                        fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        "${rupees(perCt * lkrPerUsd)} /ct",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val range = rangeFor(bracket, heat)
                    VerdictPill(GemMath.verdict(perCt, range), gem, heat, range, lkrPerUsd)
                    if (gem.heatMatters && heat == Heat.UNKNOWN) {
                        rangeFor(bracket, Heat.UNHEATED)?.let { u ->
                            Text(
                                "Judged against heated prices. If the seller says it's unheated, the usual range is " +
                                    "${both(u, lkrPerUsd)} per carat, but only a lab report proves it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Panel {
                Eyebrow("${gem.name} prices by size")
                val today = java.time.LocalDate.now()
                if (Gems.isStale(today.year, today.monthValue)) {
                    Text(
                        "These ballparks are from ${Gems.PRICES_AS_OF}, " +
                            "${Gems.ageInMonths(today.year, today.monthValue)} months ago, and may be out of date. " +
                            "Tap Edit to use current numbers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalVerdictColors.current.warn,
                    )
                }
                val heats = if (gem.heatMatters) listOf(Heat.HEATED, Heat.UNHEATED) else listOf(Heat.HEATED)
                heats.forEach { h ->
                    if (gem.heatMatters) Text(h.label, fontWeight = FontWeight.SemiBold)
                    Bracket.entries.forEach { b ->
                        BracketRow(
                            label = b.label,
                            range = rangeFor(b, h),
                            lkrPerUsd = lkrPerUsd,
                            current = b == bracket && (!gem.heatMatters || h == heat || (heat == Heat.UNKNOWN && h == Heat.HEATED)),
                            edited = overrides.containsKey(key(b, h)),
                            onEdit = { editing = key(b, h) },
                        )
                    }
                }
                Text(
                    "Typical asking prices from Sri Lankan dealers, per carat, compiled ${Gems.PRICES_AS_OF}. Street " +
                        "prices in Ratnapura can be lower and tourist-shop prices much higher. Colour, clarity and cut " +
                        "move prices a lot within each range. \"No figure\" means no reliable source was found; " +
                        "tap Edit to add your own.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Panel {
                Eyebrow("Signs it may be lab-made or fake")
                Text(gem.summary)
                SignList(gem.signs)
            }

            Panel {
                Eyebrow("Buying in Sri Lanka")
                SignList(Gems.general)
            }

            Text(
                "These are hints, not proof. Many lab-grown stones show none of these signs. For anything " +
                    "expensive, get it tested at the NGJA lab or another independent gem lab before paying.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    editing?.let { k ->
        val (b, h) = Bracket.entries[k.substringAfterLast(':').toInt()] to
            (if (k.split(':')[1] == "U") Heat.UNHEATED else Heat.HEATED)
        val default = gem.range(b, h)
        RangeDialog(
            title = "${gem.name}, ${b.label}${if (gem.heatMatters) ", ${h.label.lowercase()}" else ""}",
            current = overrides[k] ?: default,
            hasDefault = default != null,
            onSave = { vm.setGemRange(k, it); editing = null },
            onReset = { vm.setGemRange(k, null); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun VerdictPill(verdict: DealVerdict, gem: Gem, heat: Heat, range: PriceRange?, lkrPerUsd: Double) {
    val c = LocalVerdictColors.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val usual = range?.let { "The usual range for this size is ${both(it, lkrPerUsd)} per carat." }.orEmpty()
    val (detail, tone) = when (verdict) {
        DealVerdict.TOO_GOOD -> ("Far below the usual price. It may be lab-made, glass, a different stone, or " +
            (if (gem.heatMatters && heat == Heat.UNHEATED) "heated stone sold as unheated. " else "heavily treated. ") +
            "Get it tested before paying. $usual") to c.bad
        DealVerdict.GOOD -> "In the lower part of the usual range, or just below it. A good price if the stone is " +
            "genuine. $usual" to c.good
        DealVerdict.FAIR -> "In the middle of the usual range. $usual" to c.good
        DealVerdict.PRICEY -> "In the upper part of the usual range. Worth negotiating unless the colour and " +
            "clarity are exceptional. $usual" to c.warn
        DealVerdict.OVERPRICED -> "Above the usual range. Negotiate hard or walk away, unless a lab report shows " +
            "exceptional quality. $usual" to c.bad
        DealVerdict.NO_REFERENCE -> "No reliable price figure for this stone and size yet. Tap Edit below to add " +
            "your own range." to muted
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(shape = RoundedCornerShape(50), color = Color.Transparent, border = BorderStroke(1.dp, tone)) {
            Text(verdict.label, color = tone, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

@Composable
private fun BracketRow(
    label: String,
    range: PriceRange?,
    lkrPerUsd: Double,
    current: Boolean,
    edited: Boolean,
    onEdit: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(range?.let { "${whole(it.low)} – ${whole(it.high)}" } ?: "No figure", fontFamily = FontFamily.Monospace)
            range?.let {
                Text("${rupees(it.low * lkrPerUsd)} – ${rupees(it.high * lkrPerUsd)}",
                    style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (edited) Text("Your numbers", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
        TextButton(onClick = onEdit) { Text("Edit") }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun SignList(signs: List<GemSign>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        signs.forEach { s ->
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.width(78.dp),
                ) {
                    Text(
                        s.check.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
                Text(s.text, modifier = Modifier.padding(start = 10.dp).weight(1f))
            }
        }
    }
}

@Composable
private fun RangeDialog(
    title: String,
    current: PriceRange?,
    hasDefault: Boolean,
    onSave: (PriceRange) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var low by remember { mutableStateOf(current?.low?.let(::plain) ?: "") }
    var high by remember { mutableStateOf(current?.high?.let(::plain) ?: "") }
    val lowV = MeltMath.parseAmount(low, phoneDecimal())
    val highV = MeltMath.parseAmount(high, phoneDecimal())
    val valid = lowV != null && highV != null && lowV > 0 && highV > lowV

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("USD per carat", color = MaterialTheme.colorScheme.onSurfaceVariant)
                NumberField(label = "Low", value = low, onValueChange = { low = it }, suffix = "USD",
                    modifier = Modifier.fillMaxWidth())
                NumberField(label = "High", value = high, onValueChange = { high = it }, suffix = "USD",
                    supporting = if (lowV != null && highV != null && highV <= lowV) "High must be more than Low" else null,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { if (lowV != null && highV != null) onSave(PriceRange(lowV, highV)) }, enabled = valid) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text(if (hasDefault) "Use default" else "Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/** "$300–1,200 (Rs 99,000–396,000)" */
private fun both(r: PriceRange, lkrPerUsd: Double): String =
    "${whole(r.low)}–${whole(r.high).removePrefix("$")} (${rupees(r.low * lkrPerUsd)}–${rupees(r.high * lkrPerUsd).removePrefix("Rs ")})"

private fun whole(v: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(v)

private fun rupees(v: Double): String =
    "Rs " + NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(v)

/** Plain number with the phone's decimal separator, so it parses back unchanged. */
private fun plain(v: Double): String =
    (if (v == Math.floor(v)) v.toLong().toString() else v.toString()).replace('.', phoneDecimal())
