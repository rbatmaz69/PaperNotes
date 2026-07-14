package com.papernotes.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.earAccent
import com.papernotes.ui.theme.PaperMotion

/**
 * "Tinten-Suche": dezentes Suchfeld, das per Pull-down auf dem Grid hervorkommt.
 * Nicht-Treffer im Grid verblassen wie verdünnte Tinte (Dimming in [NoteCard]).
 */
@Composable
fun InkSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "🖋", style = MaterialTheme.typography.bodyLarge)
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
            cursorBrush = SolidColor(ink),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Suchen …",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                inner()
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        // Trefferfläche 44dp, das Symbol bleibt klein – große Zange, feiner Stift.
        Box(
            modifier = Modifier
                .size(44.dp)
                .paperPress(CircleShape) { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Suche schließen",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Schmale Punkteleiste zum Filtern nach Stimmung (Eselsohr-Farbe). Nur Stimmungen,
 * die tatsächlich vorkommen; der aktive Punkt wächst und bekommt einen Ring.
 */
@Composable
fun MoodFilterRow(
    presentMoods: List<MoodCategory>,
    active: MoodCategory?,
    onToggle: (MoodCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (presentMoods.size < 2) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presentMoods.forEach { mood ->
            val isActive = mood == active
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.35f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "moodDot",
            )
            // Ring blendet weich ein/aus statt hart zu schalten.
            val ringProgress by animateFloatAsState(
                targetValue = if (isActive) 1f else 0f,
                animationSpec = tween(PaperMotion.DurMedium),
                label = "moodRing",
            )
            val ringColor = MaterialTheme.colorScheme.onBackground
            // Trefferfläche 32dp um den kleinen Punkt; Semantik für Screenreader.
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .paperPress(CircleShape) { onToggle(mood) }
                    .semantics {
                        contentDescription = mood.label
                        selected = isActive
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .size(14.dp)
                        .background(mood.earAccent(), CircleShape)
                        .border(
                            width = 1.5.dp * ringProgress,
                            color = ringColor.copy(alpha = ringProgress),
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}
