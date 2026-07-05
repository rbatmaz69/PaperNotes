package com.papernotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.earAccent
import com.papernotes.ui.theme.PaperDimens
import com.papernotes.util.rememberPaperHaptics

/**
 * Werkzeugleiste im Auswahlmodus: ersetzt die oberen Bedienelemente durch einen
 * Papierstreifen mit ✕, Zähler und den Stapel-Aktionen (Pinnen, Archiv, Papierkorb,
 * Stimmung, Karteireiter).
 */
@Composable
fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onMood: () -> Unit,
    onTag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(PaperDimens.actionCorner)
    Row(
        modifier = modifier
            .graphicsLayer { rotationZ = PaperDimens.TILT_SUBTLE }
            .shadow(5.dp, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionAction(Icons.Rounded.Close, "Auswahl beenden", onClose)
        Text(
            text = if (count == 1) "1 Zettel" else "$count Zettel",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.width(4.dp))
        SelectionAction(Icons.Rounded.PushPin, "Anheften", onPin)
        SelectionAction(Icons.Rounded.Mood, "Stimmung", onMood)
        SelectionAction(Icons.Rounded.Sell, "Karteireiter", onTag)
        SelectionAction(Icons.Rounded.Inventory2, "Archivieren", onArchive)
        SelectionAction(Icons.Rounded.Delete, "Zerknüllen", onTrash)
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(PaperDimens.touchTarget)
            .paperPress(CircleShape, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Kleine rote Heftzwecke als Auswahl-Marker auf der Karte. Bewusst keine Büroklammer –
 * die steht in der App schon für Stapel ([StackItem]).
 */
@Composable
fun SelectionPin(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val head = center.copy(y = center.y - size.height * 0.08f)
        // Nadel-Schatten schräg nach unten
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = head,
            end = Offset(head.x + size.width * 0.20f, head.y + size.height * 0.42f),
            strokeWidth = size.width * 0.09f,
            cap = StrokeCap.Round,
        )
        // Zweckenkopf mit Glanzlicht
        drawCircle(color = WaxRed, radius = size.minDimension * 0.30f, center = head)
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = size.minDimension * 0.09f,
            center = Offset(head.x - size.width * 0.09f, head.y - size.height * 0.09f),
        )
    }
}

/** Abgespecktes Stimmungs-Sheet für die Mehrfachauswahl: nur die Farbreihe, keine Extras. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodOnlySheet(
    onPick: (MoodCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberPaperHaptics()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaperDimens.sheetHPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Stimmung",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MoodCategory.entries.forEach { mood ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .paperPress(CircleShape) {
                                haptics.tick()
                                onPick(mood)
                            }
                            .background(mood.earAccent(), CircleShape)
                            .semantics { contentDescription = mood.label },
                    )
                }
            }
        }
    }
}
