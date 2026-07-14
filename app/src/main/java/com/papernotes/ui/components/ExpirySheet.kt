package com.papernotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.papernotes.ui.theme.Terracotta
import com.papernotes.ui.theme.sheetItemEnter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Bottom-Sheet zum Setzen einer Ablaufzeit (vergängliche Notiz): Schnell-Presets plus
 * "Eigene Zeit …". Zur Ablaufzeit zerknüllt sich die Notiz selbst in den Papierkorb.
 * Ist bereits eine Ablaufzeit gesetzt, lässt sie sich hier auch wieder aufheben.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpirySheet(
    currentExpiresAt: Long?,
    onPick: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    var showCustom by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Selbstzerstörung",
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )

            Text(
                text = if (currentExpiresAt != null) {
                    "Verfällt: ${formatExpiry(currentExpiresAt)}"
                } else {
                    "Wähle, wann sich diese Notiz von selbst zerknüllt."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PresetRow("In 1 Stunde", modifier = Modifier.sheetItemEnter(0)) { onPick(inHours(1)) }
            PresetRow("Heute Abend · 20:00", modifier = Modifier.sheetItemEnter(1)) { onPick(todayAt(20, 0)) }
            PresetRow("Morgen früh · 09:00", modifier = Modifier.sheetItemEnter(2)) { onPick(tomorrowAt(9, 0)) }
            PresetRow("In 1 Woche", modifier = Modifier.sheetItemEnter(3)) { onPick(inDays(7)) }
            PresetRow("Eigene Zeit …", icon = Icons.Rounded.Schedule, modifier = Modifier.sheetItemEnter(4)) { showCustom = true }

            if (currentExpiresAt != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paperPress(RoundedCornerShape(14.dp)) { onClear() }
                        .background(Terracotta.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestoreFromTrash,
                        contentDescription = null,
                        tint = Terracotta,
                    )
                    Text(
                        text = "Vergänglichkeit aufheben",
                        style = MaterialTheme.typography.labelLarge,
                        color = Terracotta,
                    )
                }
            }
        }
    }

    if (showCustom) {
        CustomExpiryPicker(
            initial = currentExpiresAt,
            onConfirm = {
                showCustom = false
                onPick(it)
            },
            onDismiss = { showCustom = false },
        )
    }
}

@Composable
private fun PresetRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.HourglassEmpty,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = modifier
            .fillMaxWidth()
            .paperPress(RoundedCornerShape(14.dp)) { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ink)
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

/** Zweistufige Auswahl: erst Datum, dann Uhrzeit; das Ergebnis fließt in [onConfirm]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomExpiryPicker(
    initial: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val base = initial ?: System.currentTimeMillis()
    var pickedDate by remember { mutableStateOf<Long?>(null) }

    if (pickedDate == null) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = base)
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { pickedDate = dateState.selectedDateMillis ?: base }) {
                    Text("Weiter")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(combine(pickedDate!!, timeState.hour, timeState.minute))
                }) { Text("Setzen") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
            title = {
                Text(
                    text = "Uhrzeit",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
        )
    }
}

// --- Zeit-Helfer ---

private fun inHours(hours: Int): Long = System.currentTimeMillis() + hours * 60L * 60L * 1000L

private fun inDays(days: Int): Long = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L

private fun todayAt(hour: Int, minute: Int): Long {
    val cal = atTime(Calendar.getInstance(), hour, minute)
    if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
    return cal.timeInMillis
}

private fun tomorrowAt(hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    return atTime(cal, hour, minute).timeInMillis
}

private fun combine(dateMillis: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    return atTime(cal, hour, minute).timeInMillis
}

private fun atTime(cal: Calendar, hour: Int, minute: Int): Calendar = cal.apply {
    set(Calendar.HOUR_OF_DAY, hour)
    set(Calendar.MINUTE, minute)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun formatExpiry(millis: Long): String =
    SimpleDateFormat("EEE, d. MMM · HH:mm", Locale.GERMAN).format(millis)
