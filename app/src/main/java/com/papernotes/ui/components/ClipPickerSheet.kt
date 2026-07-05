package com.papernotes.ui.components

import com.papernotes.ui.theme.PaperDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.Note

private val ClipSteel = Color(0xFF8A8F98)

/**
 * Auswahl-Sheet für die Büroklammer: listet die anderen Notizen auf. Ein Tipp klammert sie
 * an den Stapel der aktuellen Notiz (oder löst sie wieder). Bereits geklammerte Notizen
 * tragen eine gefüllte Klammer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipPickerSheet(
    candidates: List<Note>,
    clippedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "An Stapel klammern",
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )

            if (candidates.isEmpty()) {
                Text(
                    text = "Es gibt noch keine andere Notiz zum Klammern.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(candidates, key = { it.id }) { note ->
                        ClipRow(
                            note = note,
                            clipped = note.id in clippedIds,
                            ink = ink,
                            onClick = { onToggle(note.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipRow(
    note: Note,
    clipped: Boolean,
    ink: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .paperPress(RoundedCornerShape(14.dp), onClick = onClick)
            .background(
                if (clipped) ClipSteel.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(14.dp),
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Klammer: gefüllt-getönt, wenn geklammert – sonst nur ein leiser Ring um das Icon.
        Box(
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (clipped) {
                        Modifier.background(ClipSteel.copy(alpha = 0.25f), CircleShape)
                    } else {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AttachFile,
                contentDescription = null,
                tint = if (clipped) ink else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = note.title.ifBlank { note.preview },
            style = MaterialTheme.typography.labelLarge,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
