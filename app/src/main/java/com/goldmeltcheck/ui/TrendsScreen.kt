package com.goldmeltcheck.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goldmeltcheck.core.AlertRule
import com.goldmeltcheck.core.AlertType
import com.goldmeltcheck.core.DAY_MS
import com.goldmeltcheck.core.History
import com.goldmeltcheck.core.MeltMath
import com.goldmeltcheck.core.Metal
import com.goldmeltcheck.core.PricePoint
import com.goldmeltcheck.core.WEEK_MS
import com.goldmeltcheck.data.PriceAlerts
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private enum class Span(val label: String, val ms: Long) {
    WEEK("1W", WEEK_MS), MONTH("1M", 30 * DAY_MS), QUARTER("3M", 91 * DAY_MS), YEAR("1Y", 365 * DAY_MS)
}

/** Price history, trend stats and price alerts for gold and silver. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrendsScreen(vm: GoldViewModel, contentPadding: PaddingValues) {
    val prices by vm.prices.collectAsStateWithLifecycle()
    val allHistory by vm.history.collectAsStateWithLifecycle()
    val rules by vm.alertRules.collectAsStateWithLifecycle()
    var metal by remember { mutableStateOf(vm.metal) }
    var span by remember { mutableStateOf(Span.MONTH) }
    var adding by remember { mutableStateOf(false) }
    val history = allHistory[metal].orEmpty()

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
                Text("Trends", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Price history saved on this phone, and alerts when prices move.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Picker(
                label = "Metal",
                options = Metal.entries.map { it.label },
                selected = metal.ordinal,
                onSelect = { metal = Metal.entries[it] },
                modifier = Modifier.fillMaxWidth(),
            )

            Panel {
                Eyebrow("${metal.label} spot")
                Text(
                    usd(history.lastOrNull()?.usdPerOz ?: prices.liveSpot(metal)) + " /oz",
                    fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Change("24h", History.changePercent(history, DAY_MS))
                    Change("1 week", History.changePercent(history, WEEK_MS))
                    Change("1 month", History.changePercent(history, 30 * DAY_MS))
                    Change("1 year", History.changePercent(history, 365 * DAY_MS))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                val fourWeek = History.range(history, 4 * WEEK_MS)
                Row(Modifier.fillMaxWidth()) {
                    Stat("4-week low", fourWeek?.first?.let { usd(it) } ?: "—", Modifier.weight(1f))
                    Stat("4-week high", fourWeek?.second?.let { usd(it) } ?: "—", Modifier.weight(1f))
                }
                if (fourWeek == null) {
                    Text(
                        "Needs 4 weeks of saved prices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Panel {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Span.entries.forEach { s ->
                        SpanChip(s.label, selected = s == span, onClick = { span = s })
                    }
                }
                val visible = history.lastOrNull()?.let { History.since(history, it.at - span.ms) }.orEmpty()
                if (visible.size < 2) {
                    Text(
                        "Not enough history to chart yet. The app saves a price about every hour, so this " +
                            "fills in as it runs.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val end = visible.last().at
                    TrendChart(visible, windowStart = end - span.ms, windowEnd = end)
                    val have = end - history.first().at
                    if (have < span.ms - DAY_MS) {
                        Text(
                            "Only ${historyLength(have)} of saved prices so far, so the line covers the right-hand " +
                                "end of this ${span.label} view. It fills in as the app keeps checking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    history.firstOrNull()?.let {
                        "History since ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it.at))} · " +
                            "${history.size} saved prices"
                    } ?: "No saved prices yet. Tap Refresh now on another tab to save the first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AlertsPanel(
                rules = rules,
                onChange = vm::saveAlertRules,
                onAdd = { adding = true },
            )

            Text(
                "Prices are checked about every hour on any connection (each check is under 1 KB). " +
                    "Android may delay checks to save battery, so an alert can arrive a little late. " +
                    "Gold and silver prices from gold-api.com.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (adding) {
        AddAlertDialog(
            initialMetal = metal,
            onSave = { vm.saveAlertRules(rules + it); adding = false },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun Change(label: String, percent: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val text = when {
            percent == null -> "—"
            percent > 0.005 -> "▲ " + String.format(Locale.getDefault(), "%.2f%%", percent)
            percent < -0.005 -> "▼ " + String.format(Locale.getDefault(), "%.2f%%", -percent)
            else -> "0.00%"
        }
        Text(text, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpanChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

/**
 * One series on a time axis: 2dp line with a faint area fill, low/high labels, an end marker, and a
 * crosshair you drag to read any saved price.
 */
