package com.papernotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.PaperStyle
import com.papernotes.domain.model.earAccent
import com.papernotes.ui.theme.PaperDimens
import com.papernotes.ui.theme.Terracotta

/**
 * Bottom-Sheet als Kontext-Menü einer Notiz. Statt einer langen Liste sind die
 * Aktionen in kleine Abschnitte gruppiert (Ordnen / Zeit / Extras / Teilen) und
 * stehen paarweise nebeneinander – ein Zettel mit Rubriken, kein Menübaum.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodPickerSheet(
    selected: MoodCategory,
    pinned: Boolean,
    hasReminder: Boolean,
    sealed: Boolean,
    invisibleInk: Boolean,
    done: Boolean,
    hasExpiry: Boolean,
    hasCountdown: Boolean,
    hasCapsule: Boolean,
    hasPhoto: Boolean,
    paper: PaperStyle,
    onPick: (MoodCategory) -> Unit,
    onPickPaper: (PaperStyle) -> Unit,
    onTogglePin: () -> Unit,
    onSetReminder: () -> Unit,
    onSetCountdown: () -> Unit,
    onAttachPhoto: () -> Unit,
    onLink: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onClip: (() -> Unit)? = null,
    onToggleSeal: () -> Unit,
    onSetCapsule: () -> Unit,
    onToggleInvisibleInk: () -> Unit,
    onToggleDone: () -> Unit,
    onEditTags: () -> Unit,
    onSetExpiry: () -> Unit,
    onDelete: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PaperDimens.sheetHPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Stimmung",
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MoodCategory.entries.forEach { mood ->
                    val isSelected = mood == selected
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .paperPress(CircleShape) { onPick(mood) }
                            .semantics {
                                contentDescription = mood.label
                                this.selected = isSelected
                            }
                            .background(mood.earAccent(), CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.5.dp, ink, CircleShape)
                                } else Modifier,
                            ),
                    )
                }
            }

            // Papier-Liniierung wählen (Live-Vorschau je Swatch)
            SectionLabel("Papier")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PaperStyle.entries.forEach { style ->
                    val isSelected = style == paper
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .paperPress(RoundedCornerShape(8.dp)) { onPickPaper(style) }
                            .semantics {
                                contentDescription = style.label
                                this.selected = isSelected
                            }
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp),
                            )
                            .paperRuling(style, ink, spacing = 9.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.5.dp, ink, RoundedCornerShape(8.dp))
                                } else Modifier,
                            ),
                    )
                }
            }

            SectionLabel("Ordnen")
            ActionPair(
                first = {
                    SheetAction(
                        icon = Icons.Rounded.PushPin,
                        label = if (pinned) "Washi-Tape lösen" else "Anheften",
                        onClick = onTogglePin,
                        modifier = it,
                    )
                },
                second = {
                    SheetAction(
                        icon = Icons.Rounded.LocalOffer,
                        label = "Reiter / Tags",
                        onClick = onEditTags,
                        modifier = it,
                    )
                },
            )
            if (onClip != null) {
                ActionPair(
                    first = {
                        SheetAction(
                            icon = Icons.Rounded.Hub,
                            label = "Roter Faden",
                            onClick = onLink,
                            modifier = it,
                        )
                    },
                    second = {
                        SheetAction(
                            icon = Icons.Rounded.AttachFile,
                            label = "An Stapel klammern",
                            onClick = onClip,
                            modifier = it,
                        )
                    },
                )
            } else {
                SheetAction(
                    icon = Icons.Rounded.Hub,
                    label = "Mit rotem Faden verknüpfen",
                    onClick = onLink,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionLabel("Zeit")
            ActionPair(
                first = {
                    SheetAction(
                        icon = Icons.Rounded.NotificationsActive,
                        label = if (hasReminder) "Erinnerung ändern" else "Erinnerung setzen",
                        onClick = onSetReminder,
                        modifier = it,
                    )
                },
                second = {
                    SheetAction(
                        icon = Icons.Rounded.CalendarMonth,
                        label = if (hasCountdown) "Termin ändern" else "Countdown setzen",
                        onClick = onSetCountdown,
                        modifier = it,
                    )
                },
            )
            ActionPair(
                first = {
                    SheetAction(
                        icon = Icons.Rounded.Schedule,
                        label = if (hasCapsule) "Zeitkapsel ändern" else "Zeitkapsel",
                        onClick = onSetCapsule,
                        modifier = it,
                    )
                },
                second = {
                    SheetAction(
                        icon = Icons.Rounded.HourglassEmpty,
                        label = if (hasExpiry) "Selbstzerstörung ändern" else "Vergänglich machen",
                        onClick = onSetExpiry,
                        modifier = it,
                    )
                },
            )

            SectionLabel("Extras")
            ActionPair(
                first = {
                    SheetAction(
                        icon = Icons.Rounded.Lock,
                        label = if (sealed) "Siegel entfernen" else "Versiegeln",
                        onClick = onToggleSeal,
                        modifier = it,
                    )
                },
                second = {
                    SheetAction(
                        icon = Icons.Rounded.Whatshot,
                        label = if (invisibleInk) "Geheimtinte entfernen" else "Geheimtinte",
                        onClick = onToggleInvisibleInk,
                        modifier = it,
                    )
                },
            )
            ActionPair(
                first = {
                    SheetAction(
                        icon = Icons.Rounded.TaskAlt,
                        label = if (done) "Stempel entfernen" else "Erledigt stempeln",
                        onClick = onToggleDone,
                        modifier = it,
                    )
                },
                second = {
                    SheetAction(
                        icon = Icons.Rounded.AddAPhoto,
                        label = if (hasPhoto) "Foto ersetzen" else "Foto anhängen",
                        onClick = onAttachPhoto,
                        modifier = it,
                    )
                },
            )

            SectionLabel("Teilen")
            ActionPair(
                first = {
                    SheetAction(
                        icon = Icons.AutoMirrored.Rounded.Send,
                        label = "Papierflieger",
                        onClick = onShare,
                        modifier = it,
                    )
                },
                second = {
                    SheetAction(
                        icon = Icons.Rounded.ContentCopy,
                        label = "Text kopieren",
                        onClick = onCopy,
                        modifier = it,
                    )
                },
            )

            SheetAction(
                icon = Icons.Rounded.Delete,
                label = "Zerknüllen & in den Papierkorb",
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                tint = Color(0xFFC97B5F),
                container = Terracotta.copy(alpha = 0.18f),
            )
        }
    }
}

/** Kleine Rubrik-Überschrift, wie eine Bleistift-Notiz am Rand. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Zwei halbbreite Aktionen nebeneinander, gleich hoch – halbiert die Scroll-Länge. */
@Composable
private fun ActionPair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        first(Modifier.weight(1f).fillMaxHeight())
        second(Modifier.weight(1f).fillMaxHeight())
    }
}

/** Einheitliche Aktions-Zeile: Symbol + Beschriftung auf einem Papier-Plättchen. */
@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val shape = RoundedCornerShape(PaperDimens.actionCorner)
    Row(
        modifier = modifier
            .paperPress(shape) { onClick() }
            .background(container, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
        )
    }
}
