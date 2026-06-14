package com.lemon.veil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun HabitStackCard(
    currentHabit: String,
    newHabit: String,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onFieldChanged: (field: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var localCurrent by remember(currentHabit) { mutableStateOf(currentHabit) }
    var localNew by remember(newHabit) { mutableStateOf(newHabit) }

    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.section_habit_stack),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    if (isEditing) {
                        onFieldChanged("currentHabit", localCurrent)
                        onFieldChanged("newHabit", localNew)
                    }
                    onToggleEdit()
                }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) stringResource(R.string.cd_save) else stringResource(R.string.cd_edit),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (isEditing) {
                LabelRow(stringResource(R.string.label_current_habit), labelWidth = 72) {
                    CompactTextField(
                        value = localCurrent,
                        onValueChange = { localCurrent = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(R.string.placeholder_current_habit),
                    )
                }
                Spacer(Modifier.height(6.dp))
                LabelRow(stringResource(R.string.label_new_habit), labelWidth = 72) {
                    CompactTextField(
                        value = localNew,
                        onValueChange = { localNew = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(R.string.placeholder_new_habit),
                    )
                }
            } else {
                val displayText = if (currentHabit.isEmpty() && newHabit.isEmpty()) {
                    stringResource(R.string.hint_current_habit)
                } else {
                    "继${currentHabit}之后，我将${newHabit}"
                }
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentHabit.isEmpty() && newHabit.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}
