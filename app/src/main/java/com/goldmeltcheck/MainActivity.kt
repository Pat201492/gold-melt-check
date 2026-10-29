package com.goldmeltcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldmeltcheck.ui.CurrencyScreen
import com.goldmeltcheck.ui.GemsScreen
import com.goldmeltcheck.ui.GoldScreen
import com.goldmeltcheck.ui.GoldTheme
import com.goldmeltcheck.ui.GoldViewModel
import com.goldmeltcheck.ui.RatesScreen

/** A cut-gem outline: flat table on top, pointed culet below. The core icon set has none. */
private val GemIcon: ImageVector = materialIcon(name = "Gem") {
    materialPath {
        moveTo(6f, 3f)
        lineTo(18f, 3f)
        lineTo(22f, 9f)
        lineTo(12f, 21f)
        lineTo(2f, 9f)
        close()
    }
}

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Check", Icons.Filled.CheckCircle),
    Tab("Gold Rates", Icons.Filled.Star),
    Tab("Currency Rates", Icons.Filled.Refresh),
    Tab("Gems", GemIcon),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoldTheme {
                val vm: GoldViewModel = viewModel()
                var tab by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            tabs.forEachIndexed { i, t ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = { Icon(t.icon, contentDescription = null) },
                                    label = {
                                        Text(t.label, fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center,
                                            lineHeight = 13.sp)
                                    },
                                )
                            }
                        }
                    },
                ) { padding ->
                    when (tab) {
                        0 -> GoldScreen(vm, padding)
                        1 -> RatesScreen(vm, padding, onPick = { code ->
                            vm.update { it.copy(currency = code) }
                            tab = 0
                        })
                        2 -> CurrencyScreen(vm, padding)
                        else -> GemsScreen(vm, padding)
                    }
                }
            }
        }
    }
}
