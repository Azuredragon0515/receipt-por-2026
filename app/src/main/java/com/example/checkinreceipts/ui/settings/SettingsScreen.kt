package com.example.checkinreceipts.ui.settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.checkinreceipts.core.settings.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onGoContacts: () -> Unit
) {
    val ctx = LocalContext.current
    val ds = remember { SettingsDataStore(ctx) }
    var apiBase by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("200") }
    var saveOriginal by remember { mutableStateOf(false) }
    var enableShake by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        apiBase = ds.apiBaseUrl.first()
        radius = ds.checkInRadiusM.first().toString()
        saveOriginal = ds.saveOriginalImage.first()
        enableShake = ds.enableShakeToAdd.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {}
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier.padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("General", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = apiBase,
                onValueChange = { apiBase = it },
                label = { Text("API Base URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = radius,
                onValueChange = { radius = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Check-in Radius (m)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Save original image")
                Switch(checked = saveOriginal, onCheckedChange = { saveOriginal = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable shake to add")
                Switch(checked = enableShake, onCheckedChange = { enableShake = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* save */ },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
                Button(
                    onClick = onGoContacts,
                    modifier = Modifier.weight(1f)
                ) { Text("Manage") }
            }
        }
    }
}