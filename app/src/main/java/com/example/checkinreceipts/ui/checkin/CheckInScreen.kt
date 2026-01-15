@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.checkinreceipts.ui.checkin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.checkinreceipts.data.export.ExportUtils
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CheckInScreen(
    onBackClick: () -> Unit = {},
    exportAction: suspend () -> File?
) {
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var previewOpen by remember { mutableStateOf(false) }
    var previewText by remember { mutableStateOf("") }
    var previewFull by remember { mutableStateOf("") }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-in") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { p ->
        Column(
            modifier = Modifier.padding(p).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Location status", style = MaterialTheme.typography.titleMedium)
            Text("Data export", style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                scope.launch {
                    val f = exportAction()
                    if (f == null) {
                        snack.showSnackbar("Export failed")
                    } else {
                        exportedFile = f
                        val raw = ExportUtils.readFileText(f)
                        val pretty = if (ExportUtils.isLikelyJsonObjectOrArray(raw)) ExportUtils.formatJsonIfPossible(raw) else raw
                        val redacted = ExportUtils.redactJson(pretty)
                        previewFull = redacted
                        previewText = ExportUtils.firstLines(redacted, 10)
                        previewOpen = true
                        snack.showSnackbar("Exported: ${f.absolutePath}")
                    }
                }
            }) { Text("Biometric Export JSON") }
            if (previewOpen) {
                AlertDialog(
                    onDismissRequest = { previewOpen = false },
                    title = { Text("Export preview") },
                    text = {
                        OutlinedTextField(
                            value = previewText,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            readOnly = true,
                            minLines = 6
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            exportedFile?.let { ExportUtils.shareJson(ctx, it) }
                        }) { Text("Share JSON") }
                    },
                    dismissButton = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                ExportUtils.copyToClipboard(ctx, "records.json", previewFull)
                                scope.launch { snack.showSnackbar("Copied redacted JSON") }
                            }) { Text("Copy") }
                            TextButton(onClick = { previewOpen = false }) { Text("Close") }
                        }
                    }
                )
            }
        }
    }
}

