package com.goldmeltcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.goldmeltcheck.ui.GoldScreen
import com.goldmeltcheck.ui.GoldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoldTheme {
                GoldScreen()
            }
        }
    }
}