@Composable
private fun TrendChart(points: List<PricePoint>, windowStart: Long, windowEnd: Long) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline
    val ink = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    var selected by remember(points) { mutableIntStateOf(-1) }
    val low = points.minOf { it.usdPerOz }
    val high = points.maxOf { it.usdPerOz }
    val shown = points.getOrNull(selected) ?: points.last()
    val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(usd(shown.usdPerOz), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            Text(fmt.format(Date(shown.at)), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth()) {
            Canvas(
                Modifier
                    .weight(1f)
                    .height(180.dp)
                    .pointerInput(points, windowStart) {
                        fun pick(x: Float) {
                            val span = (windowEnd - windowStart).coerceAtLeast(1)
                            val target = windowStart + (x / size.width).coerceIn(0f, 1f) * span
                            selected = points.indices.minBy { kotlin.math.abs(points[it].at - target) }
                        }
                        detectTapGestures { pick(it.x) }
                    }
                    .pointerInput(points, windowStart) {
                        fun pick(x: Float) {
                            val span = (windowEnd - windowStart).coerceAtLeast(1)
                            val target = windowStart + (x / size.width).coerceIn(0f, 1f) * span
                            selected = points.indices.minBy { kotlin.math.abs(points[it].at - target) }
                        }
                        detectDragGestures(
                            onDragStart = { pick(it.x) },
                            onDragEnd = { selected = -1 },
                            onDragCancel = { selected = -1 },
                        ) { change, _ -> pick(change.position.x) }
                    },
            ) {
                val pad = 6.dp.toPx()
                // The x axis always spans the chosen time frame, so 1W and 1Y look different even
                // while the saved history is shorter than the frame.
                val t0 = windowStart
                val tSpan = (windowEnd - windowStart).coerceAtLeast(1).toFloat()
                val pSpan = (high - low).takeIf { it > 0 } ?: 1.0
                fun x(p: PricePoint) = (p.at - t0) / tSpan * size.width
                fun y(v: Double) = pad + ((high - v) / pSpan).toFloat() * (size.height - 2 * pad)

                // Recessive grid: top, middle, bottom
                listOf(y(high), y((high + low) / 2), y(low)).forEach {
                    drawLine(grid, Offset(0f, it), Offset(size.width, it), strokeWidth = 1f)
                }
                val path = Path().apply {
                    points.forEachIndexed { i, p -> if (i == 0) moveTo(x(p), y(p.usdPerOz)) else lineTo(x(p), y(p.usdPerOz)) }
                }
                val area = Path().apply {
                    addPath(path)
                    lineTo(x(points.last()), size.height)
                    lineTo(x(points.first()), size.height)
                    close()
                }
                drawPath(area, line.copy(alpha = 0.12f))
                drawPath(path, line, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                val end = points.last()
                drawCircle(surface, radius = 6.dp.toPx(), center = Offset(x(end), y(end.usdPerOz)))
                drawCircle(line, radius = 4.dp.toPx(), center = Offset(x(end), y(end.usdPerOz)))

                points.getOrNull(selected)?.let { p ->
                    drawLine(ink.copy(alpha = 0.5f), Offset(x(p), 0f), Offset(x(p), size.height), strokeWidth = 1.dp.toPx())
                    drawCircle(surface, radius = 6.dp.toPx(), center = Offset(x(p), y(p.usdPerOz)))
                    drawCircle(ink, radius = 4.dp.toPx(), center = Offset(x(p), y(p.usdPerOz)))
                }
            }
            Column(
                Modifier.height(180.dp).padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                AxisLabel(usd(high))
                AxisLabel(usd(low))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val d = DateFormat.getDateInstance(DateFormat.SHORT)
            AxisLabel(d.format(Date(windowStart)))
            AxisLabel(d.format(Date(windowEnd)))
        }
        Text("Tap or drag on the chart to read a price.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "3 hours", "2 days", "5 weeks": how much history there is, for the short-history note. */
private fun historyLength(ms: Long): String = when {
    ms < DAY_MS -> "${(ms / (60 * 60 * 1000)).coerceAtLeast(1)} hours"
    ms < 2 * WEEK_MS -> "${ms / DAY_MS} days"
    else -> "${ms / WEEK_MS} weeks"
}

@Composable
private fun AxisLabel(text: String) = Text(
    text, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun AlertsPanel(rules: List<AlertRule>, onChange: (List<AlertRule>) -> Unit, onAdd: () -> Unit) {
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(PriceAlerts.canNotify(context)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = PriceAlerts.canNotify(context)
    }
    // Re-check when the screen returns, e.g. after the person changes it in system settings.
    LaunchedEffect(Unit) { canNotify = PriceAlerts.canNotify(context) }

    Panel {
        Eyebrow("Price alerts")
        if (!canNotify) {
            Text(
                "Notifications are off, so alerts can't reach you.",
                color = LocalVerdictColors.current.warn,
            )
            OutlinedButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }) { Text("Turn on notifications") }
            Text(
                "If Android doesn't ask, turn them on under Settings › Apps › Gold Melt Check › Notifications.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
        if (rules.isEmpty()) {
            Text("No alerts yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        rules.forEach { rule ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rule.describe(), modifier = Modifier.weight(1f))
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { on -> onChange(rules.map { if (it.id == rule.id) it.copy(enabled = on) else it }) },
                )
                IconButton(onClick = { onChange(rules.filter { it.id != rule.id }) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete alert: ${rule.describe()}")
                }
            }
        }
        OutlinedButton(onClick = onAdd) { Text("Add alert") }
        Text(
            "N-week high and low alerts need that many weeks of saved prices before they can fire. " +
                "Each alert notifies at most once every 20 hours.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddAlertDialog(initialMetal: Metal, onSave: (AlertRule) -> Unit, onDismiss: () -> Unit) {
    var metal by remember { mutableStateOf(initialMetal) }
    var type by remember { mutableStateOf(AlertType.NEW_LOW) }
    var value by remember(type) {
        mutableStateOf(when (type) {
            AlertType.NEW_LOW, AlertType.NEW_HIGH -> "4"
            AlertType.DAILY_MOVE -> "2"
            else -> ""
        })
    }
    val parsed = MeltMath.parseAmount(value, phoneDecimal())
    val (label, suffix) = when (type) {
        AlertType.BELOW, AlertType.ABOVE -> "Price" to "USD/oz"
        AlertType.NEW_LOW, AlertType.NEW_HIGH -> "Weeks" to "weeks"
        AlertType.DAILY_MOVE -> "Move" to "%"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New alert") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Picker(
                    label = "Metal",
                    options = Metal.entries.map { it.label },
                    selected = metal.ordinal,
                    onSelect = { metal = Metal.entries[it] },
                    modifier = Modifier.fillMaxWidth(),
                )
                Picker(
                    label = "When",
                    options = AlertType.entries.map { it.label },
                    selected = type.ordinal,
                    onSelect = { type = AlertType.entries[it] },
                    modifier = Modifier.fillMaxWidth(),
                )
                NumberField(label = label, value = value, onValueChange = { value = it }, suffix = suffix,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null && parsed > 0,
                onClick = {
                    if (parsed != null) onSave(AlertRule(UUID.randomUUID().toString(), metal, type, parsed))
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
