@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.checkinreceipts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.checkinreceipts.ui.list.RecordsListScreen
import com.example.checkinreceipts.ui.scan.ScanScreen
import com.example.checkinreceipts.ui.checkin.CheckInScreen
import com.example.checkinreceipts.ui.settings.SettingsScreen
import com.example.checkinreceipts.ui.contacts.ContactsScreen
import com.example.checkinreceipts.data.repo.RecordRepository
import com.example.checkinreceipts.data.export.Exporter

object Routes {
    const val List = "list"
    const val Scan = "scan"
    const val CheckIn = "checkin"
    const val Settings = "settings"
    const val Contacts = "contacts"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = Routes.List) {
        composable(Routes.List) {
            RecordsListScreen(
                onAddClick = { navController.navigate(Routes.Scan) },
                onItemClick = { },
                onCheckInClick = { navController.navigate(Routes.CheckIn) },
                onSettingsClick = { navController.navigate(Routes.Settings) },
                onContactsClick = { navController.navigate(Routes.Contacts) }
            )
        }
        composable(Routes.Scan) {
            ScanScreen(
                onSavedNavigateBack = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.CheckIn) {
            val ctx = LocalContext.current
            val repo = remember { RecordRepository.getInstance(ctx) }
            val exporter = remember { Exporter(ctx, repo) }
            CheckInScreen(
                onBackClick = { navController.popBackStack() },
                exportAction = {
                    val file = exporter.exportAll()
                    val jsonText = file.readText()
                    file.absolutePath to jsonText
                }
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onGoContacts = { navController.navigate(Routes.Contacts) }
            )
        }
        composable(Routes.Contacts) {
            ContactsScreen(
                onBackClick = {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        navController.navigate(Routes.List) {
                            popUpTo(Routes.List) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}