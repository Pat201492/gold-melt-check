package com.goldmeltcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldmeltcheck.ui.GoldScreen
import com.goldmeltcheck.ui.GoldTheme
import com.goldmeltcheck.ui.GoldViewModel
import com.goldmeltcheck.ui.RatesScreen

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
                            NavigationBarItem(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                                label = { Text("Check") },
                            )
                            NavigationBarItem(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                label = { Text("Rates") },
                            )
                        }
                    },
                ) { padding ->
                    if (tab == 0) {
                        GoldScreen(vm, padding)
                    } else {
                        RatesScreen(vm, padding, onPick = { code ->
                            vm.update { it.copy(currency = code) }
                            tab = 0
                        })
                    }
                }
            }
        }
    }
}
