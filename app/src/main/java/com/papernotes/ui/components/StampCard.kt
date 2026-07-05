package com.papernotes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papernotes.domain.StampCodec
import com.papernotes.domain.StampMotif
import com.papernotes.util.rememberPaperHaptics
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val WEEKDAY_INITIALS = listOf("M", "D", "M", "D", "F", "S", "S")
private val MONTH_NAMES = listOf(
    "Januar", "Februar", "März", "April", "Mai", "Juni",
    "Juli", "August", "September", "Oktober", "November", "Dezember",
)

private val CELL = 34.dp
private val COMPACT_CELL = 22.dp

private val SLOT_DATE = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN)

/** Deutsche Bezeichnung des Motivs – für Screenreader auf den Canvas-Abdrücken. */
private val StampMotif.germanLabel: String
    get() = when (this) {
        StampMotif.CHECK -> "Haken"
        StampMotif.STAR -> "Stern"
        StampMotif.HEART -> "Herz"
        StampMotif.DROP -> "Tropfen"
        StampMotif.LEAF -> "Blatt"
        StampMotif.SUN -> "Sonne"
    }

/**
 * Eine Papier-Stempelkarte (Gewohnheit). Jeder erledigte Tag trägt einen leicht schiefen
 * Tinten-Abdruck im gewählten [motif]; leere Tage einen zarten Umriss, der heutige Slot lädt
 * sichtbar (pulsierend) zum Stempeln ein. Tippen setzt/entfernt den Stempel ([onToggleDay]) –
 * vergangene Tage lassen sich nachträglich stempeln, zukünftige bleiben gesperrt.
 *
 * [compact] = Grid-Vorschau (Strähne + letzte 7 Tage); sonst Editor-Ansicht: ein echter,
 * wochentags-ausgerichteter Monatskalender mit ‹ ›-Monatsnavigation.
 */
