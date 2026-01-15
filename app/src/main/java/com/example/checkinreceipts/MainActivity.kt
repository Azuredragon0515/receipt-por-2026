package com.example.checkinreceipts

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.checkinreceipts.ui.navigation.AppNavHost
import com.example.checkinreceipts.ui.theme.CheckinReceiptsTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CheckinReceiptsTheme {
                val snack = remember { SnackbarHostState() }
                val systemUi = com.google.accompanist.systemuicontroller.rememberSystemUiController()
                val appBarColor = MaterialTheme.colorScheme.primaryContainer
                val navBarColor = MaterialTheme.colorScheme.surface
                LaunchedEffect(appBarColor, navBarColor) {
                    systemUi.setStatusBarColor(color = appBarColor, darkIcons = false)
                    systemUi.setNavigationBarColor(color = navBarColor, darkIcons = true)
                }
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavHost()
                    SnackbarHost(hostState = snack)
                }
            }
        }
    }
}