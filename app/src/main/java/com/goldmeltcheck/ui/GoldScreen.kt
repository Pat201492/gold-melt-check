package com.goldmeltcheck.ui

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldmeltcheck.core.Catalog
import com.goldmeltcheck.core.GRAMS_PER_TROY_OUNCE
import com.goldmeltcheck.core.MeltMath
import com.goldmeltcheck.core.MeltResult
import com.goldmeltcheck.core.Metal
import com.goldmeltcheck.core.Verdict
import com.goldmeltcheck.data.PriceSnapshot
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

private val usdFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
internal fun usd(v: Double): String = usdFormat.format(v)
internal const val STALE_AFTER_MS = 24 * 60 * 60 * 1000L

/** The phone's decimal separator ("." in the US, "," in much of Europe). Breaks ties in parsing. */
fun phoneDecimal(): Char = DecimalFormatSymbols.getInstance().decimalSeparator

/** Echoes a typed amount back in the phone's own format, so a misread is visible at a glance. */
internal fun readBack(text: String, unit: String, maxDecimals: Int): String? {
    if (text.isBlank()) return null
    val v = MeltMath.parseAmount(text, phoneDecimal()) ?: return "Not a number"
    val nf = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 0
        maximumFractionDigits = maxDecimals
    }
    return "= ${nf.format(v)} $unit"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoldScreen(vm: GoldViewModel = viewModel(), contentPadding: PaddingValues = PaddingValues()) {
    val prices by vm.prices.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val result = vm.result(prices)

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
                Text("Gold Melt Check", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Enter what a dealer is asking for gold or silver. See it in US dollars and as a " +
                        "percentage of melt value.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            InputsCard(vm, prices)
            VerdictCard(result)
            PricesCard(vm, prices, refreshing)
            Text(
                "Melt value = grams × purity × spot ÷ 31.1035 (grams per troy ounce). ${vm.metal.premiumNote}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Gold and silver prices from gold-api.com. Rates by Exchange Rate API (exchangerate-api.com).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun Panel(container: Color = MaterialTheme.colorScheme.surface, content: @Composable () -> Unit) {
    Surface(
        color = container,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
}

@Composable
internal fun Eyebrow(text: String) = Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelMedium,
    letterSpacing = 1.sp,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun InputsCard(vm: GoldViewModel, p: PriceSnapshot) {
    val inputs = vm.inputs
    val metal = vm.metal
    Panel {
        Picker(
            label = "Metal",
            options = Metal.entries.map { it.label },
            selected = metal.ordinal,
            onSelect = { i -> vm.selectMetal(Metal.entries[i]) },
            modifier = Modifier.fillMaxWidth(),
        )
        Picker(
            label = "Currency you're quoted in",
            options = Catalog.currencies.map { "${it.code} · ${it.name}" },
            selected = Catalog.currencies.indexOfFirst { it.code == inputs.currency }.coerceAtLeast(0),
            onSelect = { i -> vm.update { it.copy(currency = Catalog.currencies[i].code) } },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Weight",
                value = inputs.grams,
                suffix = "g",
                onValueChange = { v -> vm.update { it.copy(grams = v) } },
                supporting = readBack(inputs.grams, "g", 3),
                modifier = Modifier.weight(1f),
            )
            Picker(
                label = "Purity",
                options = metal.purities.map { it.label },
                selected = inputs.purityIndex.coerceIn(0, metal.purities.lastIndex),
                onSelect = { i -> vm.update { it.copy(purityIndex = i) } },
                modifier = Modifier.weight(1.2f),
            )
        }
        MeltLine(vm.meltUsd(p), inputs.currency, p.effectiveRate(inputs.currency))
        Picker(
            label = "The price is",
            options = listOf("Total for the piece", "Per gram"),
            selected = if (inputs.pricePerGram) 1 else 0,
            onSelect = { i -> vm.update { it.copy(pricePerGram = i == 1) } },
            modifier = Modifier.fillMaxWidth(),
        )
        val total = vm.totalPrice
        NumberField(
            label = if (inputs.pricePerGram) "Price per gram" else "Price they're charging (total)",
            value = inputs.price,
            suffix = if (inputs.pricePerGram) "${inputs.currency}/g" else inputs.currency,
            onValueChange = { v -> vm.update { it.copy(price = v) } },
            supporting = if (inputs.pricePerGram && total != null) {
                "${readBack(inputs.price, "${inputs.currency}/g", 2)} · total ${amountIn(total)} ${inputs.currency}"
            } else {
                readBack(inputs.price, inputs.currency, 2)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Melt value of the weight entered, shown before any price: USD, plus the quoted currency. */
@Composable
private fun MeltLine(meltUsd: Double?, currency: String, unitsPerUsd: Double) {
    if (meltUsd == null) return
    Surface(
        color = LocalVerdictColors.current.goldSoft,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Melt value", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(usd(meltUsd), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (currency != "USD") {
                    Text(
                        "≈ ${amountIn(meltUsd * unitsPerUsd)} $currency",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A money amount in the phone's number format, two decimals. */
private fun amountIn(v: Double): String =
    NumberFormat.getNumberInstance().apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(v)

@Composable
private fun VerdictCard(result: MeltResult?) {
    val colors = LocalVerdictColors.current
    Panel(container = colors.goldSoft) {
        Eyebrow("Percent of melt")
        if (result == null) {
            Text("—", fontSize = 52.sp, fontWeight = FontWeight.Bold)
            Text("Enter a weight and a price.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Panel
        }
        Text(
            String.format(Locale.US, "%.1f%%", result.percentOfMelt),
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 56.sp,
        )
        val (label, tone) = when (result.verdict) {
            Verdict.BELOW_MELT -> "Below melt" to colors.good
            Verdict.NEAR_MELT -> "Close to melt" to colors.good
            Verdict.MODERATE_MARKUP -> "Moderate markup" to colors.warn
            Verdict.HIGH_MARKUP -> "High markup" to colors.bad
        }
        Surface(shape = RoundedCornerShape(50), color = Color.Transparent, border = BorderStroke(1.dp, tone)) {
            Text(label, color = tone, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
        MeltMeter(result.percentOfMelt)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Figure("Price in USD", usd(result.priceUsd), strong = true)
        Figure("Melt value (USD)", usd(result.meltUsd))
        Figure("Premium over melt", (if (result.premiumUsd >= 0) "+" else "−") + usd(kotlin.math.abs(result.premiumUsd)))
        Figure("Paying per gram", usd(result.usdPerGram) + " /g")
        Figure("Pure gold content", String.format(Locale.US, "%.3f g", result.fineGrams))
    }
}

/** 0–200% of melt; the dark tick marks 100% (exactly melt). */
@Composable
private fun MeltMeter(percent: Double) {
    val track = MaterialTheme.colorScheme.surface
    val fill = MaterialTheme.colorScheme.primary
    val tick = MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            val barTop = 2.dp.toPx()
            val barH = size.height - 2 * barTop
            val r = CornerRadius(barH / 2)
            drawRoundRect(track, Offset(0f, barTop), Size(size.width, barH), r)
            val frac = (percent / 200).coerceIn(0.0, 1.0).toFloat()
            if (frac > 0f) drawRoundRect(fill, Offset(0f, barTop), Size(size.width * frac, barH), r)
            drawRect(tick, Offset(size.width / 2 - 1.dp.toPx(), 0f), Size(2.dp.toPx(), size.height))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0%", "100% melt", "200%").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun PricesCard(vm: GoldViewModel, p: PriceSnapshot, refreshing: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(60_000) }
    }
    val code = vm.inputs.currency

    Panel {
        Eyebrow("Spot price and exchange rate")

        val metal = vm.metal
        OverrideField(
            label = "${metal.label} spot, USD per troy oz",
            live = p.liveSpot(metal),
            override = p.overrideFor(metal),
            decimals = 2,
            onEdit = vm::setSpotOverride,
            key = metal,
        )
        SourceLine(
            fetchedAt = p.fetchedAt(metal),
            overridden = p.overrideFor(metal) != null,
            liveText = usd(p.liveSpot(metal)),
            now = now,
            onUseLive = { vm.setSpotOverride(null) },
        )
        Text(
            "${usd(p.spot(metal) / GRAMS_PER_TROY_OUNCE)} per gram of pure ${metal.label.lowercase()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (code != "USD") {
            OverrideField(
                label = "$code per 1 USD",
                live = p.liveRate(code),
                override = p.fxOverrides[code],
                decimals = 6,
                onEdit = vm::setRateOverride,
                key = code,
            )
            SourceLine(
                fetchedAt = if (p.unitsPerUsd.containsKey(code)) p.fxFetchedAt else 0,
                overridden = p.fxOverrides.containsKey(code),
                liveText = trim(p.liveRate(code), 6),
                now = now,
                onUseLive = { vm.clearRateOverride() },
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Text(
            "Prices refresh automatically about every 6 hours on WiFi, and never over mobile data. " +
                "Refresh now uses any connection. Offline, the app uses the last saved prices.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        p.lastError?.let { err ->
            Text(
                "Last refresh ${relative(p.lastAttemptAt, now)} failed:\n$err",
                style = MaterialTheme.typography.bodySmall,
                color = LocalVerdictColors.current.bad,
            )
        }
    }
}

@Composable
private fun SourceLine(fetchedAt: Long, overridden: Boolean, liveText: String, now: Long, onUseLive: () -> Unit) {
    val colors = LocalVerdictColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        val (text, tone) = when {
            overridden -> "Your number (live: $liveText)" to MaterialTheme.colorScheme.onSurfaceVariant
            fetchedAt == 0L -> "Starting estimate. Not live yet: connect to WiFi or tap Refresh now." to colors.warn
            now - fetchedAt > STALE_AFTER_MS -> "Live, but updated ${relative(fetchedAt, now)}" to colors.warn
            else -> "Live · updated ${relative(fetchedAt, now)}" to colors.good
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = tone, modifier = Modifier.weight(1f))
        if (overridden) TextButton(onClick = onUseLive) { Text("Use live") }
    }
}

/** Shows the live value until the person types their own; typing saves it as an override. */
@Composable
private fun OverrideField(
    label: String,
    live: Double,
    override: Double?,
    decimals: Int,
    onEdit: (Double?) -> Unit,
    key: Any = Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var text by remember(key) { mutableStateOf(trim(override ?: live, decimals)) }
    LaunchedEffect(key, live, override) {
        if (!focused) text = trim(override ?: live, decimals)
    }
    NumberField(
        label = label,
        value = text,
        onValueChange = {
            text = it
            onEdit(MeltMath.parseAmount(it, phoneDecimal()))
        },
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
    )
}

@Composable
internal fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        suffix = suffix?.let { { Text(it, fontFamily = FontFamily.Monospace) } },
        supportingText = supporting?.let { { Text(it, fontFamily = FontFamily.Monospace) } },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Picker(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = options.getOrElse(selected) { options.first() },
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(i); expanded = false })
            }
        }
    }
}

internal fun relative(at: Long, now: Long): String =
    if (now - at < DateUtils.MINUTE_IN_MILLIS) "just now"
    else DateUtils.getRelativeTimeSpanString(at, now, DateUtils.MINUTE_IN_MILLIS).toString()

/** Plain digits with the phone's decimal separator and no grouping, so it parses back unchanged. */
private fun trim(v: Double, decimals: Int): String =
    BigDecimal(v).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        .replace('.', phoneDecimal())