@Composable
fun StampCard(
    stamps: Set<Long>,
    motif: StampMotif,
    today: Long,
    accent: Color,
    onToggleDay: (Long) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val haptics = rememberPaperHaptics()
    val streak = StampCodec.streak(stamps, today)
    val total = StampCodec.total(stamps)
    val record = StampCodec.longestStreak(stamps)
    val todayStamped = today in stamps

    // Frischer Stempel senkt sich ein (Scale 1.6 → 1) – nur für den heutigen Slot.
    val pressScale = remember { Animatable(1f) }
    LaunchedEffect(todayStamped) {
        if (todayStamped) {
            pressScale.snapTo(1.6f)
            pressScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    // Sanftes Pulsieren des heutigen, noch leeren Slots. Nur dann die Endlos-Transition
    // erzeugen – ist heute bereits gestempelt, pulsiert nichts Sichtbares, also läuft pro
    // Stempelkarte auch keine ungenutzte Animation jeden Frame weiter.
    val pulse = if (!todayStamped) {
        rememberInfiniteTransition(label = "stampPulse").animateFloat(
            initialValue = 0.85f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "stampPulseScale",
        ).value
    } else {
        1f
    }

    // Heute/vergangenen Tag stempeln: satter „Thunk", beim Entfernen nur ein leiser Tick.
    fun toggle(day: Long) {
        if (day in stamps) haptics.tick() else haptics.stamp()
        onToggleDay(day)
    }

    @Composable
    fun slot(day: Long, cell: Dp, dayLabel: Int?, tappable: Boolean = true) {
        val isFuture = day > today
        StampSlot(
            stamped = day in stamps,
            isToday = day == today,
            isFuture = isFuture,
            accent = accent,
            outline = outline,
            slotSize = cell,
            rotationSeed = day,
            pressScale = pressScale.value,
            pulse = pulse,
            motif = motif,
            dayLabel = dayLabel,
            onStamp = if (tappable && !isFuture) ({ toggle(day) }) else null,
        )
    }

    // Strähne lebt noch (Anker gestern), aber heute ist nicht gestempelt → sanft mahnen.
    val todayOpen = streak > 0 && !todayStamped

    if (compact) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (streak > 0) "Strähne $streak" else "Noch keine Strähne",
                style = MaterialTheme.typography.titleSmall,
                color = if (todayOpen) inkOf(accent) else ink,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Nur der heutige Slot stempelt – vergangene Tage sind hier reine Anzeige,
                // damit ein Fehltipp auf der Pinnwand keine Historie zerstört (Nachstempeln
                // bewusst im Editor). Wochentags-Initialen geben den 7 Punkten ihren Bezug.
                (today - 6..today).forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val dow = LocalDate.ofEpochDay(day).dayOfWeek.value
                        Text(
                            text = WEEKDAY_INITIALS[dow - 1],
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (day == today) accent else outline.copy(alpha = 0.7f),
                        )
                        slot(day, COMPACT_CELL, null, tappable = day == today)
                    }
                }
            }
        }
        return
    }

    // --- Editor: echter Monatskalender ---
    val currentMonth = remember(today) { YearMonth.from(LocalDate.ofEpochDay(today)) }
    var month by remember { mutableStateOf(currentMonth) }
    val canNext = month < currentMonth

    BoxWithConstraints(modifier = modifier) {
        // Zellen füllen die Blattbreite (größere Trefferfläche), bleiben aber gedeckelt.
        val cell = ((maxWidth - 4.dp * 6) / 7).coerceIn(CELL, 48.dp)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Kopf: Monatsnavigation + Rücksprung zu heute
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NavArrow(glyph = "‹", accent = accent, enabled = true) { month = month.minusMonths(1) }
            Text(
                text = "${MONTH_NAMES[month.monthValue - 1]} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                color = ink,
            )
            NavArrow(glyph = "›", accent = accent, enabled = canNext) {
                if (canNext) month = month.plusMonths(1)
            }
            if (month != currentMonth) {
                Box(
                    modifier = Modifier
                        .paperPress(RoundedCornerShape(50)) { month = currentMonth }
                        .background(accent.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "Heute",
                        style = MaterialTheme.typography.labelLarge,
                        color = inkOf(accent),
                    )
                }
            }
        }

        // Stat-Zeile: Strähne · heute offen · bis Konfetti · Rekord · gesamt
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (streak > 0) "Strähne $streak" else "Noch keine Strähne",
                style = MaterialTheme.typography.titleSmall,
                color = if (todayOpen) inkOf(accent) else ink,
            )
            val extra = buildList {
                if (todayOpen) add("heute noch offen")
                if (streak > 0 && streak % 7L != 0L) add("noch ${7 - streak % 7} bis Konfetti")
                if (record > 0) add("Rekord $record")
                if (total > 0) add("$total gesamt")
            }
            if (extra.isNotEmpty()) {
                Text(
                    text = "· ${extra.joinToString(" · ")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = outline,
                )
            }
        }

        // Wochentags-Kopfzeile (Montag-first)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WEEKDAY_INITIALS.forEach { letter ->
                Box(modifier = Modifier.size(cell), contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelSmall,
                        color = outline,
                    )
                }
            }
        }

        // Tageszellen: führende Leerzellen bis zum 1. Wochentag, dann 1..lengthOfMonth.
        val firstDow = month.atDay(1).dayOfWeek.value // 1 = Montag … 7 = Sonntag
        val cells: List<Int?> = List(firstDow - 1) { null } + (1..month.lengthOfMonth()).toList()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            cells.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { d ->
                        if (d == null) {
                            Spacer(Modifier.size(cell))
                        } else {
                            slot(month.atDay(d).toEpochDay(), cell, d)
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.size(cell)) }
                }
            }
        }
        }
    }
}

