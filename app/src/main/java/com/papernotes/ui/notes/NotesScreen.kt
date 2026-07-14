package com.papernotes.ui.notes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.papernotes.R
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import com.papernotes.domain.model.cardSurface
import com.papernotes.domain.toShareText
import com.papernotes.ui.components.AddFab
import com.papernotes.ui.components.CrumpleRequest
import com.papernotes.ui.components.DrawerHandle
import com.papernotes.ui.components.InkSearchBar
import com.papernotes.ui.components.MoodFilterRow
import com.papernotes.ui.components.SelectionBar
import com.papernotes.ui.components.SelectionPin
import com.papernotes.ui.components.TagFilterRow
import com.papernotes.ui.components.NoteCard
import com.papernotes.ui.components.NoteStack
import com.papernotes.ui.components.PaperBackground
import com.papernotes.ui.components.PaperPlaneRequest
import com.papernotes.ui.components.PaperSnackbar
import com.papernotes.ui.components.RedThreadOverlay
import com.papernotes.ui.components.paperPress
import com.papernotes.ui.components.TeabagPull
import com.papernotes.ui.components.WaxRed
import com.papernotes.ui.components.WaxSealBreakRequest
import com.papernotes.ui.theme.PaperDimens
import com.papernotes.ui.theme.PaperMotion
import com.papernotes.ui.theme.ThemeViewModel
import com.papernotes.util.PhotoStore
import com.papernotes.util.ShareCardRenderer
import com.papernotes.util.rememberPaperHaptics
import com.papernotes.util.shareImage
import com.papernotes.util.sharePlainText
import kotlin.math.sin

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NotesScreen(
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    onOpenNote: (Long) -> Unit,
    onCreateNote: (NoteType) -> Unit,
    onOpenAgenda: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val undoEvent by viewModel.undoEvent.collectAsStateWithLifecycle()
    val longPressHint by viewModel.longPressHint.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val currentTheme by themeViewModel.theme.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Leichter Zeit-Ticker: lässt fällige Erinnerungen live ins „Flattern" übergehen.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(30_000)
        }
    }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Ergebnis egal: Alarm läuft, Notification erscheint erst nach Erteilung. */ }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val haptics = rememberPaperHaptics()
    val columns by viewModel.gridColumns.collectAsStateWithLifecycle()
    var zoomAccum by remember { mutableFloatStateOf(1f) }
    var columnsChangedAt by remember { mutableStateOf(0L) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        zoomAccum *= zoomChange
        val next = when {
            zoomAccum > 1.25f -> (columns - 1).coerceAtLeast(1)
            zoomAccum < 0.8f -> (columns + 1).coerceAtMost(3)
            else -> columns
        }
        if (next != columns) {
            zoomAccum = 1f
            haptics.fold()
            columnsChangedAt = System.currentTimeMillis()
            viewModel.setGridColumns(next)
        } else if (zoomAccum > 1.25f || zoomAccum < 0.8f) {
            // Am Anschlag (1 bzw. 3 Spalten): Akku kappen, damit die Gegenrichtung
            // nicht erst den ganzen angesammelten Zoom abtragen muss.
            zoomAccum = zoomAccum.coerceIn(0.8f, 1.25f)
        }
    }
    // Restzoom einer beendeten Geste verwerfen – sonst schaltet der nächste Pinch
    // sofort oder in die falsche Richtung.
    LaunchedEffect(transformState.isTransformInProgress) {
        if (!transformState.isTransformInProgress) zoomAccum = 1f
    }

    // Tinten-Suche: ausschließlich über das Lupen-Icon (kein Pull-down mehr).
    var searchVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = searchVisible) {
        searchVisible = false
        viewModel.clearSearch()
    }

    // Position jeder Karte (Root-Koordinaten) – für die Knüddel-Animation UND die roten
    // Fäden. Snapshot-Map, damit das Faden-Overlay beim Scrollen/Zoomen live nachzeichnet.
    val cardBounds = remember { mutableStateMapOf<Long, Rect>() }
    val overlays = remember { NotesOverlayState() }
    val sheets = remember { NotesSheetState() }

    // Foto-Picker (System-Auswahl, keine Berechtigung): wählt ein Bild für photoTarget.
    val scope = rememberCoroutineScope()
    var photoTarget by remember { mutableStateOf<Note?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = photoTarget
        photoTarget = null
        if (uri != null && target != null) {
            scope.launch {
                val name = PhotoStore.save(context, uri)
                if (name != null) {
                    target.photoPath?.let { PhotoStore.delete(context, it) }
                    viewModel.setPhoto(target, name)
                }
            }
        }
    }

    // Sicherung: ZIP schreiben/lesen über die System-Dateiauswahl (keine Berechtigung nötig).
    fun backupToast(count: Int, exporting: Boolean) {
        val msg = when {
            count < 0 -> "Sicherung fehlgeschlagen"
            exporting -> "$count Notizen gesichert"
            else -> "$count Notizen importiert"
        }
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let { viewModel.exportBackup(context, it) { n -> backupToast(n, exporting = true) } } }
    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importBackup(context, it) { n -> backupToast(n, exporting = false) } } }

    // Welche Karten schon „eingeflogen" sind – neue Notizen fallen einmalig herein.
    val introducedIds = remember { mutableStateMapOf<Long, Unit>() }
    var introSeeded by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // Gedimmte (Such-/Filter-Nicht-Treffer) Ids nur neu berechnen, wenn sich die Notizen
    // ändern – nicht bei jedem 30-s-Tick oder jeder Bounds-Aktualisierung.
    val dimmedIds by remember {
        derivedStateOf { state.notes.filter { it.dimmed }.map { it.note.id }.toSet() }
    }

    // Stempel-Meilenstein → Konfetti.
    LaunchedEffect(Unit) {
        viewModel.stampMilestone.collect {
            overlays.stampConfettiKey++
            overlays.showStampConfetti = true
        }
    }
    var fabExpanded by remember { mutableStateOf(false) }

    // Mehrfachauswahl: Long-Press auf eine Karte betritt den Modus, Tap toggelt weitere.
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val selectionMode = selectedIds.isNotEmpty()

    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }

    // Pinnwand: Anordnen-Modus zum freien Umsortieren der Karten.
    var arrangeMode by remember { mutableStateOf(false) }
    // Position jedes Grid-Items (Solo & Stapel) in Root-Koordinaten – für die Ziel-Erkennung.
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    // Lokale Reihenfolge nur während des Modus (der Flow überschreibt nicht mitten im Ziehen).
    var orderedItems by remember { mutableStateOf<List<GridItem>>(emptyList()) }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var fingerAbs by remember { mutableStateOf(Offset.Zero) }
    // Leichtes Wackeln als Modus-Signal – nur im Anordnen-Modus erzeugen, damit die
    // Endlos-Animation außerhalb nicht jeden Frame ungenutzt weiterläuft. Der Wert wird
    // unten in der graphicsLayer (Draw-Phase) gelesen, daher hier als State halten.
    val jiggle = if (arrangeMode) {
        rememberInfiniteTransition(label = "jiggle").animateFloat(
            initialValue = 0f,
            targetValue = 6.2831855f,
            animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
            label = "jigglePhase",
        )
    } else null

    fun finishArrange() {
        if (arrangeMode) {
            viewModel.applyOrder(orderedItems)
            arrangeMode = false
            draggingKey = null
        }
    }

    // Beim Betreten des Modus die aktuelle Reihenfolge übernehmen.
    LaunchedEffect(arrangeMode) {
        if (arrangeMode) orderedItems = state.items
    }
    // Während des Modus neue Notizen/Änderungen einpflegen, ohne die Ordnung zu verlieren.
    LaunchedEffect(state.items, arrangeMode) {
        if (arrangeMode && draggingKey == null) {
            val current = orderedItems.map { it.key }
            if (state.items.map { it.key } != current) {
                val byKey = state.items.associateBy { it.key }
                // Bekannte in alter Reihenfolge behalten, neue hinten anhängen.
                val kept = orderedItems.mapNotNull { byKey[it.key] }
                val keptKeys = kept.map { it.key }.toSet()
                orderedItems = kept + state.items.filter { it.key !in keptKeys }
            }
        }
    }

    val contentPadding = WindowInsets.safeDrawing.asPaddingValues()

    // Veraltete Karten-Positionen entfernen (archiviert/gelöscht), damit kein roter Faden
    // zu einer Geister-Position gezeichnet wird.
    LaunchedEffect(state.notes) {
        val visible = state.notes.map { it.note.id }.toSet()
        cardBounds.keys.retainAll(visible)
        // Beim ersten Laden alle bestehenden Notizen als „schon da" markieren – nur danach
        // erstellte Notizen sollen hereinfallen.
        if (!introSeeded && state.notes.isNotEmpty()) {
            visible.forEach { introducedIds[it] = Unit }
            introSeeded = true
        }
    }

    // Vergängliche Notiz: läuft eine sichtbare Notiz ab, zerknüllt sie sich von selbst in den
    // Papierkorb (über die bestehende Knüll-Animation). Die Stimmungsfläche wird hier in der
    // Komposition ermittelt (cardSurface ist @Composable) und in den Effekt übergeben.
    val expiring = state.notes.firstOrNull { it.note.isExpired(now) }?.note
    val expiringSurface = expiring?.mood?.cardSurface()
    LaunchedEffect(expiring?.id, now) {
        val note = expiring ?: return@LaunchedEffect
        if (overlays.crumple != null) return@LaunchedEffect
        val bounds = cardBounds[note.id]
        if (bounds != null && expiringSurface != null) {
            overlays.crumple = CrumpleRequest(note.id, bounds, expiringSurface)
        } else {
            viewModel.moveToTrash(note.id)
        }
    }

    PaperBackground(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentPadding.calculateTopPadding() + PaperDimens.topBarHeight),
            ) {
                AnimatedVisibility(
                    visible = searchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    InkSearchBar(
                        query = state.searchQuery,
                        onQueryChange = viewModel::onSearchChange,
                        onClose = {
                            searchVisible = false
                            viewModel.clearSearch()
                        },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                MoodFilterRow(
                    presentMoods = state.presentMoods,
                    active = state.activeMoodFilter,
                    onToggle = viewModel::toggleMoodFilter,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp),
                )

                TagFilterRow(
                    presentTags = state.presentTags,
                    active = state.activeTagFilter,
                    onToggle = viewModel::toggleTagFilter,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp),
                )

                // Lade-, Leer- und Grid-Phase blenden weich über statt hart zu springen.
                val uiPhase = when {
                    !state.loaded -> GridPhase.LOADING
                    state.notes.isEmpty() -> GridPhase.EMPTY
                    else -> GridPhase.GRID
                }
                AnimatedContent(
                    targetState = uiPhase,
                    label = "gridPhase",
                    transitionSpec = {
                        fadeIn(
                            tween(PaperMotion.DurLong, easing = PaperMotion.EaseEmphasizedDecel),
                        ) togetherWith fadeOut(tween(PaperMotion.DurShortExit))
                    },
                ) { phase ->
                if (phase == GridPhase.LOADING) {
                    // Während des ersten Ladens nur Hintergrund – kein „leer"-Aufblitzen.
                    Box(modifier = Modifier.fillMaxSize())
                } else if (phase == GridPhase.EMPTY) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EmptyState(modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                Column {
                    val displayItems = if (arrangeMode) orderedItems else state.items
                    // Suche/Filter aktiv, aber jede Karte verblasst → ehrlicher Hinweis statt
                    // eines Grids voller blasser Tinte.
                    val filtering = state.searchQuery.isNotBlank() ||
                        state.activeMoodFilter != null || state.activeTagFilter != null
                    if (filtering && state.notes.isNotEmpty() && state.notes.all { it.dimmed }) {
                        NoMatchState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                        )
                    }
                    // Beim Pinch-Zoom „setzt sich" das Papier kurz (Mini-Puls), statt hart umzubrechen.
                    val gridSettle = remember { Animatable(1f) }
                    var settledColumns by remember { mutableIntStateOf(columns) }
                    LaunchedEffect(columns) {
                        if (columns != settledColumns) {
                            settledColumns = columns
                            gridSettle.snapTo(0.98f)
                            gridSettle.animateTo(1f, PaperMotion.SpringGentle)
                        }
                    }
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = gridSettle.value
                                scaleY = gridSettle.value
                            }
                            .then(if (arrangeMode) Modifier else Modifier.transformable(transformState)),
                        userScrollEnabled = !arrangeMode,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 18.dp,
                            bottom = contentPadding.calculateBottomPadding() + 110.dp,
                        ),
                        verticalItemSpacing = 14.dp,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(displayItems, key = { it.key }) { item ->
                          val isDragged = item.key == draggingKey
                          val arrangeBox = Modifier
                              .animateItem(
                                  fadeInSpec = tween(PaperMotion.DurShort),
                                  placementSpec = PaperMotion.SpringPlacement,
                                  fadeOutSpec = tween(PaperMotion.DurShortExit),
                              )
                              .then(
                                  if (arrangeMode) {
                                      Modifier
                                          .onGloballyPositioned { itemBounds[item.key] = it.boundsInRoot() }
                                          .zIndex(if (isDragged) 1f else 0f)
                                          .graphicsLayer {
                                              if (isDragged) {
                                                  val c = itemBounds[item.key]?.center
                                                  if (c != null) {
                                                      translationX = fingerAbs.x - c.x
                                                      translationY = fingerAbs.y - c.y
                                                  }
                                                  scaleX = 1.05f
                                                  scaleY = 1.05f
                                              } else {
                                                  rotationZ = sin((jiggle?.value ?: 0f) + item.key.hashCode()) * 1.2f
                                              }
                                          }
                                          .pointerInput(item.key) {
                                              detectDragGestures(
                                                  onDragStart = { offset ->
                                                      draggingKey = item.key
                                                      val b = itemBounds[item.key]
                                                      fingerAbs = (b?.topLeft ?: Offset.Zero) + offset
                                                      haptics.tick()
                                                  },
                                                  onDrag = { change, drag ->
                                                      change.consume()
                                                      fingerAbs += drag
                                                      val targetKey = itemBounds.entries.firstOrNull { (k, r) ->
                                                          k != draggingKey && r.contains(fingerAbs)
                                                      }?.key
                                                      if (targetKey != null) {
                                                          val from = orderedItems.indexOfFirst { it.key == draggingKey }
                                                          val to = orderedItems.indexOfFirst { it.key == targetKey }
                                                          if (from in orderedItems.indices &&
                                                              to in orderedItems.indices && from != to
                                                          ) {
                                                              orderedItems = orderedItems.toMutableList()
                                                                  .also { m -> m.add(to, m.removeAt(from)) }
                                                          }
                                                      }
                                                  },
                                                  onDragEnd = { draggingKey = null },
                                                  onDragCancel = { draggingKey = null },
                                              )
                                          }
                                  } else Modifier,
                              )
                          Box(modifier = arrangeBox) {
                          when (item) {
                            is SoloItem -> {
                            val gridNote = item.gridNote
                            val note = gridNote.note
                            val hidden = overlays.crumple?.noteId == note.id

                            // Neue Notiz fällt einmalig federnd ins Grid.
                            val willDrop = introSeeded && note.id !in introducedIds
                            val appear = remember(note.id) { Animatable(if (willDrop) 0f else 1f) }
                            LaunchedEffect(note.id) {
                                if (willDrop) {
                                    introducedIds[note.id] = Unit
                                    appear.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow,
                                        ),
                                    )
                                }
                            }
                            val dropPx = with(density) { 20.dp.toPx() }

                            val card: @Composable () -> Unit = {
                                if (!hidden) {
                                    with(sharedScope) {
                                        NoteCard(
                                            note = note,
                                            dimmed = gridNote.dimmed,
                                            reminderDue = note.isReminderDue(now),
                                            now = now,
                                            onClick = {
                                                if (arrangeMode) return@NoteCard
                                                if (selectionMode) {
                                                    haptics.tick()
                                                    viewModel.toggleSelected(item)
                                                    return@NoteCard
                                                }
                                                when {
                                                    // Zeitkapsel: lässt sich vor dem Termin nicht öffnen.
                                                    note.isCapsuleLocked(now) -> {
                                                        val day = java.text.SimpleDateFormat(
                                                            "d. MMM yyyy",
                                                            java.util.Locale.GERMAN,
                                                        ).format(note.capsuleAt!!)
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "Versiegelt bis $day",
                                                            android.widget.Toast.LENGTH_SHORT,
                                                        ).show()
                                                    }
                                                    note.sealed -> {
                                                        cardBounds[note.id]?.let { b ->
                                                            overlays.sealBreak = WaxSealBreakRequest(note.id, b, WaxRed)
                                                        } ?: onOpenNote(note.id)
                                                    }
                                                    else -> onOpenNote(note.id)
                                                }
                                            },
                                            onToggleDogEar = { if (!arrangeMode && !selectionMode) viewModel.toggleDogEar(note) },
                                            onPickMood = { if (!arrangeMode && !selectionMode) sheets.moodTarget = note },
                                            // Long-Press betritt die Mehrfachauswahl (Pinnen bleibt
                                            // über das Eselsohr-Sheet und als Stapel-Aktion erreichbar).
                                            onLongPress = {
                                                if (!arrangeMode) {
                                                    haptics.tick()
                                                    viewModel.toggleSelected(item)
                                                }
                                            },
                                            onCountdown = { if (!arrangeMode && !selectionMode) sheets.countdownTarget = note },
                                            onToggleStampDay = { day -> if (!arrangeMode && !selectionMode) viewModel.toggleStamp(note, day) },
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    val a = appear.value
                                                    alpha = a
                                                    scaleX = 0.8f + 0.2f * a
                                                    scaleY = 0.8f + 0.2f * a
                                                    translationY = (1f - a) * -dropPx
                                                }
                                                .onGloballyPositioned {
                                                    cardBounds[note.id] = it.boundsInRoot()
                                                }
                                                .sharedBounds(
                                                    rememberSharedContentState(key = "note-${note.id}"),
                                                    animatedVisibilityScope = animatedScope,
                                                ),
                                        )
                                    }
                                }
                            }

                            if (arrangeMode || selectionMode) {
                                // Im Auswahlmodus keinen Swipe anbieten – ein versehentlicher
                                // Wisch mitten in der Auswahl würde sonst archivieren.
                                card()
                            } else {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value != SwipeToDismissBoxValue.Settled) {
                                            viewModel.archive(note.id)
                                            true
                                        } else false
                                    },
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        // Nur während des aktiven Swipes zeigen – sonst würde das
                                        // Label hinter verblassten (gedimmten) Karten durchscheinen.
                                        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = "archiviert",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.outline,
                                                )
                                            }
                                        }
                                    },
                                ) {
                                    card()
                                }
                            }
                            }
                            is StackItem -> {
                                NoteStack(
                                    item = item,
                                    now = now,
                                    // Im Auswahlmodus wählt ein Tap den ganzen Stapel als Einheit.
                                    onOpenNote = {
                                        if (selectionMode) {
                                            haptics.tick()
                                            viewModel.toggleSelected(item)
                                        } else if (!arrangeMode) {
                                            onOpenNote(it)
                                        }
                                    },
                                    onToggleDogEar = { if (!arrangeMode && !selectionMode) viewModel.toggleDogEar(it) },
                                    onPickMood = { if (!arrangeMode && !selectionMode) sheets.moodTarget = it },
                                    onToggleStampDay = { n, day -> if (!arrangeMode && !selectionMode) viewModel.toggleStamp(n, day) },
                                    onUnclip = { if (!arrangeMode && !selectionMode) viewModel.unclip(it) },
                                    onCountdown = { if (!arrangeMode && !selectionMode) sheets.countdownTarget = it },
                                    modifier = Modifier,
                                )
                            }
                          }
                          // Heftzwecken-Badge auf ausgewählten Karten/Stapeln.
                          val itemSelected = when (item) {
                              is SoloItem -> item.gridNote.note.id in selectedIds
                              is StackItem -> item.members.all { it.note.id in selectedIds }
                          }
                          if (itemSelected) {
                              SelectionPin(
                                  modifier = Modifier
                                      .align(Alignment.TopStart)
                                      .padding(start = 8.dp, top = 2.dp)
                                      .zIndex(2f),
                              )
                          }
                          }
                        }
                    }
                }
                }
                }
            }

            // Rote Fäden zwischen verknüpften Karten (über den Karten, unter den Bedienelementen).
            RedThreadOverlay(
                links = state.links,
                bounds = cardBounds,
                dimmedIds = dimmedIds,
                modifier = Modifier.fillMaxSize(),
            )

            // Im Auswahlmodus ersetzt die Werkzeugleiste die oberen Bedienelemente.
            if (selectionMode) {
                SelectionBar(
                    count = selectedIds.size,
                    onClose = {
                        haptics.tap()
                        viewModel.clearSelection()
                    },
                    onPin = {
                        haptics.tap()
                        viewModel.pinSelected()
                    },
                    onArchive = {
                        haptics.tap()
                        viewModel.archiveSelected()
                    },
                    onTrash = {
                        haptics.crumple()
                        viewModel.trashSelected()
                    },
                    onMood = { sheets.selectionMoodOpen = true },
                    onTag = { sheets.selectionTagOpen = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = contentPadding.calculateTopPadding() + 4.dp),
                )
            } else {
            // Sichtbare Bedienelemente oben: Suche + Design (links), Archiv (rechts).
            // Der Teebeutel bleibt mittig.
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = contentPadding.calculateTopPadding() + 4.dp),
            ) {
                TopAction(
                    icon = Icons.Rounded.Search,
                    description = "Suchen",
                    onClick = { searchVisible = true },
                )
                Spacer(Modifier.width(8.dp))
                TopAction(
                    icon = Icons.Rounded.Settings,
                    description = "Einstellungen",
                    onClick = { sheets.settingsSheetOpen = true },
                )
                Spacer(Modifier.width(8.dp))
                TopAction(
                    icon = Icons.Rounded.Event,
                    description = "Agenda",
                    onClick = onOpenAgenda,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = contentPadding.calculateTopPadding() + 4.dp),
            ) {
                TopAction(
                    icon = Icons.Rounded.SwapVert,
                    description = if (arrangeMode) "Anordnen beenden" else "Ordnen",
                    onClick = {
                        haptics.tap()
                        if (arrangeMode) finishArrange() else sheets.sortSheetOpen = true
                    },
                )
                Spacer(Modifier.width(8.dp))
                TopAction(
                    icon = Icons.Rounded.Inventory2,
                    description = "Archiv & Papierkorb",
                    onClick = { sheets.drawerOpen = true },
                )
            }

            // Glücks-Teebeutel oben
            TeabagPull(
                delight = state.delight,
                highlighted = state.delightAvailable,
                stats = state.statsLine.ifBlank { null },
                onPulled = { viewModel.markDelightPulled() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding()),
            )
            }

            // Schubladen-Lasche unten mittig (Archiv + Papierkorb) – zusätzlicher Einstieg.
            // Im Anordnen-Modus weicht sie der „Fertig"-Pille, im Auswahlmodus der Werkzeugleiste.
            if (!arrangeMode && !selectionMode) {
                DrawerHandle(
                    archiveCount = state.archived.size,
                    trashCount = state.trashed.size,
                    onOpen = { sheets.drawerOpen = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = contentPadding.calculateBottomPadding() + 4.dp),
                )
            } else if (arrangeMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp)
                        .paperPress(RoundedCornerShape(50)) { finishArrange() }
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Fertig",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            // Leichter Scrim, wenn das FAB-Fächer offen ist (Tap daneben schließt es)
            if (fabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) { fabExpanded = false },
                )
            }

            // Runder "+"-Button mit Fächer – im Anordnen- und Auswahlmodus ausgeblendet.
            if (!arrangeMode && !selectionMode) {
                AddFab(
                    expanded = fabExpanded,
                    onExpandedChange = { fabExpanded = it },
                    onCreate = onCreateNote,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .padding(bottom = contentPadding.calculateBottomPadding()),
                )
            }

            // Spalten-Streifen: zeigt nach dem Pinch kurz die neue Spaltenzahl.
            var showColumnsStrip by remember { mutableStateOf(false) }
            LaunchedEffect(columnsChangedAt) {
                if (columnsChangedAt > 0L) {
                    showColumnsStrip = true
                    kotlinx.coroutines.delay(1200)
                    showColumnsStrip = false
                }
            }
            AnimatedVisibility(
                visible = showColumnsStrip,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding() + PaperDimens.topBarHeight + 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer { rotationZ = PaperDimens.TILT_PLAYFUL }
                        .shadow(6.dp, RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = if (columns == 1) "1 Spalte" else "$columns Spalten",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Undo-Streifen: lugt nach Zerknüllen/Archivieren kurz aus dem Papierkorb hervor.
            // lastUndo hält das Event für die Ausblend-Animation fest.
            var lastUndo by remember { mutableStateOf<UndoEvent?>(null) }
            if (undoEvent != null) lastUndo = undoEvent
            AnimatedVisibility(
                visible = undoEvent != null,
                enter = slideInVertically { it * 2 } + fadeIn(),
                exit = slideOutVertically { it * 2 } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 56.dp),
            ) {
                lastUndo?.let { event ->
                    PaperSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        onAction = {
                            haptics.tap()
                            viewModel.undoLast()
                        },
                    )
                }
            }
            undoEvent?.let { event ->
                LaunchedEffect(event.id) {
                    // Bei mehreren Zetteln länger sichtbar – mehr Zeit zum Glattstreichen.
                    kotlinx.coroutines.delay(if (event.noteIds.size > 1) 8000 else 5000)
                    viewModel.dismissUndo(event.id)
                }
            }

            // Einmaliger Papier-Tipp: Langdruck öffnet die Mehrfachauswahl. Weicht dem
            // Undo-Streifen und verschwindet nach kurzer Zeit von selbst.
            AnimatedVisibility(
                visible = longPressHint && undoEvent == null && !selectionMode,
                enter = slideInVertically { it * 2 } + fadeIn(),
                exit = slideOutVertically { it * 2 } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 56.dp),
            ) {
                PaperSnackbar(
                    message = stringResource(R.string.hint_long_press),
                    actionLabel = stringResource(R.string.hint_long_press_ok),
                    onAction = {
                        haptics.tap()
                        viewModel.dismissLongPressHint()
                    },
                )
            }
            if (longPressHint) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(8000)
                    viewModel.dismissLongPressHint()
                }
            }
        }
    }

    // Vollbild-Overlays: Knüddeln, Papierflieger, Siegelbruch, Konfetti.
    NotesOverlays(
        overlays = overlays,
        onTrash = viewModel::moveToTrash,
        onOpenNote = onOpenNote,
    )

    // Alle Bottom-Sheets des Grids; Querschnitts-Aktionen laufen über Callbacks.
    NotesSheets(
        state = state,
        sheets = sheets,
        viewModel = viewModel,
        sortMode = sortMode,
        currentTheme = currentTheme,
        onPickTheme = { themeViewModel.setTheme(it) },
        onShare = { target, surface, ink, accent ->
            val bounds = cardBounds[target.id]
            overlays.shareText = target.toShareText()
            val surfaceArgb = surface.toArgb()
            val inkArgb = ink.toArgb()
            val accentArgb = accent.toArgb()
            scope.launch {
                val uri = withContext(Dispatchers.IO) {
                    ShareCardRenderer.render(context, target, surfaceArgb, inkArgb, accentArgb)
                }
                overlays.shareUri = uri
                if (bounds != null) {
                    overlays.shareRequest = PaperPlaneRequest(target.id, bounds, surface)
                } else if (uri != null) {
                    context.shareImage(uri, overlays.shareText)
                } else {
                    context.sharePlainText(overlays.shareText)
                }
            }
        },
        onDelete = { target, surface ->
            val bounds = cardBounds[target.id]
            if (bounds != null) {
                overlays.crumple = CrumpleRequest(target.id, bounds, surface)
            } else {
                viewModel.moveToTrash(target.id)
            }
        },
        onAttachPhoto = { target ->
            photoTarget = target
            pickPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onReminderSet = ::ensureNotificationPermission,
        onEnterArrange = { arrangeMode = true },
        onExportBackup = { createBackup.launch("papernotes-sicherung-${backupDateStamp()}.zip") },
        onImportBackup = { openBackup.launch(arrayOf("application/zip")) },
    )
}

