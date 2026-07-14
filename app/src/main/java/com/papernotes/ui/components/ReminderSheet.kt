package com.papernotes.ui.components

import com.papernotes.ui.theme.PaperDimens
import com.papernotes.ui.theme.PaperMotion
import com.papernotes.ui.theme.sheetItemEnter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.papernotes.R
import com.papernotes.domain.model.ReminderRule
import com.papernotes.ui.theme.Terracotta
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Bottom-Sheet zum Setzen einer Erinnerung: papierhafte Schnell-Presets
 * ("In 1 Stunde", "Heute Abend", "Morgen früh") plus "Eigene Zeit …" (Datum + Uhrzeit).
 * Ist bereits eine Erinnerung gesetzt, lässt sie sich hier auch wieder entfernen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
    currentReminderAt: Long?,
    currentRule: ReminderRule,
    onPick: (Long, ReminderRule) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    var showCustom by remember { mutableStateOf(false) }
    // Gewählte Wiederholung; gilt für alle Zeit-Auswahlen dieses Sheets.
    var rule by remember { mutableStateOf(currentRule) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaperDimens.sheetHPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Erinnerung",
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )

            // Benachrichtigungen systemweit aus? Dann ehrlich sagen, dass der Wecker stumm
            // bleibt – setzen darf man ihn trotzdem (die Agenda zeigt ihn ja).
            val context = LocalContext.current
            val notificationsOff = remember {
                !NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
            if (notificationsOff) {
                Text(
                    text = stringResource(R.string.reminder_muted_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            // Exakte Wecker entzogen? Dann fällt der Scheduler still auf ungefähre
            // Alarme zurück – das hier ist die einzige Stelle, die es dem Nutzer sagt.
            val exactUnavailable = remember {
                !(context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager)
                    .canScheduleExactAlarms()
            }
            if (exactUnavailable) {
                Text(
                    text = stringResource(R.string.reminder_inexact_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            if (currentReminderAt != null) {
                Text(
                    text = "Aktuell: ${formatReminder(currentReminderAt)}" +
                        if (currentRule != ReminderRule.NONE) " · ${currentRule.label.lowercase()}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Wiederholung: gilt für die anschließend gewählte Zeit.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sheetItemEnter(0)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReminderRule.entries.forEach { r ->
                    RuleChip(label = r.label, selected = r == rule) { rule = r }
                }
            }

            PresetRow("In 1 Stunde", modifier = Modifier.sheetItemEnter(1)) { onPick(inOneHour(), rule) }
            PresetRow("Heute Abend · 20:00", modifier = Modifier.sheetItemEnter(2)) { onPick(todayAt(20, 0), rule) }
            PresetRow("Morgen früh · 09:00", modifier = Modifier.sheetItemEnter(3)) { onPick(tomorrowAt(9, 0), rule) }

            PresetRow("Eigene Zeit …", icon = Icons.Rounded.Schedule, modifier = Modifier.sheetItemEnter(4)) { showCustom = true }

            if (currentReminderAt != null) {
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
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        tint = Terracotta,
                    )
                    Text(
                        text = "Erinnerung entfernen",
                        style = MaterialTheme.typography.labelLarge,
                        color = Terracotta,
                    )
                }
            }
        }
    }

    if (showCustom) {
        CustomDateTimePicker(
            initial = currentReminderAt,
            onConfirm = {
                showCustom = false
                onPick(it, rule)
            },
            onDismiss = { showCustom = false },
        )
    }
}

/** Kleiner Auswahl-Chip für die Wiederholungs-Regel. */
@Composable
private fun RuleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    // Auswahl blendet weich über statt hart umzuschalten.
    val bg by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(PaperMotion.DurMedium),
        label = "ruleChipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(PaperMotion.DurMedium),
        label = "ruleChipFg",
    )
    Box(
        modifier = Modifier
            .paperPress(shape) { onClick() }
            .background(bg, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun PresetRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.NotificationsActive,
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
private fun CustomDateTimePicker(
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

private fun inOneHour(): Long = System.currentTimeMillis() + 60 * 60 * 1000L

private fun todayAt(hour: Int, minute: Int): Long {
    val cal = atTime(Calendar.getInstance(), hour, minute)
    // Schon vorbei? Dann morgen zur selben Zeit.
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

private fun formatReminder(millis: Long): String =
    SimpleDateFormat("EEE, d. MMM · HH:mm", Locale.GERMAN).format(millis)
