package com.lemon.veil

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lemon.veil.data.IdentityEntity
import kotlinx.coroutines.async

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityManagerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val identities by viewModel.allIdentities.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var editingIdentity by remember { mutableStateOf<IdentityEntity?>(null) }
    var deletingIdentity by remember { mutableStateOf<IdentityEntity?>(null) }
    var deleteTaskCount by remember { mutableStateOf(0) }

    var taskCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }

    LaunchedEffect(identities) {
        taskCounts = identities.associate { identity ->
            identity.id to async { viewModel.getNoteCountForIdentity(identity.id) }
        }.mapValues { it.value.await() }
    }

    LaunchedEffect(deletingIdentity) {
        if (deletingIdentity != null) {
            deleteTaskCount = viewModel.getNoteCountForIdentity(deletingIdentity!!.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                title = { Text(stringResource(R.string.title_identity_manager)) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_new_identity))
            }
        },
    ) { padding ->
        if (identities.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.msg_no_identity),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(identities, key = { it.id }) { identity ->
                    IdentityCard(
                        identity = identity,
                        taskCount = taskCounts[identity.id] ?: 0,
                        onEdit = { editingIdentity = identity },
                        onDelete = { deletingIdentity = identity },
                    )
                }
            }
        }
    }

    if (showForm || editingIdentity != null) {
        IdentityFormDialog(
            initialName = editingIdentity?.name ?: "",
            initialDescription = editingIdentity?.description ?: "",
            onConfirm = { name, description ->
                if (editingIdentity != null) {
                    viewModel.updateIdentity(editingIdentity!!.copy(name = name, description = description))
                } else {
                    viewModel.insertIdentity(name, description)
                }
                showForm = false
                editingIdentity = null
            },
            onDismiss = { showForm = false; editingIdentity = null },
        )
    }

    if (deletingIdentity != null) {
        AlertDialog(
            onDismissRequest = { deletingIdentity = null },
            title = { Text(stringResource(R.string.action_delete_identity)) },
            text = {
                Text(
                    stringResource(R.string.msg_delete_identity, deleteTaskCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteIdentity(deletingIdentity!!.id)
                    deletingIdentity = null
                }) {
                    Text(stringResource(R.string.action_confirm_delete_identity), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingIdentity = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun IdentityCard(
    identity: IdentityEntity,
    taskCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = identity.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (identity.description.isNotEmpty()) {
                    Text(
                        text = identity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.identity_linked_tasks, taskCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit_identity), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete_identity), modifier = Modifier.size(18.dp))
            }
        }
    }
}
