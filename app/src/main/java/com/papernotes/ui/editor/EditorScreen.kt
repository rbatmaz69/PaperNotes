package com.papernotes.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.papernotes.domain.model.NoteType
import com.papernotes.ui.components.HighlightColors
import com.papernotes.ui.components.highlightTransformation
import com.papernotes.ui.components.ordered
import com.papernotes.domain.model.cardSurface
import com.papernotes.domain.model.earAccent
import com.papernotes.domain.toShareText
import com.papernotes.ui.components.CapsuleSheet
import com.papernotes.ui.components.ConfettiBurst
import com.papernotes.ui.components.DogEar
import com.papernotes.ui.components.ExpirySheet
import com.papernotes.ui.components.ClipPickerSheet
import com.papernotes.ui.components.CountdownSheet
import com.papernotes.ui.components.MoodPickerSheet
import com.papernotes.ui.components.PaperBackground
import com.papernotes.ui.components.NoteLinkPickerSheet
import com.papernotes.ui.components.PaperPlaneOverlay
import com.papernotes.ui.components.PaperPlaneRequest
import com.papernotes.ui.components.Polaroid
import com.papernotes.ui.components.INK_PALETTE
import com.papernotes.ui.components.PEN_MEDIUM
import com.papernotes.ui.components.ReminderSheet
import com.papernotes.ui.components.SketchCanvas
import com.papernotes.ui.components.SketchToolbar
import com.papernotes.ui.components.StampCard
import com.papernotes.ui.components.StampMotifPicker
import com.papernotes.ui.components.TagPickerSheet
import com.papernotes.ui.components.paperPress
import com.papernotes.ui.components.paperRuling
import com.papernotes.ui.theme.PaperDimens
import java.time.LocalDate
import com.papernotes.util.PhotoStore
import com.papernotes.util.ShareCardRenderer
import com.papernotes.util.rememberPaperHaptics
import com.papernotes.util.shareImage
import com.papernotes.util.sharePlainText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Clean Writing Mode": Beim Öffnen blenden die System-Leisten sanft aus – es bleibt nur
 * Papier, Text und Tastatur. Die Karte morpht via Shared-Element fließend in den Vollbild-Editor.
 * Checklisten-Notizen bekommen statt des Fließtexts den [ChecklistEditor].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EditorScreen(
    noteId: Long,
    newType: NoteType,
    session: Int,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    onOpenLinkedNote: (Long) -> Unit,
    // Vorbefüllung für einen frisch „eingeklebten" Zettel (geteilter Text/Shortcut).
    initialTitle: String = "",
    initialBody: String = "",
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val haptics = rememberPaperHaptics()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Ergebnis egal: Alarm läuft, Notification erscheint erst nach Erteilung. */ }

    // Foto-Picker (System-Auswahl, keine Berechtigung): speichert das Bild und setzt den Pfad.
    val photoScope = rememberCoroutineScope()
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            photoScope.launch {
                val name = PhotoStore.save(context, uri)
                if (name != null) {
                    viewModel.note.value.photoPath?.let { PhotoStore.delete(context, it) }
                    viewModel.setPhoto(name)
                }
            }
        }
    }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(session) { viewModel.load(noteId, newType, session, initialTitle, initialBody) }
    val note by viewModel.note.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val strokes by viewModel.strokes.collectAsStateWithLifecycle()
    val celebration by viewModel.celebration.collectAsStateWithLifecycle()
    val focusRequestId by viewModel.focusRequest.collectAsStateWithLifecycle()
    val linkedNotes by viewModel.linkedNotes.collectAsStateWithLifecycle()
    val candidateNotes by viewModel.candidateNotes.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    var showMood by remember { mutableStateOf(false) }
    var showTags by remember { mutableStateOf(false) }
    var showingBack by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var showExpiry by remember { mutableStateOf(false) }
    var showCapsule by remember { mutableStateOf(false) }
    var showLinkPicker by remember { mutableStateOf(false) }
    var showClip by remember { mutableStateOf(false) }
    var showCountdown by remember { mutableStateOf(false) }
    var confettiKey by remember { mutableStateOf<Int?>(null) }
    var editorBounds by remember { mutableStateOf(Rect.Zero) }
    var shareRequest by remember { mutableStateOf<PaperPlaneRequest?>(null) }
    var shareText by remember { mutableStateOf("") }
    var shareUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val bodyFocus = remember { FocusRequester() }
    // Textmarker: Body-Feld als TextFieldValue (für die Auswahl) + sichtbare Farbleiste.
    var bodyValue by remember(session) { mutableStateOf(TextFieldValue(note.body)) }
    var lastSelection by remember(session) { mutableStateOf(TextRange.Zero) }
    var markerBar by remember { mutableStateOf(false) }
    // Externen Lade-/Reset-Stand übernehmen, ohne die laufende Eingabe zu stören.
    LaunchedEffect(note.id, note.body) {
        if (note.body != bodyValue.text) bodyValue = bodyValue.copy(text = note.body)
    }
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = note.mood.earAccent()
    // Stimmungswechsel: Editor-Hintergrund weich durchwaschen.
    val noteSurface by animateColorAsState(note.mood.cardSurface(), tween(320), label = "editorSurface")
    // Blatt umdrehen: 0° = Vorderseite, 180° = Rückseite (Inhalt wird bei 90° getauscht).
    val flip by animateFloatAsState(
        targetValue = if (showingBack) 180f else 0f,
        animationSpec = tween(450),
        label = "editorFlip",
    )

    // Konfetti, wenn der letzte offene Eintrag abgehakt wurde
    LaunchedEffect(celebration) {
        if (celebration > 0) {
            haptics.confirm()
            confettiKey = celebration
        }
    }

    // Bei neuer Text-Notiz direkt Tastatur/Fokus (Checkliste fokussiert ihre erste Zeile selbst).
    LaunchedEffect(Unit) {
        if (noteId <= 0L && newType == NoteType.TEXT) bodyFocus.requestFocus()
    }

    fun goBack() {
        viewModel.flush()
        onBack()
    }

    BackHandler { goBack() }

    val insets = WindowInsets.safeDrawing.asPaddingValues()

    PaperBackground(
        baseColor = note.mood.cardSurface(),
        dotGrid = true,
    ) {
        with(sharedScope) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { editorBounds = it.boundsInRoot() }
                    .sharedBounds(
                        rememberSharedContentState(key = "note-$noteId"),
                        animatedVisibilityScope = animatedScope,
                    )
                    .padding(
                        top = insets.calculateTopPadding() + 8.dp,
                        bottom = 8.dp,
                    )
                    .imePadding(),
            ) {
                // Kopfzeile: Zurück + Eselsohr (Stimmung) – feste Höhe wie in der Übersicht,
                // damit nichts springt, wenn Werkzeuge ein-/ausgeblendet werden.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PaperDimens.topBarHeight)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = {
                        haptics.tap()
                        goBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Zurück",
                            tint = ink,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (markerBar && note.type == NoteType.TEXT && !showingBack) {
                        // Textmarker aktiv: Farb-Swatches direkt in der oberen Leiste.
                        HighlightColors.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(26.dp)
                                    .paperPress(CircleShape) {
                                        haptics.tick()
                                        val (a, b) = lastSelection.ordered()
                                        viewModel.applyHighlight(a, b, index)
                                    }
                                    .background(color, CircleShape)
                                    .border(1.dp, ink.copy(alpha = 0.25f), CircleShape),
                            )
                        }
                        IconButton(onClick = {
                            haptics.tap()
                            markerBar = false
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Fertig",
                                tint = note.mood.earAccent(),
                            )
                        }
                      } else {
                        // Radiergummi & Durchschlag: Schritt zurück / wieder vor.
                        // Immer komponiert, damit die Nachbar-Symbole nicht springen –
                        // die Icons blenden nur ein/aus wie trocknende Tinte.
                        val undoAlpha by animateFloatAsState(
                            targetValue = if (canUndo) 1f else 0f,
                            animationSpec = tween(220),
                            label = "undoAlpha",
                        )
                        val redoAlpha by animateFloatAsState(
                            targetValue = if (canRedo) 1f else 0f,
                            animationSpec = tween(220),
                            label = "redoAlpha",
                        )
                        IconButton(
                            onClick = {
                                haptics.tick()
                                viewModel.undo()
                            },
                            enabled = canUndo,
                            modifier = Modifier.graphicsLayer { alpha = undoAlpha },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = "Rückgängig",
                                tint = ink,
                            )
                        }
                        IconButton(
                            onClick = {
                                haptics.tick()
                                viewModel.redo()
                            },
                            enabled = canRedo,
                            modifier = Modifier.graphicsLayer { alpha = redoAlpha },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Redo,
                                contentDescription = "Wiederholen",
                                tint = ink,
                            )
                        }
                        // Textmarker: Farbleiste ein-/ausblenden (nur für Text-Notizen).
                        if (note.type == NoteType.TEXT && !showingBack) {
                            IconButton(onClick = {
                                haptics.tap()
                                markerBar = true
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.BorderColor,
                                    contentDescription = "Textmarker",
                                    tint = ink,
                                )
                            }
                        }
                        // Blatt umdrehen: Vorder- ↔ Rückseite
                        IconButton(onClick = {
                            haptics.fold()
                            showingBack = !showingBack
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.FlipToBack,
                                contentDescription = if (showingBack) "Vorderseite" else "Rückseite",
                                tint = if (showingBack) note.mood.earAccent() else ink,
                            )
                        }
                        DogEar(
                            folded = note.dogEarFolded,
                            accent = note.mood.earAccent(),
                            onToggle = {
                                haptics.tick()
                                viewModel.toggleDogEar()
                            },
                            onLongPress = { showMood = true },
                        )
                        // Sichtbarer Einstieg zu Stimmung / Anheften / Löschen
                        IconButton(onClick = {
                            haptics.tap()
                            showMood = true
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Optionen",
                                tint = ink,
                            )
                        }
                      }
                    }
                }

                // Sprung-Chips zu per rotem Faden verknüpften Notizen
                if (linkedNotes.isNotEmpty()) {
                    LinkedNoteChips(
                        notes = linkedNotes,
                        onOpen = { id ->
                            haptics.tap()
                            viewModel.flush()
                            onOpenLinkedNote(id)
                        },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }

                // Angehängtes Foto als Polaroid (tippen ersetzt, ✕ entfernt).
                note.photoPath?.let { path ->
                    Polaroid(
                        name = path,
                        onClick = {
                            pickPhoto.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onRemove = {
                            photoScope.launch {
                                PhotoStore.delete(context, path)
                                viewModel.setPhoto(null)
                            }
                        },
                        maxImageHeight = 220.dp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }

                // Das Blatt: Vorderseite (typabhängig) ↔ frei beschreibbare Rückseite.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationY = flip
                            cameraDistance = 12f * density
                        },
                ) {
                  if (flip <= 90f) {
                    when (note.type) {
                    NoteType.CHECKLIST -> ChecklistEditor(
                        items = items,
                        focusRequestId = focusRequestId,
                        accentColor = note.mood.earAccent(),
                        onConsumeFocusRequest = viewModel::consumeFocusRequest,
                        onToggle = viewModel::toggleItem,
                        onTextChange = viewModel::setItemText,
                        onAddAfter = viewModel::addItem,
                        onRemove = viewModel::removeItem,
                        modifier = Modifier
                            .fillMaxSize()
                            .paperRuling(note.paper, ink)
                            .padding(horizontal = 24.dp),
                        header = {
                            TitleField(
                                title = note.title,
                                onTitleChange = viewModel::onTitleChange,
                                onNext = {},
                            )
                        },
                    )

                    NoteType.STAMPCARD -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                    ) {
                        TitleField(
                            title = note.title,
                            onTitleChange = viewModel::onTitleChange,
                            onNext = {},
                        )
                        StampMotifPicker(
                            selected = note.stampMotif,
                            accent = note.mood.earAccent(),
                            onPick = {
                                haptics.tick()
                                viewModel.setStampMotif(it)
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        StampCard(
                            stamps = note.stamps,
                            motif = note.stampMotif,
                            today = LocalDate.now().toEpochDay(),
                            accent = note.mood.earAccent(),
                            onToggleDay = viewModel::toggleStamp,
                            compact = false,
                            modifier = Modifier.padding(top = 16.dp, bottom = 48.dp),
                        )
                    }

                    NoteType.SKETCH -> {
                        var penColor by remember { mutableStateOf(INK_PALETTE[0]) }
                        var penWidth by remember { mutableFloatStateOf(PEN_MEDIUM) }
                        var eraseMode by remember { mutableStateOf(false) }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                        ) {
                            TitleField(
                                title = note.title,
                                onTitleChange = viewModel::onTitleChange,
                                onNext = {},
                            )
                            SketchToolbar(
                                penColor = penColor,
                                penWidth = penWidth,
                                eraseMode = eraseMode,
                                onPenColor = { penColor = it; eraseMode = false },
                                onPenWidth = { penWidth = it; eraseMode = false },
                                onToggleErase = {
                                    haptics.tick()
                                    eraseMode = !eraseMode
                                },
                                onUndo = {
                                    haptics.tick()
                                    viewModel.undoStroke()
                                },
                                onClear = {
                                    haptics.tick()
                                    viewModel.clearSketch()
                                },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            SketchCanvas(
                                strokes = strokes,
                                penColor = penColor,
                                penWidthDp = penWidth,
                                eraseMode = eraseMode,
                                defaultColor = note.mood.earAccent(),
                                onStrokeFinished = {
                                    haptics.tick()
                                    viewModel.addStroke(it)
                                },
                                onErase = viewModel::eraseStrokes,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(top = 8.dp, bottom = 24.dp),
                            )
                        }
                    }

                    NoteType.TEXT -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .paperRuling(note.paper, ink)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                    ) {
                        TitleField(
                            title = note.title,
                            onTitleChange = viewModel::onTitleChange,
                            onNext = { bodyFocus.requestFocus() },
                        )

                        BasicTextField(
                            value = bodyValue,
                            onValueChange = { v ->
                                viewModel.onBodyChange(v.text)
                                // Letzte echte Auswahl merken – das Antippen einer Marker-Farbe
                                // oben darf den Text-Fokus verlieren, ohne die Auswahl zu vergessen.
                                if (!v.selection.collapsed) lastSelection = v.selection
                                bodyValue = v
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                            cursorBrush = SolidColor(ink),
                            visualTransformation = highlightTransformation(note.highlightRanges),
                            decorationBox = { inner ->
                                if (note.body.isEmpty()) {
                                    Text(
                                        text = "Schreib los …",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                                inner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(bodyFocus)
                                .padding(bottom = 48.dp),
                        )
                    }
                    }
                  } else {
                    BackEditor(
                        text = note.backText,
                        onTextChange = viewModel::onBackChange,
                        surface = lerp(noteSurface, SEPIA, 0.4f),
                        ink = ink,
                        modifier = Modifier.graphicsLayer { rotationY = 180f },
                    )
                  }
                }
            }
        }

        // Konfetti-Overlay über dem ganzen Editor
        confettiKey?.let { key ->
            Box(modifier = Modifier.fillMaxSize()) {
                ConfettiBurst(trigger = key, onFinished = { confettiKey = null })
            }
        }

        // Papierflieger-Animation → Android-Teilen-Auswahl (Karten-Bild, sonst Klartext)
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

        if (showMood) {
            MoodPickerSheet(
                selected = note.mood,
                pinned = note.pinned,
                hasReminder = note.hasReminder,
                sealed = note.sealed,
                invisibleInk = note.invisibleInk,
                done = note.done,
                hasExpiry = note.hasExpiry,
                hasCountdown = note.hasCountdown,
                hasCapsule = note.capsuleAt != null,
                hasPhoto = note.hasPhoto,
                paper = note.paper,
                onPick = {
                    haptics.tick()
                    viewModel.setMood(it)
                    showMood = false
                },
                onPickPaper = {
                    haptics.tick()
                    viewModel.setPaper(it)
                },
                onTogglePin = {
                    haptics.tap()
                    viewModel.togglePin()
                    showMood = false
                },
                onToggleSeal = {
                    haptics.stamp()
                    viewModel.toggleSeal()
                    showMood = false
                },
                onSetCapsule = {
                    showMood = false
                    showCapsule = true
                },
                onToggleInvisibleInk = {
                    haptics.stamp()
                    viewModel.toggleInvisibleInk()
                    showMood = false
                },
                onToggleDone = {
                    haptics.stamp()
                    viewModel.toggleDone()
                    showMood = false
                },
                onEditTags = {
                    showMood = false
                    showTags = true
                },
                onSetExpiry = {
                    showMood = false
                    showExpiry = true
                },
                onSetReminder = {
                    showMood = false
                    showReminder = true
                },
                onSetCountdown = {
                    showMood = false
                    showCountdown = true
                },
                onAttachPhoto = {
                    showMood = false
                    pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onLink = {
                    showMood = false
                    showLinkPicker = true
                },
                onClip = {
                    showMood = false
                    showClip = true
                },
                onShare = {
                    showMood = false
                    val snapshot = note
                    shareText = snapshot.toShareText()
                    val surfaceArgb = noteSurface.toArgb()
                    val inkArgb = ink.toArgb()
                    val accentArgb = accent.toArgb()
                    photoScope.launch {
                        val uri = withContext(Dispatchers.IO) {
                            ShareCardRenderer.render(context, snapshot, surfaceArgb, inkArgb, accentArgb)
                        }
                        shareUri = uri
                        if (editorBounds != Rect.Zero) {
                            shareRequest = PaperPlaneRequest(snapshot.id, editorBounds, noteSurface)
                        } else if (uri != null) {
                            context.shareImage(uri, shareText)
                        } else {
                            context.sharePlainText(shareText)
                        }
                    }
                },
                onCopy = {
                    showMood = false
                    clipboard.setText(AnnotatedString(note.toShareText()))
                },
                onDelete = {
                    showMood = false
                    haptics.crumple()
                    viewModel.moveToTrash { onBack() }
                },
                onDismiss = { showMood = false },
            )
        }

        if (showReminder) {
            ReminderSheet(
                currentReminderAt = note.reminderAt,
                currentRule = note.reminderRule,
                onPick = { at, rule ->
                    haptics.tick()
                    viewModel.setReminder(at, rule)
                    ensureNotificationPermission()
                    showReminder = false
                },
                onClear = {
                    haptics.tap()
                    viewModel.setReminder(null)
                    showReminder = false
                },
                onDismiss = { showReminder = false },
            )
        }

        if (showCapsule) {
            CapsuleSheet(
                currentCapsuleAt = note.capsuleAt,
                onPick = { at ->
                    haptics.stamp()
                    viewModel.setCapsule(at)
                    showCapsule = false
                },
                onClear = {
                    haptics.tap()
                    viewModel.setCapsule(null)
                    showCapsule = false
                },
                onDismiss = { showCapsule = false },
            )
        }

        if (showExpiry) {
            ExpirySheet(
                currentExpiresAt = note.expiresAt,
                onPick = { at ->
                    haptics.tick()
                    viewModel.setExpiry(at)
                    showExpiry = false
                },
                onClear = {
                    haptics.tap()
                    viewModel.setExpiry(null)
                    showExpiry = false
                },
                onDismiss = { showExpiry = false },
            )
        }

        if (showCountdown) {
            CountdownSheet(
                currentCountdownAt = note.countdownAt,
                onPick = { at ->
                    haptics.tick()
                    viewModel.setCountdown(at)
                    showCountdown = false
                },
                onClear = {
                    viewModel.setCountdown(null)
                    showCountdown = false
                },
                onDismiss = { showCountdown = false },
            )
        }

        if (showLinkPicker) {
            val linkedIds = linkedNotes.map { it.id }.toSet()
            NoteLinkPickerSheet(
                candidates = candidateNotes,
                linkedIds = linkedIds,
                onToggle = { otherId ->
                    haptics.tick()
                    if (otherId in linkedIds) {
                        viewModel.unlinkNotes(otherId)
                    } else {
                        viewModel.linkNotes(otherId)
                    }
                },
                onDismiss = { showLinkPicker = false },
            )
        }

        if (showClip) {
            val groupId = note.clipId ?: note.id
            val clippedIds = candidateNotes.filter { it.clipId == groupId }.map { it.id }.toSet()
            ClipPickerSheet(
                candidates = candidateNotes,
                clippedIds = clippedIds,
                onToggle = { otherId ->
                    haptics.tick()
                    viewModel.toggleClip(otherId)
                },
                onDismiss = { showClip = false },
            )
        }

        if (showTags) {
            TagPickerSheet(
                noteTags = note.tagList,
                allTags = allTags,
                onToggle = { tag ->
                    haptics.tick()
                    viewModel.toggleTag(tag)
                },
                onAdd = { tag ->
                    haptics.tick()
                    viewModel.addTag(tag)
                },
                onDismiss = { showTags = false },
            )
        }
    }
}

/** Antippbare Chips ("🧵 Titel") zu verknüpften Notizen – Tap springt direkt hinüber. */
@Composable
private fun LinkedNoteChips(
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
private fun TitleField(
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
private fun BackEditor(
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

/** Wärmerer „Papier-Unterseiten"-Ton, in den die Rückseite eingefärbt wird. */
private val SEPIA = Color(0xFFE8D7A0)
