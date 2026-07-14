package com.papernotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.papernotes.ui.components.paperPress

/** Wärmerer „Papier-Unterseiten"-Ton, in den die Rückseite eingefärbt wird. */
internal val SEPIA = Color(0xFFE8D7A0)

/** Antippbare Chips ("🧵 Titel") zu verknüpften Notizen – Tap springt direkt hinüber. */
@Composable
internal fun LinkedNoteChips(
    notes: List<com.papernotes.domain.model.Note>,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thread = Color(0xFFB3402F)
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        notes.forEach { linked ->
            Row(
                modifier = Modifier
                    .paperPress(RoundedCornerShape(50)) { onOpen(linked.id) }
                    .background(thread.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(thread, CircleShape),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = linked.title.ifBlank { linked.preview },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
internal fun TitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        textStyle = MaterialTheme.typography.headlineMedium.copy(color = ink),
        cursorBrush = SolidColor(ink),
        singleLine = true,
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        decorationBox = { inner ->
            if (title.isEmpty()) {
                Text(
                    text = "Titel",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            inner()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
    )
}

/**
 * Die Rückseite des Blatts: eine freie Textfläche auf dem etwas dunkleren „Unterseiten"-Ton
 * des Papiers – für Nachgedanken, Quellen oder eine Antwort, unabhängig vom Typ der Vorderseite.
 */
@Composable
internal fun BackEditor(
    text: String,
    onTextChange: (String) -> Unit,
    surface: Color,
    ink: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .background(surface, RoundedCornerShape(18.dp))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Text(
            text = "Rückseite",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
            cursorBrush = SolidColor(ink),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        text = "Notiz auf der Rückseite …",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                inner()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
        )
    }
}
