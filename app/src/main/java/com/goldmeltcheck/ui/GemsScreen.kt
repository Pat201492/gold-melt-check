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
import com.goldmeltcheck.core.Catalog
import com.goldmeltcheck.core.Gem
import com.goldmeltcheck.core.GemMath
import com.goldmeltcheck.core.GemSign
import com.goldmeltcheck.core.Gems
import com.goldmeltcheck.core.MeltMath
import com.goldmeltcheck.core.PriceBand
import com.goldmeltcheck.core.PriceRange
import java.text.NumberFormat
import java.util.Locale

/** Price per carat against dated ballparks, and a field guide to spotting lab-grown stones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GemsScreen(vm: GoldViewModel, contentPadding: PaddingValues) {
    val prices by vm.prices.collectAsStateWithLifecycle()
    val overrides by vm.gemRangeOverrides.collectAsStateWithLifecycle()
    val inputs = vm.inputs
    val gem = Gems.byId(inputs.gemId)
    val natural = overrides["${gem.id}:natural"] ?: gem.natural
    val lab = overrides["${gem.id}:lab"] ?: gem.lab
    var editing by remember { mutableStateOf<String?>(null) }

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
                Text("Gems", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Check a gem's price per carat and the common signs that it's lab-grown.",
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
                Eyebrow("Price per carat")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = "Carats",
                        value = inputs.gemCarats,
                        suffix = "ct",
                        onValueChange = { v -> vm.update { it.copy(gemCarats = v) } },
                        supporting = readBack(inputs.gemCarats, "ct", 3),
                        modifier = Modifier.weight(1f),
                    )
                    Picker(
                        label = "Currency",
                        options = Catalog.currencies.map { it.code },
                        selected = Catalog.currencies.indexOfFirst { it.code == inputs.gemCurrency }.coerceAtLeast(0),
                        onSelect = { i -> vm.update { it.copy(gemCurrency = Catalog.currencies[i].code) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                NumberField(
                    label = "Asking price (total)",
                    value = inputs.gemPrice,
                    suffix = inputs.gemCurrency,
                    onValueChange = { v -> vm.update { it.copy(gemPrice = v) } },
                    supporting = readBack(inputs.gemPrice, inputs.gemCurrency, 2),
                    modifier = Modifier.fillMaxWidth(),
                )

                val perCt = GemMath.pricePerCaratUsd(
                    MeltMath.parseAmount(inputs.gemPrice, phoneDecimal()),
                    prices.effectiveRate(inputs.gemCurrency),
                    MeltMath.parseAmount(inputs.gemCarats, phoneDecimal()),
                )
                if (perCt == null) {
                    Text("Enter a weight and a price.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        usd(perCt) + " /ct",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    BandPill(GemMath.band(perCt, natural, lab), gem)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                RangeRow(
                    label = if (gem.natural == null && gem.lab != null) "Natural" else "Natural, ~1 ct",
                    range = natural,
                    edited = overrides.containsKey("${gem.id}:natural"),
                    onEdit = { editing = "${gem.id}:natural" },
                )
                RangeRow(
                    label = "Lab-grown, ~1 ct",
                    range = lab,
                    edited = overrides.containsKey("${gem.id}:lab"),
                    onEdit = { editing = "${gem.id}:lab" },
                )
                Text(
                    "Rough retail ballparks, compiled ${Gems.PRICES_AS_OF} from retailer and price-tracker " +
                        "figures. Size, cut, color, clarity, origin and treatment move prices a lot, so a price " +
                        "can only raise a question, not prove anything. Tap Edit to use your own numbers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Panel {
                Eyebrow("Signs it may be lab-grown")
                Text(gem.summary)
                SignList(gem.signs)
            }

            Panel {
                Eyebrow("For any stone")
                SignList(Gems.general)
            }

            Text(
                "These are hints, not proof. Many lab-grown stones show none of these signs, and some natural " +
                    "stones show a few. For anything expensive, buy with a report from an independent gem lab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    editing?.let { key ->
        val current = overrides[key] ?: if (key.endsWith(":lab")) gem.lab else gem.natural
        val default = if (key.endsWith(":lab")) gem.lab else gem.natural
        RangeDialog(
            title = if (key.endsWith(":lab")) "${gem.name}, lab-grown" else "${gem.name}, natural",
            current = current,
            hasDefault = default != null,
            onSave = { vm.setGemRange(key, it); editing = null },
            onReset = { vm.setGemRange(key, null); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun BandPill(band: PriceBand, gem: Gem) {
    val c = LocalVerdictColors.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val labOnly = gem.natural == null && gem.lab != null
    val (label, detail, tone) = when (band) {
        PriceBand.LAB -> Triple(
            "Priced like lab-grown",
            if (labOnly) "In the usual range." else "If it's sold as natural, ask for a lab report before paying.",
            if (labOnly) c.good else c.warn,
        )
        PriceBand.NATURAL -> Triple(
            "Within the natural ballpark",
            "Price alone can't prove a stone is natural.",
            c.good,
        )
        PriceBand.BETWEEN -> Triple(
            "Between lab-grown and natural prices",
            "Could be a lower-grade natural stone or a pricey lab one. Check the signs below.",
            c.warn,
        )
        PriceBand.BELOW_ALL -> Triple(
            "Below every ballpark",
            "Very cheap for this stone: it could be lab-grown, an imitation, or heavily treated.",
            c.bad,
        )
        PriceBand.ABOVE_ALL -> Triple(
            "Above the ballpark",
            "Large, fine or certified stones can be. Make sure the report backs the price.",
            c.warn,
        )
        PriceBand.BOTH -> Triple("Fits both ranges", "Your natural and lab ranges overlap here.", muted)
        PriceBand.NO_REFERENCE -> Triple(
            "No ballpark for this stone yet",
            "Tap Edit below to add your own range.",
            muted,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(shape = RoundedCornerShape(50), color = Color.Transparent, border = BorderStroke(1.dp, tone)) {
            Text(label, color = tone, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

@Composable
private fun RangeRow(label: String, range: PriceRange?, edited: Boolean, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                range?.let { "${whole(it.low)} – ${whole(it.high)} /ct" } ?: "No figure",
                fontFamily = FontFamily.Monospace,
            )
            if (edited) Text("Your numbers", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
        TextButton(onClick = onEdit) { Text("Edit") }
    }
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
    val valid = lowV != null && highV != null && lowV >= 0 && highV >= lowV

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("USD per carat", color = MaterialTheme.colorScheme.onSurfaceVariant)
                NumberField(label = "Low", value = low, onValueChange = { low = it }, suffix = "USD",
                    modifier = Modifier.fillMaxWidth())
                NumberField(label = "High", value = high, onValueChange = { high = it }, suffix = "USD",
                    supporting = if (lowV != null && highV != null && highV < lowV) "High must be at least Low" else null,
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

private fun whole(v: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(v)

/** Plain number with the phone's decimal separator, so it parses back unchanged. */
private fun plain(v: Double): String =
    (if (v == Math.floor(v)) v.toLong().toString() else v.toString()).replace('.', phoneDecimal())
