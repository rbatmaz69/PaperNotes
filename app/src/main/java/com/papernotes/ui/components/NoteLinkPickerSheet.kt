package com.papernotes.ui.components

import com.papernotes.ui.theme.PaperDimens
import com.papernotes.ui.theme.PaperMotion
import com.papernotes.ui.theme.sheetItemEnter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.Note

private val ThreadRed = Color(0xFFB3402F)

/**
 * Auswahl-Sheet für den "roten Faden": listet die anderen Notizen auf. Ein Tipp
 * verknüpft sie mit der aktuellen Notiz (oder löst eine bestehende Verbindung wieder).
 * Bereits verbundene Notizen tragen einen roten Knoten.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteLinkPickerSheet(
    candidates: List<Note>,
    linkedIds: Set<Long>,
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
                text = "Roter Faden",
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )

            if (candidates.isEmpty()) {
                Text(
                    text = "Es gibt noch keine andere Notiz zum Verknüpfen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(candidates, key = { _, note -> note.id }) { index, note ->
                        val linked = note.id in linkedIds
                        LinkRow(
                            note = note,
                            linked = linked,
                            ink = ink,
                            onClick = { onToggle(note.id) },
                            modifier = Modifier.sheetItemEnter(index),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkRow(
    note: Note,
    linked: Boolean,
    ink: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Verknüpfungs-Zustand blendet weich über statt hart umzuschalten.
    val rowBg by animateColorAsState(
        targetValue = if (linked) ThreadRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(PaperMotion.DurMedium),
        label = "linkRowBg",
    )
    val knotFill by animateColorAsState(
        targetValue = if (linked) ThreadRed else ThreadRed.copy(alpha = 0f),
        animationSpec = tween(PaperMotion.DurMedium),
        label = "linkKnotFill",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (linked) 0f else 1f,
        animationSpec = tween(PaperMotion.DurMedium),
        label = "linkKnotRing",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .paperPress(RoundedCornerShape(14.dp), onClick = onClick)
            .background(rowBg, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Knoten: gefüllt, wenn verbunden – sonst ein leerer Ring.
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(knotFill, CircleShape)
                .border(
                    1.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = ringAlpha),
                    CircleShape,
                ),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = note.title.ifBlank { note.preview },
                style = MaterialTheme.typography.labelLarge,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
