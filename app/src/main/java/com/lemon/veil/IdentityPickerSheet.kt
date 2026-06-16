package com.lemon.veil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lemon.veil.data.IdentityEntity

@Composable
fun IdentityPickerDialog(
    allIdentities: List<IdentityEntity>,
    selectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
    onNewIdentity: () -> Unit,
) {
    var localSelected by remember(selectedIds, allIdentities) { mutableStateOf(selectedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_identity_picker)) },
        text = {
            Column {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allIdentities) { identity ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = identity.id in localSelected,
                                onCheckedChange = { checked ->
                                    localSelected = if (checked) localSelected + identity.id
                                    else localSelected - identity.id
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = identity.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (identity.description.isNotEmpty()) {
                                    Text(
                                        text = identity.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onNewIdentity, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_new_identity))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(localSelected) }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun IdentityFormDialog(
    initialName: String = "",
    initialDescription: String = "",
    onConfirm: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialName.isEmpty()) stringResource(R.string.action_new_identity)
                else stringResource(R.string.action_edit_identity)
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.label_identity_name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CompactTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.hint_identity_name),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.label_identity_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CompactTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = stringResource(R.string.hint_identity_description),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, description) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
