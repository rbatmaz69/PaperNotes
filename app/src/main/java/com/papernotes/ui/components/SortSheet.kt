package com.papernotes.ui.components

import com.papernotes.ui.theme.PaperDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.papernotes.ui.notes.SortMode
import com.papernotes.util.rememberPaperHaptics

/**
 * Bottom-Sheet „Ordnen": wählt die Grid-Sortierung (Pinnwand / Erstellt / Titel) und
 * bietet darunter den Einstieg in den Anordnen-Modus (freies Ziehen auf der Pinnwand).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(
    selected: SortMode,
    onPick: (SortMode) -> Unit,
    onArrange: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberPaperHaptics()
    val ink = MaterialTheme.colorScheme.onBackground

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaperDimens.sheetHPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Ordnen",
                style = MaterialTheme.typography.titleLarge,
                color = ink,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            SortRow(
                label = "Pinnwand",
                hint = "frei angeordnet, zuletzt bearbeitet zuerst",
                selected = selected == SortMode.PINNWAND,
            ) {
                haptics.tap()
                onPick(SortMode.PINNWAND)
            }
            SortRow(
                label = "Zuletzt erstellt",
                hint = "neueste Zettel zuerst",
                selected = selected == SortMode.CREATED,
            ) {
                haptics.tap()
                onPick(SortMode.CREATED)
            }
            SortRow(
                label = "Titel A–Z",
                hint = "alphabetisch nach Titel",
                selected = selected == SortMode.TITLE,
            ) {
                haptics.tap()
                onPick(SortMode.TITLE)
            }

            // Freies Ziehen: wechselt immer auf die Pinnwand (sonst würde die aktive
            // Sortierung die gezogene Reihenfolge sofort wieder überstimmen).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .paperPress(RoundedCornerShape(14.dp)) {
                        haptics.tap()
                        onArrange()
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PanTool,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(text = "Frei anordnen", style = MaterialTheme.typography.labelLarge, color = ink)
                    if (selected != SortMode.PINNWAND) {
                        Text(
                            text = "wechselt zur Pinnwand",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortRow(
    label: String,
    hint: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .paperPress(RoundedCornerShape(14.dp), onClick = onClick)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                RoundedCornerShape(14.dp),
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = ink)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        if (selected) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Ausgewählt",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
