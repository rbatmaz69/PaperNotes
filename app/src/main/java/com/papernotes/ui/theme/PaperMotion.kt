package com.papernotes.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState

/**
 * Zentrale Motion-Tokens im Geist von Material 3: kurze Exits, betont abbremsende
 * Entrances, Papier-typische Federn. Neue Animationen greifen hierauf zu, statt
 * Dauern/Easings erneut hart zu codieren.
 */
object PaperMotion {
    // Dauern (ms) — decken sich mit den bereits etablierten Werten der App.
    const val DurShort = 160
    const val DurShortExit = 120
    const val DurMedium = 220
    const val DurLong = 320
    const val DurWash = 450

    // M3-Easings (Standard / Emphasized) als Bezier-Näherung.
    val EaseStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseEmphasizedDecel = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EaseEmphasizedAccel = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // Federn — leichtes Nachfedern bei Gesten, keines bei Layoutverschiebungen.
    val SpringGentle = spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
    val SpringPaper = spring<Float>(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)
    val SpringPop = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
    val SpringPlacement =
        spring<IntOffset>(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
}

/**
 * Einmalige Entrance für Zeilen in Sheets/Listen: sanft von unten einblenden,
 * gestaffelt über [index]. Läuft nur beim ersten Aufbau der Komposition —
 * Datenänderungen spielen die Staffel nicht erneut ab.
 */
fun Modifier.sheetItemEnter(index: Int): Modifier = composed {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(
            durationMillis = PaperMotion.DurLong,
            delayMillis = (40 * index).coerceAtMost(200),
            easing = PaperMotion.EaseEmphasizedDecel,
        ),
        label = "sheetItemEnter",
    )
    graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 12.dp.toPx()
    }
}
