@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.checkinreceipts.ui.checkin

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import com.example.checkinreceipts.data.repo.RecordRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume

@Composable
fun CheckInScreen(
    onBackClick: () -> Unit,
    exportAction: suspend () -> Pair<String, String>
) {
    val context = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val previewVisible = remember { mutableStateOf(false) }
    val previewText = remember { mutableStateOf("") }
    val filePath = remember { mutableStateOf<String?>(null) }
    val exporting = remember { mutableStateOf(false) }
    val statusText = remember { mutableStateOf("Not checked") }
    val latestSummary = remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val fine = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            scope.launch { performCheckIn(context, statusText, latestSummary) }
        } else {
            statusText.value = "Permission denied"
        }
    }

    LaunchedEffect(Unit) {
        val repo = RecordRepository.getInstance(context)
        val latest = repo.getLatestCheckIn()
        if (latest != null) {
            latestSummary.value = "Time: ${formatTs(latest.createdAt)}  Lat: ${latest.latitude}  Lng: ${latest.longitude}  Acc: ${latest.accuracy}"
            statusText.value = "Last check-in recorded"
        } else {
            statusText.value = "Not checked"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-in") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(hostState = snack) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(text = statusText.value, color = MaterialTheme.colorScheme.onSurface)
            if (latestSummary.value != null) {
                Text(text = latestSummary.value!!, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted || coarseGranted) {
                            scope.launch { performCheckIn(context, statusText, latestSummary) }
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors()
                ) { Text("Check in", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Button(
                    onClick = {
                        scope.launch {
                            val ok = runBiometric(context)
                            if (!ok) {
                                snack.showSnackbar("Authentication failed")
                                return@launch
                            }
                            if (!exporting.value) {
                                exporting.value = true
                                try {
                                    val (path, json) = exportAction()
                                    filePath.value = path
                                    previewText.value = json
                                    previewVisible.value = true
                                    snack.showSnackbar("Exported to $path")
                                } catch (_: Exception) {
                                    snack.showSnackbar("Export failed")
                                } finally {
                                    exporting.value = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors()
                ) { Text(if (exporting.value) "Exporting" else "Export", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (previewVisible.value) {
                ExportPreviewAlert(
                    title = "Export preview",
                    path = filePath.value ?: "",
                    json = previewText.value,
                    onDismiss = { previewVisible.value = false },
                    onCopy = {
                        val cm = context.getSystemService<ClipboardManager>() ?: return@ExportPreviewAlert
                        cm.setPrimaryClip(ClipData.newPlainText("export", previewText.value))
                    },
                    onShare = {
                        val f: File? = filePath.value?.let { File(it) }
                        val intent = Intent(Intent.ACTION_SEND)
                        if (f != null && f.exists()) {
                            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
                            intent.type = "application/json"
                            intent.putExtra(Intent.EXTRA_STREAM, uri)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, previewText.value)
                        }
                        ContextCompat.startActivity(context, Intent.createChooser(intent, "Share export"), null)
                    }
                )
            }
        }
    }
}

private fun formatTs(ts: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}

private suspend fun performCheckIn(
    context: Context,
    statusText: androidx.compose.runtime.MutableState<String>,
    latestSummary: androidx.compose.runtime.MutableState<String?>
) {
    try {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            statusText.value = "Permission denied"
            return
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
            client.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
        if (location == null) {
            statusText.value = "Location unavailable"
            return
        }
        val repo = RecordRepository.getInstance(context)
        repo.insertCheckIn(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = if (location.hasAccuracy()) location.accuracy else 0f,
            note = null
        )
        statusText.value = "Check-in recorded"
        latestSummary.value = "Time: ${formatTs(System.currentTimeMillis())}  Lat: ${location.latitude}  Lng: ${location.longitude}  Acc: ${if (location.hasAccuracy()) location.accuracy else 0f}"
    } catch (_: SecurityException) {
        statusText.value = "Permission denied"
    } catch (_: Throwable) {
        statusText.value = "Check-in failed"
    }
}

@Composable
fun ExportPreviewAlert(
    title: String,
    path: String,
    json: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 520.dp)
            ) {
                Text(
                    text = "Path: $path",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.padding(top = 8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = json,
                        style = MaterialTheme.typography.bodySmall,
                        softWrap = true
                    )
                }
                Spacer(Modifier.padding(top = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                    ) { Text("Share") }
                    Button(
                        onClick = onCopy,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                    ) { Text("Copy") }
                }
                Spacer(Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .heightIn(min = 52.dp)
                    ) { Text("Close") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

private suspend fun runBiometric(context: Context): Boolean {
    return suspendCancellableCoroutine { cont ->
        val activity = findFragmentActivity(context)
        if (activity == null) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }
        val manager = BiometricManager.from(activity)
        val capable = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (capable != BiometricManager.BIOMETRIC_SUCCESS) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }
        val executor = Executors.newSingleThreadExecutor()
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    cont.resume(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    cont.resume(false)
                }
                override fun onAuthenticationFailed() {
                    cont.resume(false)
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate")
            .setSubtitle("Confirm to export")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }
}

private tailrec fun findFragmentActivity(context: Context?): FragmentActivity? {
    return when (context) {
        null -> null
        is FragmentActivity -> context
        is android.content.ContextWrapper -> findFragmentActivity(context.baseContext)
        else -> null
    }
}