/** Horizontale „Stempel-Schublade": jedes Motiv als kleiner Tinten-Abdruck; das aktive hervorgehoben. */
@Composable
fun StampMotifPicker(
    selected: StampMotif,
    accent: Color,
    onPick: (StampMotif) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    val ink = inkOf(accent)
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StampMotif.entries.forEach { m ->
            val isSelected = m == selected
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .paperPress(CircleShape) { onPick(m) }
                    .semantics {
                        contentDescription = m.germanLabel
                        this.selected = isSelected
                    }
                    .then(
                        if (isSelected) Modifier.border(2.dp, accent, CircleShape) else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .size(34.dp)
                        .alpha(if (isSelected) 1f else 0.55f),
                ) {
                    drawStampImpression(m, accent, ink, outline, tilt = -4f, scale = 1f)
                }
            }
        }
    }
}

@Composable
private fun NavArrow(
    glyph: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .then(if (enabled) Modifier.paperPress(CircleShape, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun StampSlot(
    stamped: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    accent: Color,
    outline: Color,
    slotSize: Dp,
    rotationSeed: Long,
    pressScale: Float,
    pulse: Float,
    motif: StampMotif,
    dayLabel: Int?,
    onStamp: (() -> Unit)?,
) {
    // Pro Tag leicht andere Stempel-Drehung – wirkt handgestempelt.
    val tilt = ((rotationSeed * 37L) % 11L - 5L).toFloat()
    val ink = inkOf(accent)
    // rotationSeed ist der Epoch-Tag → daraus die Ansage für Screenreader bauen.
    val slotDescription = buildString {
        append(if (isToday) "Heute" else LocalDate.ofEpochDay(rotationSeed).format(SLOT_DATE))
        append(", ")
        append(
            when {
                stamped -> "gestempelt"
                isFuture -> "liegt in der Zukunft"
                else -> "nicht gestempelt"
            },
        )
    }

    Box(
        modifier = Modifier
            .size(slotSize)
            .then(if (onStamp != null) Modifier.paperPress(CircleShape, onClick = onStamp) else Modifier)
            .semantics { contentDescription = slotDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(slotSize)) {
            val r = size.minDimension / 2f
            val c = center
            when {
                stamped -> drawStampImpression(
                    motif, accent, ink, outline,
                    tilt = tilt,
                    scale = if (isToday) pressScale else 1f,
                )

                isToday -> {
                    // Heutiger Slot: kräftiger Akzent-Ring (pulsierend).
                    drawCircle(color = accent.copy(alpha = 0.9f), radius = r * 0.8f * pulse, style = Stroke(width = 3.5f))
                    if (dayLabel == null) {
                        // Kompaktansicht ohne Tageszahl: „＋"-Hinweis zum Stempeln.
                        val a = r * 0.34f
                        drawLine(accent, Offset(c.x - a, c.y), Offset(c.x + a, c.y), strokeWidth = 3.5f, cap = StrokeCap.Round)
                        drawLine(accent, Offset(c.x, c.y - a), Offset(c.x, c.y + a), strokeWidth = 3.5f, cap = StrokeCap.Round)
                    }
                }

                else -> {
                    // Leerer Tag: dezenter Ring, Zukunft noch blasser.
                    drawCircle(
                        color = outline.copy(alpha = if (isFuture) 0.2f else 0.45f),
                        radius = r * 0.78f,
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f), 0f),
                        ),
                    )
                }
            }
        }

        // Tageszahl im Kalender: auf leeren Tagen mittig, auf gestempelten klein in der
        // Ecke – so bleibt beim Nachstempeln/Prüfen die Orientierung erhalten.
        if (dayLabel != null) {
            if (!stamped) {
                Text(
                    text = dayLabel.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isToday -> accent
                        isFuture -> outline.copy(alpha = 0.4f)
                        else -> outline
                    },
                )
            } else {
                Text(
                    text = dayLabel.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = ink.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

/** Dunklere „Tinte" zur Akzentfarbe – für die Stempel-Motive auf der hellen Abdruck-Fläche. */
private fun inkOf(accent: Color): Color =
    Color(accent.red * 0.55f, accent.green * 0.55f, accent.blue * 0.55f, 1f)

/** Zeichnet einen kompletten Tinten-Abdruck: schiefe Akzent-Fläche, definierender Rand, Motiv. */
private fun DrawScope.drawStampImpression(
    motif: StampMotif,
    accent: Color,
    ink: Color,
    outline: Color,
    tilt: Float,
    scale: Float,
) {
    val r = size.minDimension / 2f
    rotate(degrees = tilt) {
        val rad = r * 0.84f * scale
        drawCircle(color = accent, radius = rad)
        // Definierender Rand – sichtbar auch bei blassen Stimmungsfarben (PLAIN).
        drawCircle(color = outline.copy(alpha = 0.55f), radius = rad, style = Stroke(width = 2f))
        drawMotif(motif, ink, accent, r, scale)
    }
}

/** Zeichnet das Stempel-Motiv (Tinte) zentriert in den Abdruck. */
private fun DrawScope.drawMotif(
    motif: StampMotif,
    ink: Color,
    accent: Color,
    r: Float,
    scale: Float,
) {
    val c = center
    val rad = r * 0.5f * scale
    when (motif) {
        StampMotif.CHECK -> {
            drawLine(ink, Offset(c.x - rad * 0.55f, c.y + rad * 0.05f), Offset(c.x - rad * 0.1f, c.y + rad * 0.5f), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
            drawLine(ink, Offset(c.x - rad * 0.1f, c.y + rad * 0.5f), Offset(c.x + rad * 0.6f, c.y - rad * 0.5f), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
        }

        StampMotif.STAR -> drawPath(starPath(c, rad, rad * 0.42f), ink)

        StampMotif.HEART -> {
            val lobeR = rad * 0.52f
            val cy = c.y - rad * 0.15f
            drawCircle(ink, lobeR, Offset(c.x - rad * 0.42f, cy))
            drawCircle(ink, lobeR, Offset(c.x + rad * 0.42f, cy))
            val p = Path().apply {
                moveTo(c.x - rad * 0.9f, cy + rad * 0.05f)
                lineTo(c.x + rad * 0.9f, cy + rad * 0.05f)
                lineTo(c.x, c.y + rad * 0.95f)
                close()
            }
            drawPath(p, ink)
        }

        StampMotif.DROP -> {
            drawCircle(ink, rad * 0.62f, Offset(c.x, c.y + rad * 0.28f))
            val p = Path().apply {
                moveTo(c.x, c.y - rad * 0.85f)
                lineTo(c.x - rad * 0.6f, c.y + rad * 0.3f)
                lineTo(c.x + rad * 0.6f, c.y + rad * 0.3f)
                close()
            }
            drawPath(p, ink)
        }

        StampMotif.LEAF -> {
            val p = Path().apply {
                moveTo(c.x, c.y - rad)
                quadraticTo(c.x + rad * 0.95f, c.y, c.x, c.y + rad)
                quadraticTo(c.x - rad * 0.95f, c.y, c.x, c.y - rad)
                close()
            }
            drawPath(p, ink)
            // Mittelrippe in Akzentfarbe – wie eine Blattader.
            drawLine(accent, Offset(c.x, c.y - rad * 0.7f), Offset(c.x, c.y + rad * 0.7f), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
        }

        StampMotif.SUN -> {
            drawCircle(ink, rad * 0.5f, c)
            val rays = 8
            for (i in 0 until rays) {
                val a = i * (2.0 * PI / rays)
                val sx = c.x + rad * 0.66f * cos(a).toFloat()
                val sy = c.y + rad * 0.66f * sin(a).toFloat()
                val ex = c.x + rad * cos(a).toFloat()
                val ey = c.y + rad * sin(a).toFloat()
                drawLine(ink, Offset(sx, sy), Offset(ex, ey), strokeWidth = r * 0.1f, cap = StrokeCap.Round)
            }
        }
    }
}

private fun starPath(c: Offset, outer: Float, inner: Float): Path {
    val p = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) outer else inner
        val ang = -PI / 2.0 + i * PI / 5.0
        val x = c.x + rad * cos(ang).toFloat()
        val y = c.y + rad * sin(ang).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}
