package com.example.checkinreceipts.ui.list

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checkinreceipts.core.settings.SettingsDataStore
import com.example.checkinreceipts.data.repo.RecordRepository
import com.example.checkinreceipts.device.sensors.ShakeDetector
import com.example.checkinreceipts.domain.model.Record
import com.example.checkinreceipts.sensors.ShakeEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun RecordsListScreen(
    onAddClick: () -> Unit,
    onItemClick: (Record) -> Unit = {},
    onCheckInClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onContactsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: RecordsListViewModel = viewModel(factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app))
    val snack = remember { SnackbarHostState() }
    val records by vm.records.collectAsStateWithLifecycle(initialValue = emptyList())
    val isEmpty = records.isEmpty()
    val scope = rememberCoroutineScope()
    val repo = remember { RecordRepository.getInstance(context) }
    val detector = remember { ShakeDetector(context) }
    val settings = remember { SettingsDataStore(context) }
    val enableShake by settings.enableShakeToAdd.collectAsStateWithLifecycle(initialValue = true)
    var showShortcuts by remember { mutableStateOf(true) }
    var collector by remember { mutableStateOf<Job?>(null) }
    var showTriggered by remember { mutableStateOf(false) }
    var coolingActive by remember { mutableStateOf(false) }
    var coolingSeconds by remember { mutableLongStateOf(0L) }
    var uiLockedUntil by remember { mutableLongStateOf(0L) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }
    var previewRecord by remember { mutableStateOf<Record?>(null) }
    val prettyJson = remember { Json { prettyPrint = true } }

    fun nowMs(): Long = System.currentTimeMillis()

    fun startCoolingCountdown(totalMs: Long) {
        countdownJob?.cancel()
        coolingActive = true
        val start = nowMs()
        val end = start + totalMs
        uiLockedUntil = end
        countdownJob = scope.launch {
            var lastShown = -1L
            while (isActive) {
                val rem = end - nowMs()
                if (rem <= 0) {
                    coolingSeconds = 0
                    break
                }
                val sec = (rem + 999) / 1000
                if (sec != lastShown) {
                    lastShown = sec
                    coolingSeconds = sec
                }
                delay(200L)
            }
            coolingActive = false
        }
    }

    LaunchedEffect(enableShake) {
        collector?.cancel()
        collector = null
        if (!enableShake) return@LaunchedEffect
        collector = scope.launch {
            detector.events(
                threshold = 8.5f,
                requiredPeaks = 2,
                windowMs = 700L,
                cooldownMs = 5000L,
                minAxisContribution = 0.30f
            ).collect { event: ShakeEvent ->
                when (event) {
                    is ShakeEvent.Triggered -> {
                        if (nowMs() < uiLockedUntil || coolingActive) return@collect
                        showTriggered = true
                        scope.launch {
                            try { repo.quickAddNote("Shake!") } catch (_: Throwable) {
                                Toast.makeText(context, "Insert failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    is ShakeEvent.CoolingDown -> {
                        if (!coolingActive) {
                            startCoolingCountdown(5000L)
                        }
                        showTriggered = false
                    }
                    is ShakeEvent.Telemetry -> {}
                }
            }
        }
    }

    LaunchedEffect(showTriggered) {
        if (showTriggered) {
            delay(900L)
            showTriggered = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipts") },
                actions = {
                    IconButton(onClick = onContactsClick) { Icon(Icons.Filled.Person, contentDescription = "Contacts") }
                    IconButton(onClick = onCheckInClick) { Icon(Icons.Filled.LocationOn, contentDescription = "Check-in") }
                    IconButton(onClick = onSettingsClick) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
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
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Filled.Add, contentDescription = "Add") }
        }
    ) { inner ->
        Box(modifier = Modifier.padding(inner).fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                CoolingBanner(visible = coolingActive, seconds = coolingSeconds)
                if (isEmpty) {
                    EmptyState(
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                        onAddClick = onAddClick,
                        onCheckInClick = onCheckInClick
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showShortcuts) {
                            item {
                                ShortcutsRow(
                                    onScan = onAddClick,
                                    onContacts = onContactsClick,
                                    onClose = { showShortcuts = false }
                                )
                                HorizontalDivider(color = DividerDefaults.color)
                            }
                        }
                        items(records) { record ->
                            RecordRow(
                                record = record,
                                onClick = {
                                    previewRecord = record
                                    onItemClick(record)
                                }
                            )
                            HorizontalDivider(color = DividerDefaults.color)
                        }
                        item { Spacer(Modifier.height(96.dp)) }
                    }
                }
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.Center),
                visible = showTriggered,
                enter = fadeIn(animationSpec = tween(durationMillis = 150)),
                exit = fadeOut(animationSpec = tween(durationMillis = 600))
            ) {
                Text(text = "Triggered", fontSize = 28.sp)
            }
            if (previewRecord != null) {
                val json = remember(previewRecord) { prettyJson.encodeToString(previewRecord!!) }
                AlertDialog(
                    onDismissRequest = { previewRecord = null },
                    title = { Text("Record detail") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp, max = 520.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = json,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND)
                                        shareIntent.type = "text/plain"
                                        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, json)
                                        androidx.core.content.ContextCompat.startActivity(
                                            context,
                                            android.content.Intent.createChooser(shareIntent, "Share record"),
                                            null
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) { Text("Share", maxLines = 1) }
                                Button(
                                    onClick = {
                                        val cm = context.getSystemService(ClipboardManager::class.java)
                                        cm?.setPrimaryClip(ClipData.newPlainText("record", json))
                                        previewRecord = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) { Text("Copy", maxLines = 1) }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { previewRecord = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) { Text("Close", maxLines = 1) }
                            }
                        }
                    },
                    dismissButton = {}
                )
            }
        }
    }
}

