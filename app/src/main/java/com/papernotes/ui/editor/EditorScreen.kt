package com.papernotes.ui.editor

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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.papernotes.domain.model.NoteType
import com.papernotes.ui.components.HighlightColors
import com.papernotes.ui.components.ordered
import com.papernotes.domain.model.cardSurface
import com.papernotes.domain.model.earAccent
import com.papernotes.domain.toShareText
import com.papernotes.ui.components.ConfettiBurst
import com.papernotes.ui.components.DogEar
import com.papernotes.ui.components.PaperBackground
import com.papernotes.ui.components.PaperPlaneOverlay
import com.papernotes.ui.components.PaperPlaneRequest
import com.papernotes.ui.components.Polaroid
import com.papernotes.ui.components.paperPress
import com.papernotes.ui.theme.PaperDimens
import com.papernotes.ui.theme.PaperMotion
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

    val sheets = remember { EditorSheetState() }
    var showingBack by remember { mutableStateOf(false) }
    var confettiKey by remember { mutableStateOf<Int?>(null) }
    var editorBounds by remember { mutableStateOf(Rect.Zero) }
    var shareRequest by remember { mutableStateOf<PaperPlaneRequest?>(null) }
    var shareText by remember { mutableStateOf("") }
    var shareUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val bodyFocus = remember { FocusRequester() }
    // Textmarker: Body-Feld als TextFieldValue (für die Auswahl) + sichtbare Farbleiste.
    val bodyState = remember(session) { EditorBodyState(note.body) }
    var markerBar by remember { mutableStateOf(false) }
    // Externen Lade-/Reset-Stand übernehmen, ohne die laufende Eingabe zu stören.
    LaunchedEffect(note.id, note.body) {
        if (note.body != bodyState.bodyValue.text) {
            bodyState.bodyValue = bodyState.bodyValue.copy(text = note.body)
        }
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
                    // Werkzeug-Wechsel (Icons ↔ Textmarker-Farben) blendet mit kleinem Pop über;
                    // die Breite gleitet mit, statt zu springen.
                    val showMarkerColors = markerBar && note.type == NoteType.TEXT && !showingBack
                    AnimatedContent(
                        targetState = showMarkerColors,
                        label = "editorTopTools",
                        transitionSpec = {
                            (
                                fadeIn(tween(PaperMotion.DurShort)) +
                                    scaleIn(tween(PaperMotion.DurShort), initialScale = 0.92f)
                            ) togetherWith fadeOut(tween(PaperMotion.DurShortExit)) using
                                SizeTransform(clip = false) { _, _ ->
                                    tween(PaperMotion.DurMedium, easing = PaperMotion.EaseStandard)
                                }
                        },
                    ) { markerColorsVisible ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (markerColorsVisible) {
                        // Textmarker aktiv: Farb-Swatches direkt in der oberen Leiste.
                        HighlightColors.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(26.dp)
                                    .paperPress(CircleShape) {
                                        haptics.tick()
                                        val (a, b) = bodyState.lastSelection.ordered()
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
                        // Blatt umdrehen: Vorder- ↔ Rückseite (Tönung blendet weich über).
                        val flipTint by animateColorAsState(
                            targetValue = if (showingBack) note.mood.earAccent() else ink,
                            animationSpec = tween(PaperMotion.DurMedium),
                            label = "flipTint",
                        )
                        IconButton(onClick = {
                            haptics.fold()
                            showingBack = !showingBack
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.FlipToBack,
                                contentDescription = if (showingBack) "Vorderseite" else "Rückseite",
                                tint = flipTint,
                            )
                        }
                        DogEar(
                            folded = note.dogEarFolded,
                            accent = note.mood.earAccent(),
                            onToggle = {
                                haptics.tick()
                                viewModel.toggleDogEar()
                            },
                            onLongPress = { sheets.showMood = true },
                        )
                        // Sichtbarer Einstieg zu Stimmung / Anheften / Löschen
                        IconButton(onClick = {
                            haptics.tap()
                            sheets.showMood = true
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
                }

                // Sprung-Chips zu per rotem Faden verknüpften Notizen — gleiten sanft auf/zu.
                AnimatedVisibility(
                    visible = linkedNotes.isNotEmpty(),
                    enter = expandVertically(
                        tween(PaperMotion.DurLong, easing = PaperMotion.EaseEmphasizedDecel),
                    ) + fadeIn(tween(PaperMotion.DurLong, easing = PaperMotion.EaseEmphasizedDecel)),
                    exit = shrinkVertically(tween(PaperMotion.DurShortExit)) +
                        fadeOut(tween(PaperMotion.DurShortExit)),
                ) {
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
                    EditorNoteContent(
                        note = note,
                        viewModel = viewModel,
                        items = items,
                        strokes = strokes,
                        focusRequestId = focusRequestId,
                        bodyState = bodyState,
                        bodyFocus = bodyFocus,
                        ink = ink,
                    )
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

        // Alle Bottom-Sheets des Editors; Querschnitts-Aktionen laufen über Callbacks.
        EditorSheets(
            note = note,
            sheets = sheets,
            viewModel = viewModel,
            linkedNotes = linkedNotes,
            candidateNotes = candidateNotes,
            allTags = allTags,
            onShare = {
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
            onAttachPhoto = {
                pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onDelete = { viewModel.moveToTrash { onBack() } },
            onReminderSet = ::ensureNotificationPermission,
        )
    }
}

