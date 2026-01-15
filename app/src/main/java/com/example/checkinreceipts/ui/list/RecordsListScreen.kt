package com.example.checkinreceipts.ui.list

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                        if (nowMs() < uiLockedUntil || coolingActive) {
                            return@collect
                        }
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
                            RecordRow(record = record, onClick = { onItemClick(record) })
                            HorizontalDivider(color = DividerDefaults.color)
                        }
                        item { Spacer(Modifier.height(96.dp)) }
                    }
                }
            }
            if (coolingActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = if (coolingSeconds > 0) "Cooling down… ${coolingSeconds}s" else "Cooling down… 0s",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.Center),
                visible = showTriggered,
                enter = fadeIn(animationSpec = tween(durationMillis = 150)),
                exit = fadeOut(animationSpec = tween(durationMillis = 600))
            ) {
                Text(text = "Triggered", fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onScan) { Text("Scan now", fontSize = 13.sp) }
            Button(onClick = onContacts) { Text("Go to Contacts", fontSize = 13.sp) }
        }
        TextButton(onClick = onClose) { Text("Hide", fontSize = 13.sp) }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit,
    onCheckInClick: () -> Unit
) {
    Column(modifier) {
        Text("No records yet", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Tap the + button to scan a receipt and save it.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddClick) { Text("Scan now") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCheckInClick) { Text("Go to Check-in") }
    }
}

@Composable
private fun RecordRow(
    record: Record,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(text = "Record #${record.id}")
        Spacer(Modifier.height(4.dp))
        Text(text = record.createdAt.toString())
    }
}