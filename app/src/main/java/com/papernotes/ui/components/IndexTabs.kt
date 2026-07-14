package com.papernotes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.earAccent
import com.papernotes.ui.theme.PaperMotion

/**
 * Stabile Reiter-Farbe aus dem Tag-Namen: gleicher Name → immer dieselbe Farbe.
 * Greift auf die vorhandene Stimmungs-Palette zurück (Light/Dark-sicher), kein neuer Farbsatz.
 */
@Composable
@ReadOnlyComposable
fun tabColor(tag: String): Color {
    val palette = MoodCategory.entries.filter { it != MoodCategory.PLAIN }
    val idx = tag.lowercase().hashCode().mod(palette.size)
    return palette[idx].earAccent()
}

/** Lesbare Label-Farbe je nach Helligkeit des Reiter-Hintergrunds. */
private fun labelOn(bg: Color): Color =
    if (bg.luminance() > 0.5f) Color(0xFF2E2A26) else Color(0xFFF2ECE0)

private val TabShape = RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)

/**
 * Karteireiter, die seitlich aus der Karte ragen – gestapelt am rechten Rand.
 * Maximal drei werden gezeigt; bei mehr erscheint ein „+n"-Reiter.
 */
@Composable
fun IndexTabs(
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    val shown = tags.take(3)
    val extra = tags.size - shown.size

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        shown.forEach { tag -> Tab(label = tag, bg = tabColor(tag)) }
        if (extra > 0) Tab(label = "+$extra", bg = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
private fun Tab(label: String, bg: Color) {
    Box(
        modifier = Modifier
            .shadow(1.dp, TabShape, clip = false)
            .background(bg, TabShape)
            .widthIn(max = 92.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelOn(bg),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Filterleiste über dem Raster: ein Tipp auf einen Reiter blendet nicht passende Karten ab,
 * ein erneuter Tipp hebt den Filter wieder auf. Horizontal scrollbar bei vielen Reitern.
 */
@Composable
fun TagFilterRow(
    presentTags: List<String>,
    active: String?,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (presentTags.isEmpty()) return
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presentTags.forEach { tag ->
            val isActive = tag == active
            val bg = tabColor(tag)
            // Zustandswechsel weich: Füllung, Rahmen und Textfarbe blenden über,
            // die aktive Pill ploppt leicht auf statt hart umzuschalten.
            val fillAlpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.35f,
                animationSpec = tween(PaperMotion.DurMedium),
                label = "tagFill",
            )
            val borderProgress by animateFloatAsState(
                targetValue = if (isActive) 1f else 0f,
                animationSpec = tween(PaperMotion.DurMedium),
                label = "tagBorder",
            )
            val pillScale by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.94f,
                animationSpec = PaperMotion.SpringPop,
                label = "tagScale",
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) labelOn(bg) else MaterialTheme.colorScheme.onBackground,
                animationSpec = tween(PaperMotion.DurMedium),
                label = "tagText",
            )
            val borderColor = MaterialTheme.colorScheme.onBackground
            // Äußere Hitbox ≥36dp – die sichtbare Pill bleibt klein.
            Box(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .paperPress(shape) { onToggle(tag) }
                    .semantics {
                        contentDescription = "Reiter $tag"
                        this.selected = isActive
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pillScale
                            scaleY = pillScale
                        }
                        .background(bg.copy(alpha = fillAlpha), shape)
                        .border(
                            width = 1.5.dp * borderProgress,
                            color = borderColor.copy(alpha = borderProgress),
                            shape = shape,
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
