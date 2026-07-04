package com.papernotes.ui.notes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import com.papernotes.domain.model.cardSurface
import com.papernotes.domain.model.earAccent
import com.papernotes.domain.toShareText
import com.papernotes.ui.components.AddFab
import com.papernotes.ui.components.ArchiveDrawerSheet
import com.papernotes.ui.components.ConfettiBurst
import com.papernotes.ui.components.CrumpleOverlay
import com.papernotes.ui.components.CrumpleRequest
import com.papernotes.ui.components.DrawerHandle
import com.papernotes.ui.components.ExpirySheet
import com.papernotes.ui.components.InkSearchBar
import com.papernotes.ui.components.MoodFilterRow
import com.papernotes.ui.components.MoodOnlySheet
import com.papernotes.ui.components.MoodPickerSheet
import com.papernotes.ui.components.SelectionBar
import com.papernotes.ui.components.SelectionPin
import com.papernotes.ui.components.TagFilterRow
import com.papernotes.ui.components.TagPickerSheet
import com.papernotes.ui.components.ClipPickerSheet
import com.papernotes.ui.components.CapsuleSheet
import com.papernotes.ui.components.CountdownSheet
import com.papernotes.ui.components.NoteCard
import com.papernotes.ui.components.NoteLinkPickerSheet
import com.papernotes.ui.components.NoteStack
import com.papernotes.ui.components.PaperBackground
import com.papernotes.ui.components.PaperPlaneOverlay
import com.papernotes.ui.components.PaperPlaneRequest
import com.papernotes.ui.components.PaperSnackbar
import com.papernotes.ui.components.SortSheet
import com.papernotes.ui.components.RedThreadOverlay
import com.papernotes.ui.components.paperPress
import com.papernotes.ui.components.ReminderSheet
import com.papernotes.ui.components.SettingsSheet
import com.papernotes.ui.components.TeabagPull
import com.papernotes.ui.components.WaxRed
import com.papernotes.ui.components.WaxSealBreakOverlay
import com.papernotes.ui.components.WaxSealBreakRequest
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
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val currentTheme by themeViewModel.theme.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

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

    var columns by remember { mutableIntStateOf(2) }
    var zoomAccum by remember { mutableFloatStateOf(1f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        zoomAccum *= zoomChange
        when {
            zoomAccum > 1.25f -> { columns = (columns - 1).coerceAtLeast(1); zoomAccum = 1f }
            zoomAccum < 0.8f -> { columns = (columns + 1).coerceAtMost(3); zoomAccum = 1f }
        }
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
    var crumple by remember { mutableStateOf<CrumpleRequest?>(null) }
    var sealBreak by remember { mutableStateOf<WaxSealBreakRequest?>(null) }
    var shareRequest by remember { mutableStateOf<PaperPlaneRequest?>(null) }
    var shareText by remember { mutableStateOf("") }
    var shareUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var moodTarget by remember { mutableStateOf<Note?>(null) }
    var tagTarget by remember { mutableStateOf<Note?>(null) }
    var linkTarget by remember { mutableStateOf<Note?>(null) }
    var clipTarget by remember { mutableStateOf<Note?>(null) }
    var expiryTarget by remember { mutableStateOf<Note?>(null) }
    var countdownTarget by remember { mutableStateOf<Note?>(null) }
    var capsuleTarget by remember { mutableStateOf<Note?>(null) }

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
    var stampConfettiKey by remember { mutableIntStateOf(0) }
    var showStampConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.stampMilestone.collect {
            stampConfettiKey++
            showStampConfetti = true
        }
    }
    var reminderTarget by remember { mutableStateOf<Note?>(null) }
    var drawerOpen by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var settingsSheetOpen by remember { mutableStateOf(false) }
    var sortSheetOpen by remember { mutableStateOf(false) }

    // Mehrfachauswahl: Long-Press auf eine Karte betritt den Modus, Tap toggelt weitere.
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val selectionMode = selectedIds.isNotEmpty()
    var selectionMoodOpen by remember { mutableStateOf(false) }
    var selectionTagOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }

    // Pinnwand: Anordnen-Modus zum freien Umsortieren der Karten.
    val haptics = rememberPaperHaptics()
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
        if (crumple != null) return@LaunchedEffect
        val bounds = cardBounds[note.id]
        if (bounds != null && expiringSurface != null) {
            crumple = CrumpleRequest(note.id, bounds, expiringSurface)
        } else {
            viewModel.moveToTrash(note.id)
        }
    }

    PaperBackground(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentPadding.calculateTopPadding() + 46.dp),
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

                if (!state.loaded) {
                    // Während des ersten Ladens nur Hintergrund – kein „leer"-Aufblitzen.
                    Box(modifier = Modifier.fillMaxSize())
                } else if (state.notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EmptyState(modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    val displayItems = if (arrangeMode) orderedItems else state.items
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxSize()
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
                              .animateItem()
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
                            val hidden = crumple?.noteId == note.id

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
                                                            sealBreak = WaxSealBreakRequest(note.id, b, WaxRed)
                                                        } ?: onOpenNote(note.id)
                                                    }
                                                    else -> onOpenNote(note.id)
                                                }
                                            },
                                            onToggleDogEar = { if (!arrangeMode && !selectionMode) viewModel.toggleDogEar(note) },
                                            onPickMood = { if (!arrangeMode && !selectionMode) moodTarget = note },
                                            // Long-Press betritt die Mehrfachauswahl (Pinnen bleibt
                                            // über das Eselsohr-Sheet und als Stapel-Aktion erreichbar).
                                            onLongPress = {
                                                if (!arrangeMode) {
                                                    haptics.tick()
                                                    viewModel.toggleSelected(item)
                                                }
                                            },
                                            onCountdown = { if (!arrangeMode && !selectionMode) countdownTarget = note },
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
                                    onPickMood = { if (!arrangeMode && !selectionMode) moodTarget = it },
                                    onToggleStampDay = { n, day -> if (!arrangeMode && !selectionMode) viewModel.toggleStamp(n, day) },
                                    onUnclip = { if (!arrangeMode && !selectionMode) viewModel.unclip(it) },
                                    onCountdown = { if (!arrangeMode && !selectionMode) countdownTarget = it },
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
                    onMood = { selectionMoodOpen = true },
                    onTag = { selectionTagOpen = true },
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
                    onClick = { settingsSheetOpen = true },
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
                        if (arrangeMode) finishArrange() else sortSheetOpen = true
                    },
                )
                Spacer(Modifier.width(8.dp))
                TopAction(
                    icon = Icons.Rounded.Inventory2,
                    description = "Archiv & Papierkorb",
                    onClick = { drawerOpen = true },
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
                    onOpen = { drawerOpen = true },
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
                    kotlinx.coroutines.delay(5000)
                    viewModel.dismissUndo(event.id)
                }
            }
        }
    }

    // Knüddel-Animation → Papierkorb
    crumple?.let { req ->
        CrumpleOverlay(
            request = req,
            onFinished = {
                viewModel.moveToTrash(req.noteId)
                crumple = null
            },
        )
    }

    // Papierflieger-Animation → Android-Teilen-Auswahl
    shareRequest?.let { req ->
        PaperPlaneOverlay(
            request = req,
            onFinished = {
                val uri = shareUri
                if (uri != null) context.shareImage(uri, shareText) else context.sharePlainText(shareText)
                shareRequest = null
                shareUri = null
            },
        )
    }

    // Wachssiegel zerspringt → öffnet die versiegelte Notiz
    sealBreak?.let { req ->
        WaxSealBreakOverlay(
            request = req,
            onFinished = {
                onOpenNote(req.noteId)
                sealBreak = null
            },
        )
    }

    // Stempel-Meilenstein: Konfetti über dem Grid
    if (showStampConfetti) {
        ConfettiBurst(trigger = stampConfettiKey, onFinished = { showStampConfetti = false })
    }

    // Stimmungs-/Pin-/Lösch-Sheet
    moodTarget?.let { target ->
        val targetSurface = target.mood.cardSurface()
        val targetAccent = target.mood.earAccent()
        val inkColor = MaterialTheme.colorScheme.onBackground
        MoodPickerSheet(
            selected = target.mood,
            pinned = target.pinned,
            hasReminder = target.hasReminder,
            sealed = target.sealed,
            invisibleInk = target.invisibleInk,
            done = target.done,
            hasExpiry = target.hasExpiry,
            hasCountdown = target.hasCountdown,
            hasCapsule = target.capsuleAt != null,
            hasPhoto = target.hasPhoto,
            paper = target.paper,
            onPick = { mood ->
                viewModel.setMood(target, mood)
                moodTarget = null
            },
            onPickPaper = { style ->
                viewModel.setPaper(target, style)
                moodTarget = target.copy(paper = style) // Auswahl sofort im Sheet spiegeln
            },
            onTogglePin = {
                viewModel.togglePin(target)
                moodTarget = null
            },
            onToggleSeal = {
                viewModel.toggleSeal(target)
                moodTarget = null
            },
            onSetCapsule = {
                moodTarget = null
                capsuleTarget = target
            },
            onToggleInvisibleInk = {
                viewModel.toggleInvisibleInk(target)
                moodTarget = null
            },
            onToggleDone = {
                viewModel.toggleDone(target)
                moodTarget = null
            },
            onEditTags = {
                moodTarget = null
                tagTarget = target
            },
            onSetExpiry = {
                expiryTarget = target
                moodTarget = null
            },
            onSetReminder = {
                moodTarget = null
                reminderTarget = target
            },
            onSetCountdown = {
                moodTarget = null
                countdownTarget = target
            },
            onAttachPhoto = {
                photoTarget = target
                moodTarget = null
                pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onLink = {
                moodTarget = null
                linkTarget = target
            },
            onClip = {
                moodTarget = null
                clipTarget = target
            },
            onShare = {
                val bounds = cardBounds[target.id]
                moodTarget = null
                shareText = target.toShareText()
                val surfaceArgb = targetSurface.toArgb()
                val inkArgb = inkColor.toArgb()
                val accentArgb = targetAccent.toArgb()
                scope.launch {
                    val uri = withContext(Dispatchers.IO) {
                        ShareCardRenderer.render(context, target, surfaceArgb, inkArgb, accentArgb)
                    }
                    shareUri = uri
                    if (bounds != null) {
                        shareRequest = PaperPlaneRequest(target.id, bounds, targetSurface)
                    } else if (uri != null) {
                        context.shareImage(uri, shareText)
                    } else {
                        context.sharePlainText(shareText)
                    }
                }
            },
            onCopy = {
                clipboard.setText(AnnotatedString(target.toShareText()))
                moodTarget = null
            },
            onDelete = {
                val bounds = cardBounds[target.id]
                moodTarget = null
                if (bounds != null) {
                    crumple = CrumpleRequest(target.id, bounds, targetSurface)
                } else {
                    viewModel.moveToTrash(target.id)
                }
            },
            onDismiss = { moodTarget = null },
        )
    }

    // Karteireiter-Sheet (vom Grid-Long-Press)
    tagTarget?.let { target ->
        // Live-Notiz, damit die Chip-Auswahl die letzte Änderung sofort spiegelt.
        val live = state.notes.firstOrNull { it.note.id == target.id }?.note ?: target
        TagPickerSheet(
            noteTags = live.tagList,
            allTags = state.presentTags,
            onToggle = { tag -> viewModel.toggleTag(live, tag) },
            onAdd = { tag -> viewModel.addTag(live, tag) },
            onDismiss = { tagTarget = null },
        )
    }

    // Erinnerungs-Sheet (vom Grid-Long-Press)
    reminderTarget?.let { target ->
        ReminderSheet(
            currentReminderAt = target.reminderAt,
            currentRule = target.reminderRule,
            onPick = { at, rule ->
                viewModel.setReminder(target, at, rule)
                ensureNotificationPermission()
                reminderTarget = null
            },
            onClear = {
                viewModel.setReminder(target, null)
                reminderTarget = null
            },
            onDismiss = { reminderTarget = null },
        )
    }

    // Selbstzerstörung: Ablaufzeit der Notiz wählen/aufheben
    expiryTarget?.let { target ->
        ExpirySheet(
            currentExpiresAt = target.expiresAt,
            onPick = { at ->
                viewModel.setExpiry(target, at)
                expiryTarget = null
            },
            onClear = {
                viewModel.setExpiry(target, null)
                expiryTarget = null
            },
            onDismiss = { expiryTarget = null },
        )
    }

    // Abreißkalender: Zieldatum der Notiz wählen/abreißen
    countdownTarget?.let { target ->
        CountdownSheet(
            currentCountdownAt = target.countdownAt,
            onPick = { at ->
                viewModel.setCountdown(target, at)
                countdownTarget = null
            },
            onClear = {
                viewModel.setCountdown(target, null)
                countdownTarget = null
            },
            onDismiss = { countdownTarget = null },
        )
    }

    // Zeitkapsel: Notiz bis zu einem Datum versiegeln
    capsuleTarget?.let { target ->
        CapsuleSheet(
            currentCapsuleAt = target.capsuleAt,
            onPick = { at ->
                viewModel.setCapsule(target, at)
                capsuleTarget = null
            },
            onClear = {
                viewModel.setCapsule(target, null)
                capsuleTarget = null
            },
            onDismiss = { capsuleTarget = null },
        )
    }

    // Roter-Faden-Auswahl: andere Notiz zum Verknüpfen/Lösen wählen
    linkTarget?.let { target ->
        val linkedIds = state.links
            .filter { it.involves(target.id) }
            .mapNotNull { it.otherEnd(target.id) }
            .toSet()
        NoteLinkPickerSheet(
            candidates = state.notes.map { it.note }.filter { it.id != target.id },
            linkedIds = linkedIds,
            onToggle = { otherId ->
                if (otherId in linkedIds) {
                    viewModel.unlinkNotes(target.id, otherId)
                } else {
                    viewModel.linkNotes(target.id, otherId)
                }
            },
            onDismiss = { linkTarget = null },
        )
    }

    // Büroklammer-Auswahl: andere Notizen an den Stapel klammern/lösen
    clipTarget?.let { target ->
        val groupId = target.clipId ?: target.id
        val clippedIds = state.notes
            .map { it.note }
            .filter { it.id != target.id && it.clipId == groupId }
            .map { it.id }
            .toSet()
        ClipPickerSheet(
            candidates = state.notes.map { it.note }.filter { it.id != target.id },
            clippedIds = clippedIds,
            onToggle = { otherId -> viewModel.toggleClip(target, otherId) },
            onDismiss = { clipTarget = null },
        )
    }

    // Ordnen: Sortierung wählen oder frei anordnen
    if (sortSheetOpen) {
        SortSheet(
            selected = sortMode,
            onPick = { mode ->
                viewModel.setSortMode(mode)
                sortSheetOpen = false
            },
            onArrange = {
                // Freies Ziehen setzt die Sortierung auf Pinnwand zurück, sonst würde
                // die aktive Auto-Sortierung die gezogene Reihenfolge sofort überstimmen.
                viewModel.setSortMode(SortMode.PINNWAND)
                viewModel.clearSelection()
                arrangeMode = true
                sortSheetOpen = false
            },
            onDismiss = { sortSheetOpen = false },
        )
    }

    // Einstellungen-Zettel: Papier-Theme, Sicherung, Über
    if (settingsSheetOpen) {
        SettingsSheet(
            selectedTheme = currentTheme,
            onPickTheme = { themeViewModel.setTheme(it) },
            onDismiss = { settingsSheetOpen = false },
            onExport = {
                settingsSheetOpen = false
                createBackup.launch("papernotes-sicherung-${backupDateStamp()}.zip")
            },
            onImport = {
                settingsSheetOpen = false
                openBackup.launch(arrayOf("application/zip"))
            },
        )
    }

    // Stimmung für die Mehrfachauswahl (abgespeckte Farbreihe)
    if (selectionMoodOpen) {
        MoodOnlySheet(
            onPick = { mood ->
                viewModel.moodSelected(mood)
                selectionMoodOpen = false
            },
            onDismiss = { selectionMoodOpen = false },
        )
    }

    // Karteireiter für die Mehrfachauswahl (fügt den Tag allen gewählten Zetteln hinzu)
    if (selectionTagOpen) {
        TagPickerSheet(
            noteTags = emptyList(),
            allTags = state.presentTags,
            onToggle = { tag ->
                viewModel.tagSelected(tag)
                selectionTagOpen = false
            },
            onAdd = { tag ->
                viewModel.tagSelected(tag)
                selectionTagOpen = false
            },
            onDismiss = { selectionTagOpen = false },
        )
    }

    // Archiv-Schublade
    if (drawerOpen) {
        ArchiveDrawerSheet(
            archived = state.archived,
            trashed = state.trashed,
            onRestoreArchived = viewModel::restore,
            onRestoreTrashed = viewModel::restore,
            onDismiss = { drawerOpen = false },
        )
    }
}

/** Datumsstempel (JJJJ-MM-TT) für den vorgeschlagenen Sicherungs-Dateinamen. */
private fun backupDateStamp(): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.GERMAN).format(java.util.Date())

/** Kleiner, runder Icon-Button für die obere Leiste (Suche / Archiv). */
@Composable
private fun TopAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .paperPress(CircleShape, onClick = onClick)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    // Sanftes Schweben – wirkt lebendig statt statisch.
    val bob = rememberInfiniteTransition(label = "emptyBob")
    val phase by bob.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "bobPhase",
    )
    val amp = with(LocalDensity.current) { 6.dp.toPx() }
    Column(
        modifier = modifier
            .padding(32.dp)
            .graphicsLayer { translationY = phase * amp },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "🌼", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Noch nichts notiert",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Tippe auf +, um deine erste Notiz zu schreiben.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}
