package com.papernotes.ui.agenda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.cardSurface
import com.papernotes.domain.model.earAccent
import androidx.compose.animation.core.tween
import com.papernotes.ui.components.PaperBackground
import com.papernotes.ui.components.paperPress
import com.papernotes.ui.theme.PaperMotion
import com.papernotes.ui.theme.sheetItemEnter
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AgendaScreen(
    onOpenNote: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AgendaViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val ink = MaterialTheme.colorScheme.onBackground

    // System-Zurück (Geste/Button) führt zurück zur Startseite statt aus der App heraus.
    BackHandler { onBack() }
    // Statusleisten-/Notch-Höhe oben freihalten, damit Zurück-Pfeil und Titel nicht unter
    // die Systemleiste rutschen (wie in NotesScreen/EditorScreen).
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    PaperBackground(dotGrid = true) {
        Column(modifier = Modifier.fillMaxSize().padding(top = topInset)) {
            // Kopfzeile: Zurück + Titel.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Zurück",
                        tint = ink,
                    )
                }
                Text(
                    text = "Schreibtisch-Agenda",
                    style = MaterialTheme.typography.titleLarge,
                    color = ink,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            if (sections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nichts steht an.\nNotizen mit Erinnerung, Countdown oder Ablauf erscheinen hier.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 40.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Laufender Index über alle Zeilen für die gestaffelte Entrance.
                    var stagger = 0
                    sections.forEach { section ->
                        val headerIndex = stagger++
                        item(key = "h-${section.title}") {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = tween(PaperMotion.DurShort),
                                        placementSpec = PaperMotion.SpringPlacement,
                                        fadeOutSpec = tween(PaperMotion.DurShortExit),
                                    )
                                    .sheetItemEnter(headerIndex)
                                    .padding(top = 12.dp, bottom = 2.dp),
                            )
                        }
                        val firstEntryIndex = stagger
                        stagger += section.entries.size
                        items(
                            section.entries,
                            key = { "${section.title}-${it.note.id}-${it.kind}" },
                        ) { entry ->
                            val entryIndex = firstEntryIndex + section.entries.indexOf(entry)
                            AgendaCard(
                                entry = entry,
                                onClick = { onOpenNote(entry.note.id) },
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = tween(PaperMotion.DurShort),
                                        placementSpec = PaperMotion.SpringPlacement,
                                        fadeOutSpec = tween(PaperMotion.DurShortExit),
                                    )
                                    .sheetItemEnter(entryIndex),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaCard(entry: AgendaEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val note = entry.note
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .paperPress(shape, onClick = onClick)
            .background(note.mood.cardSurface(), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Badge ploppt beim Erscheinen kurz auf (0.6 → 1 mit Feder).
        var badgeIn by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { badgeIn = true }
        val badgeScale by animateFloatAsState(
            targetValue = if (badgeIn) 1f else 0.6f,
            animationSpec = PaperMotion.SpringPop,
            label = "agendaBadgePop",
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer {
                    scaleX = badgeScale
                    scaleY = badgeScale
                }
                .background(note.mood.earAccent().copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.kind.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = note.title.ifBlank { note.preview },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.timeLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private fun AgendaKind.icon(): ImageVector = when (this) {
    AgendaKind.REMINDER -> Icons.Rounded.NotificationsActive
    AgendaKind.COUNTDOWN -> Icons.Rounded.CalendarMonth
    AgendaKind.EXPIRY -> Icons.Rounded.HourglassEmpty
}

private val reminderFormat = SimpleDateFormat("EEE, d. MMM · HH:mm", Locale.GERMAN)
private val dateFormat = SimpleDateFormat("EEE, d. MMM", Locale.GERMAN)

private fun AgendaEntry.timeLabel(): String = when (kind) {
    AgendaKind.REMINDER -> "🔔 " + reminderFormat.format(at)
    AgendaKind.COUNTDOWN -> {
        val days = note.daysUntil(System.currentTimeMillis())
        val rel = when {
            days == null -> ""
            days == 0L -> "heute"
            days == 1L -> "morgen"
            days > 1L -> "noch $days Tage"
            else -> "vorbei"
        }
        "📅 $rel · " + dateFormat.format(at)
    }
    AgendaKind.EXPIRY -> "⌛ läuft " + expiryLabel(at - System.currentTimeMillis()) + " ab"
}

private fun expiryLabel(remainingMs: Long): String {
    if (remainingMs <= 60_000L) return "gleich"
    val minutes = remainingMs / 60_000L
    return when {
        minutes < 60 -> "in $minutes Min"
        minutes < 60 * 24 -> "in ${minutes / 60} Std"
        else -> "in ${minutes / (60 * 24)} Tg"
    }
}
