package com.papernotes.ui.components

import com.papernotes.ui.theme.PaperDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.papernotes.ui.theme.Terracotta
import com.papernotes.ui.theme.sheetItemEnter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Bottom-Sheet für die „Zeitkapsel": versiegelt die Notiz bis zu einem Öffnungsdatum.
 * Schnell-Presets (1 Woche / 1 Monat / 1 Jahr) plus „Eigenes Datum …". Die Notiz öffnet
 * sich an dem Tag um 09:00 von selbst und meldet sich.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsuleSheet(
    currentCapsuleAt: Long?,
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
                .padding(horizontal = PaperDimens.sheetHPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "Zeitkapsel", style = MaterialTheme.typography.titleLarge, color = ink)
            Text(
                text = if (currentCapsuleAt != null) {
                    "Öffnet sich am ${formatDay(currentCapsuleAt)}"
                } else {
                    "Versiegelt die Notiz bis zu einem Tag – dann öffnet sie sich von selbst."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PresetRow("In 1 Woche", modifier = Modifier.sheetItemEnter(0)) { onPick(inDays(7)) }
            PresetRow("In 1 Monat", modifier = Modifier.sheetItemEnter(1)) { onPick(inMonths(1)) }
            PresetRow("In 1 Jahr", modifier = Modifier.sheetItemEnter(2)) { onPick(inYears(1)) }
            PresetRow("Eigenes Datum …", icon = Icons.Rounded.Schedule, modifier = Modifier.sheetItemEnter(3)) { showCustom = true }

            if (currentCapsuleAt != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paperPress(RoundedCornerShape(14.dp)) { onClear() }
                        .background(Terracotta.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.LockClock, contentDescription = null, tint = Terracotta)
                    Text(
                        text = "Zeitkapsel auflösen",
                        style = MaterialTheme.typography.labelLarge,
                        color = Terracotta,
                    )
                }
            }
        }
    }

    if (showCustom) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = currentCapsuleAt ?: inDays(7),
        )
        DatePickerDialog(
            onDismissRequest = { showCustom = false },
            confirmButton = {
                TextButton(onClick = {
                    val day = dateState.selectedDateMillis ?: inDays(7)
                    showCustom = false
                    onPick(atMorning(day))
                }) { Text("Versiegeln") }
            },
            dismissButton = { TextButton(onClick = { showCustom = false }) { Text("Abbrechen") } },
        ) {
            DatePicker(state = dateState)
        }
    }
}

@Composable
private fun PresetRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.LockClock,
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

private fun atMorning(dayMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = dayMillis
    set(Calendar.HOUR_OF_DAY, 9)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun inDays(days: Int): Long =
    atMorning(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }.timeInMillis)

private fun inMonths(months: Int): Long =
    atMorning(Calendar.getInstance().apply { add(Calendar.MONTH, months) }.timeInMillis)

private fun inYears(years: Int): Long =
    atMorning(Calendar.getInstance().apply { add(Calendar.YEAR, years) }.timeInMillis)

private fun formatDay(millis: Long): String =
    SimpleDateFormat("EEE, d. MMM yyyy", Locale.GERMAN).format(millis)
