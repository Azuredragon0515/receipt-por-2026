package com.example.checkinreceipts.ui.contacts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.checkinreceipts.data.entity.ContactEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(onBackClick: () -> Unit) {
    val vm: ContactsViewModel = viewModel()
    val state by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val gson: Gson = remember { GsonBuilder().setPrettyPrinting().create() }
    LaunchedEffect(Unit) { vm.initialSync() }
    LaunchedEffect(Unit) { vm.autoRetry() }
    var showAdd by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<ContactEntity?>(null) }
    var editTarget by remember { mutableStateOf<ContactEntity?>(null) }
    var deleteSnackJob by remember { mutableStateOf<Job?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add contact")
            }
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.items.isEmpty() && !state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No contacts")
                        TextButton(onClick = { vm.refresh() }) { Text("Sync") }
                    }
                }
            } else if (state.items.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ContactRow(
                            item = item,
                            onClick = { preview = item },
                            onDelete = {
                                deleteSnackJob?.cancel()
                                deleteSnackJob = scope.launch {
                                    vm.delete(item)
                                    val res = snack.showSnackbar(
                                        message = "Deleted",
                                        actionLabel = "Undo",
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Long
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        vm.undoDelete(item.id)
                                    } else {
                                        vm.commitDelete(item.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddContactDialog(
            onDismiss = { showAdd = false },
            onConfirm = { n, p ->
                showAdd = false
                scope.launch { vm.add(n, p) }
            }
        )
    }

    if (preview != null) {
        val json = gson.toJson(preview)
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Contact detail") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = json, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = {
                            val current = preview
                            preview = null
                            if (current != null) {
                                val v = gson.toJson(current)
                                try {
                                    val share = Intent(Intent.ACTION_SEND)
                                    share.type = "text/plain"
                                    share.putExtra(Intent.EXTRA_TEXT, v)
                                    ContextCompat.startActivity(
                                        context,
                                        Intent.createChooser(share, "Share contact"),
                                        null
                                    )
                                } catch (_: Throwable) { }
                            }
                        }
                    ) { Text("Share") }
                    TextButton(
                        onClick = {
                            try {
                                val cm = context.getSystemService(ClipboardManager::class.java)
                                cm?.setPrimaryClip(ClipData.newPlainText("contact", json))
                            } catch (_: Throwable) { }
                        }
                    ) { Text("Copy") }
                    TextButton(
                        onClick = {
                            editTarget = preview
                            preview = null
                        }
                    ) { Text("Edit") }
                    TextButton(onClick = { preview = null }) { Text("Close") }
                }
            },
            dismissButton = {}
        )
    }

    if (editTarget != null) {
        EditContactDialog(
            initial = editTarget!!,
            onDismiss = { editTarget = null },
            onConfirm = { name, phone ->
                val updating = editTarget
                editTarget = null
                if (updating != null) {
                    scope.launch { vm.update(updating, name, phone) }
                }
            }
        )
    }
}

@Composable
private fun ContactRow(item: ContactEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val elevation by animateDpAsState(
        targetValue = if (item.pendingDelete || item.pendingSync) 2.dp else 1.dp,
        label = ""
    )
    val alpha = when {
        item.pendingDelete -> 0.4f
        item.pendingSync -> 0.8f
        else -> 1f
    }
    Surface(
        tonalElevation = elevation,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .semantics { if (item.pendingDelete) disabled() }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                val suffix = when {
                    item.pendingDelete -> " • Pending delete"
                    item.pendingSync -> " • Pending sync"
                    else -> ""
                }
                Text(
                    item.name + suffix,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.pendingDelete) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    item.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.pendingDelete) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                TextButton(onClick = onClick, enabled = !item.pendingDelete) { Text("Detail") }
                TextButton(onClick = onDelete, enabled = !item.pendingDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val canSubmit = name.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), phone.trim()) }, enabled = canSubmit) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                androidx.compose.material3.OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") }
                )
            }
        }
    )
}

@Composable
private fun EditContactDialog(
    initial: ContactEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var phone by remember { mutableStateOf(initial.phone) }
    val canSubmit = name.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), phone.trim()) }, enabled = canSubmit) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Edit contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                androidx.compose.material3.OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") }
                )
            }
        }
    )
}