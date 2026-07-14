package com.papernotes.ui.notes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.papernotes.ui.components.paperPress
import com.papernotes.ui.theme.sheetItemEnter

/** Phasen der Notiz-Fläche: erst laden, dann leer oder Grid. */
internal enum class GridPhase { LOADING, EMPTY, GRID }

/** Datumsstempel (JJJJ-MM-TT) für den vorgeschlagenen Sicherungs-Dateinamen. */
internal fun backupDateStamp(): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.GERMAN).format(java.util.Date())

/** Kleiner, runder Icon-Button für die obere Leiste (Suche / Archiv). */
@Composable
internal fun TopAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .paperPress(CircleShape, onClick = onClick)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Icon-Wechsel als kurzer „Stempel"-Pop statt hartem Austausch.
        AnimatedContent(
            targetState = icon,
            label = "topActionIcon",
            transitionSpec = {
                (fadeIn(tween(90)) + scaleIn(tween(90), initialScale = 0.7f)) togetherWith
                    (fadeOut(tween(60)) + scaleOut(tween(60)))
            },
        ) { current ->
            Icon(
                imageVector = current,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
internal fun EmptyState(modifier: Modifier = Modifier) {
    // Sanftes Schweben – wirkt lebendig statt statisch.
    val bob = rememberInfiniteTransition(label = "emptyBob")
    val phase by bob.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "bobPhase",
    )
    val amp = with(LocalDensity.current) { 6.dp.toPx() }
    Column(
        modifier = modifier
            .padding(32.dp)
            .graphicsLayer { translationY = phase * amp },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "🌼", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Noch nichts notiert",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Tippe auf +, um deine erste Notiz zu schreiben.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}

/** Hinweis, wenn Suche/Filter keine einzige Notiz treffen – statt nur blasser Tinte. */
@Composable
internal fun NoMatchState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.sheetItemEnter(0).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "🫥", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Keine Notiz passt",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Die Tinte bleibt blass – ändere Suche oder Filter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}
