package com.papernotes.ui.components

import com.papernotes.ui.theme.PaperDimens
import com.papernotes.ui.theme.PaperMotion
import com.papernotes.ui.theme.sheetItemEnter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.cardSurface
import com.papernotes.domain.model.earAccent
import com.papernotes.util.rememberPaperHaptics
import kotlinx.coroutines.launch

/**
 * Die "Schublade" des Schreibtischs: dezente Lasche am unteren Rand; Tap öffnet ein Sheet
 * mit zwei Fächern — **Archiv** (zurücklegen per Tap) und **Papierkorb** (zerknüllte
 * Notizen; Tap glättet die Papierkugel und stellt die Notiz wieder her).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerHandle(
    archiveCount: Int,
    trashCount: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (archiveCount == 0 && trashCount == 0) return
    val haptics = rememberPaperHaptics()

    Box(
        modifier = modifier
            .width(72.dp)
            .paperPress(RoundedCornerShape(8.dp)) {
                haptics.tap()
                onOpen()
            }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 5.dp)
                .background(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    RoundedCornerShape(3.dp),
                ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveDrawerSheet(
    archived: List<Note>,
    trashed: List<Note>,
    onRestoreArchived: (Long) -> Unit,
    onRestoreTrashed: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberPaperHaptics()
    var tab by rememberSaveable { mutableStateOf(0) }
    val ink = MaterialTheme.colorScheme.onBackground

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaperDimens.sheetHPadding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Fach-Umschalter
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                DrawerTab("Archiv (${archived.size})", tab == 0) { tab = 0 }
                DrawerTab("Papierkorb (${trashed.size})", tab == 1) { tab = 1 }
            }

            // Fach-Wechsel gleitet seitlich, passend zur Tab-Richtung.
            AnimatedContent(
                targetState = tab,
                label = "drawerTab",
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (
                        slideInHorizontally(
                            tween(PaperMotion.DurLong, easing = PaperMotion.EaseEmphasizedDecel),
                        ) { it / 6 * dir } +
                            fadeIn(tween(PaperMotion.DurLong, easing = PaperMotion.EaseEmphasizedDecel))
                    ) togetherWith fadeOut(tween(PaperMotion.DurShortExit))
                },
            ) { currentTab ->
                if (currentTab == 0) {
                    if (archived.isEmpty()) {
                        EmptyDrawerHint("Nichts archiviert.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(archived, key = { _, note -> note.id }) { index, note ->
                                ArchivedRow(
                                    note = note,
                                    onRestore = {
                                        haptics.confirm()
                                        onRestoreArchived(note.id)
                                    },
                                    modifier = Modifier.sheetItemEnter(index),
                                )
                            }
                        }
                    }
                } else {
                    if (trashed.isEmpty()) {
                        EmptyDrawerHint("Der Papierkorb ist leer.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Tippen glättet das Papier und legt die Notiz zurück. " +
                                    "Nach 30 Tagen wird endgültig gelöscht.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(72.dp),
                                modifier = Modifier.heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(trashed, key = { it.id }) { note ->
                                    CrumpledBall(
                                        note = note,
                                        onRestore = {
                                            haptics.confirm()
                                            onRestoreTrashed(note.id)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = if (tab == 0) "Tippen legt die Notiz zurück aufs Papier." else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun DrawerTab(label: String, active: Boolean, onClick: () -> Unit) {
    // Aktiv-Zustand blendet weich über.
    val tabAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.4f,
        animationSpec = tween(PaperMotion.DurMedium),
        label = "drawerTabAlpha",
    )
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .graphicsLayer { alpha = tabAlpha }
            .paperPress(RoundedCornerShape(8.dp), onClick = onClick),
    )
}

@Composable
private fun EmptyDrawerHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun ArchivedRow(note: Note, onRestore: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .paperPress(RoundedCornerShape(12.dp), onClick = onRestore)
            .background(note.mood.cardSurface(), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(note.mood.earAccent(), RoundedCornerShape(5.dp)),
        )
        Text(
            text = note.title.ifBlank { note.preview },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Eine zerknüllte Notiz im Papierkorb. Tap spielt die Glätt-Animation (Knüll-Fortschritt
 * 1 → 0 mit Spring) und ruft danach [onRestore].
 */
@Composable
private fun CrumpledBall(note: Note, onRestore: () -> Unit) {
    val scope = rememberCoroutineScope()
    val crumpleProgress = remember { Animatable(1f) }
    var restoring by remember { mutableStateOf(false) }
    val surface = note.mood.cardSurface()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(64.dp)
                .paperPress(RoundedCornerShape(percent = 50), enabled = !restoring) {
                    restoring = true
                    scope.launch {
                        crumpleProgress.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        )
                        onRestore()
                    }
                },
        ) {
            drawCrumpledPaper(surface, crumpleProgress.value)
        }
        Text(
            text = note.title.ifBlank { "Notiz" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(72.dp),
        )
    }
}
