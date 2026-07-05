package com.papernotes.ui.components

import com.papernotes.ui.theme.PaperDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Tag-Editor einer Notiz: vorhandene Reiter werden als Chips angeboten (Tippen schaltet sie an der
 * Notiz an/aus), ein Textfeld legt neue an. Kein eigener Verwaltungs-Screen – ein Reiter "existiert",
 * solange ihn eine Notiz trägt.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagPickerSheet(
    noteTags: List<String>,
    allTags: List<String>,
    onToggle: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    var draft by remember { mutableStateOf("") }

    val submit: () -> Unit = {
        val t = draft.trim()
        if (t.isNotEmpty()) {
            onAdd(t)
            draft = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaperDimens.sheetHPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Reiter", style = MaterialTheme.typography.titleLarge, color = ink)

            if (allTags.isEmpty()) {
                Text(
                    text = "Noch keine Reiter. Tippe unten einen Namen ein, um den ersten anzulegen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    allTags.forEach { tag ->
                        TagChip(
                            label = tag,
                            selected = tag in noteTags,
                            onClick = { onToggle(tag) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Neuen Reiter eintippen…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Reiter hinzufügen",
                        tint = ink,
                        modifier = Modifier.paperPress(RoundedCornerShape(10.dp)) { submit() },
                    )
                },
            )
        }
    }
}

@Composable
private fun TagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = tabColor(label)
    Box(
        modifier = Modifier
            .paperPress(shape, onClick = onClick)
            .background(
                if (selected) bg else MaterialTheme.colorScheme.surfaceVariant,
                shape,
            )
            .then(
                if (!selected) {
                    Modifier.border(1.5.dp, bg.copy(alpha = 0.6f), shape)
                } else Modifier,
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