@Composable
private fun ShortcutsRow(
    onScan: () -> Unit,
    onContacts: () -> Unit,
    onClose: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val compact = screenWidthDp < 360

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!compact) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onScan,
                    modifier = Modifier.height(40.dp)
                ) { Text("Scan receipt", maxLines = 1, fontSize = 13.sp) }
                Button(
                    onClick = onContacts,
                    modifier = Modifier.height(40.dp)
                ) { Text("Contacts", maxLines = 1, fontSize = 13.sp) }
            }
            TextButton(onClick = onClose) { Text("Hide", fontSize = 13.sp) }
        } else {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onScan,
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("Scan", maxLines = 1, fontSize = 12.sp) }
                Button(
                    onClick = onContacts,
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("Contacts", maxLines = 1, fontSize = 12.sp) }
            }
            TextButton(onClick = onClose) { Text("Hide", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit,
    onCheckInClick: () -> Unit
) {
    Column(modifier) {
        Text("No records yet")
        Spacer(Modifier.height(8.dp))
        Text("Tap the + button to scan a receipt and save it.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddClick) { Text("Scan receipt") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCheckInClick) { Text("Go to Check-in") }
    }
}

@Composable
private fun RecordRow(
    record: Record,
    onClick: () -> Unit
) {
    val title = when {
        !record.header.isNullOrBlank() -> record.header
        !record.note.isNullOrBlank() -> record.note
        (record.type ?: "").isNotBlank() -> record.type
        else -> "Record #${record.id}"
    } ?: "Record #${record.id}"
    val timeText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        .format(java.util.Date(record.createdAt))
    val isCheckIn = (record.type ?: "").equals("check-in", ignoreCase = true)
    val subtitle = if (isCheckIn) {
        val lat = record.latitude?.let { String.format(Locale.getDefault(), "%.5f", it) } ?: "-"
        val lng = record.longitude?.let { String.format(Locale.getDefault(), "%.5f", it) } ?: "-"
        val acc = record.accuracy?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "-"
        "Lat $lat  Lng $lng  Acc $acc"
    } else {
        listOfNotNull(record.dateText, record.totalText).joinToString(" • ").ifBlank { "Note only" }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CoolingBanner(
    visible: Boolean,
    seconds: Long
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .background(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Cooling",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Cooling down, please wait",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${if (seconds > 0) seconds else 0}s",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}