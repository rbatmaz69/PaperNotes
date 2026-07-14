package com.papernotes.ui.components

import com.papernotes.ui.theme.PaperDimens
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.papernotes.ui.theme.PaperTheme
import com.papernotes.ui.theme.sheetItemEnter
import com.papernotes.util.rememberPaperHaptics

/**
 * Der „Einstellungen-Zettel": bündelt Papier-Theme, Sicherung und ein paar Zeilen
 * über die App in einem Bottom-Sheet – die App hat bewusst keinen Settings-Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    selectedTheme: PaperTheme,
    onPickTheme: (PaperTheme) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberPaperHaptics()
    val ink = MaterialTheme.colorScheme.onBackground
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PaperDimens.sheetHPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Einstellungen",
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )

            Text(
                text = "Papier wählen",
                style = MaterialTheme.typography.titleMedium,
                color = ink,
            )
            // Kein Lazy-Grid: die Column scrollt bereits, feste Reihen genügen.
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PaperTheme.selectable.chunked(4).forEachIndexed { rowIndex, rowThemes ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.sheetItemEnter(rowIndex),
                    ) {
                        rowThemes.forEach { theme ->
                            ThemeSwatch(
                                theme = theme,
                                selected = theme == selectedTheme,
                                onClick = {
                                    haptics.tap()
                                    onPickTheme(theme)
                                },
                            )
                        }
                    }
                }
            }

            Text(
                text = "Sicherung",
                style = MaterialTheme.typography.titleMedium,
                color = ink,
            )
            BackupRow(icon = Icons.Rounded.Save, label = "Sichern", modifier = Modifier.sheetItemEnter(3)) {
                haptics.tap()
                onExport()
            }
            BackupRow(icon = Icons.Rounded.FileOpen, label = "Importieren", modifier = Modifier.sheetItemEnter(4)) {
                haptics.tap()
                onImport()
            }

            Text(
                text = "Über",
                style = MaterialTheme.typography.titleMedium,
                color = ink,
                modifier = Modifier.padding(top = 8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "PaperNotes · Ausgabe $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink,
                )
                Text(
                    text = "Aus Papier, Tinte und ein bisschen Konfetti gefaltet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun BackupRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = modifier
            .fillMaxWidth()
            .paperPress(RoundedCornerShape(14.dp), onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ink)
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

@Composable
private fun ThemeSwatch(
    theme: PaperTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val description = if (selected) "${theme.label}, ausgewählt" else theme.label
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .paperPress(RoundedCornerShape(16.dp), onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (theme == PaperTheme.AUTO) {
                // Geteilter Kreis: hell (Tagespapier) / dunkel (Mitternacht)
                Canvas(
                    modifier = Modifier
                        .size(54.dp)
                        .then(
                            if (selected) Modifier.border(2.5.dp, ink, CircleShape) else Modifier,
                        ),
                ) {
                    val r = size.minDimension / 2f
                    drawCircle(PaperTheme.DAYLIGHT.paper, radius = r)
                    drawArc(
                        color = PaperTheme.MIDNIGHT.paper,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(0f, 0f),
                        size = size,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(theme.paper, CircleShape)
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) ink else theme.inkFaded.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (theme.dark || theme == PaperTheme.AUTO) Color.White else theme.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text = theme.label,
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
