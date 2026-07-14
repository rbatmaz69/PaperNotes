# PaperNotes 🌼✒️

> *Ein Schreibtisch voll warmem Papier in deiner Tasche.*

PaperNotes ist eine Notizen-App, die sich anfühlt wie ein liebevoll eingerichteter
Schreibtisch: cremefarbenes Papier mit echter Faser-Textur, Eselsohren in Stimmungsfarben,
Washi-Tape, Wachssiegel, roter Faden und ein Glücks-Teebeutel, an dem man morgens ziehen darf.
Kein nüchternes Listen-Tool — sondern ein kleiner Ort, an dem Notizen **knistern, flattern,
sich zerknüllen und wieder glattstreichen lassen.** 🪄

Muji-Ruhe trifft Material 3 — minimal, aber an allen richtigen Stellen verspielt.

---

## ✨ Was dieser Schreibtisch alles kann

### 🗒️ Zettel & Inhalte
- **Vier Papiersorten:** Fließtext, **Checkliste** (handgezeichnete Häkchen — beim letzten
  Haken regnet es Papier-Konfetti 🎉), **Stempelkarte** für Gewohnheiten und **Tinten-Skizze**
  zum Kritzeln.
- **Liniertes Papier:** blanko, liniert, kariert oder gepunktet — wie ein echtes Notizheft.
- **Rückseite:** jede Karte hat eine Kehrseite — Eselsohr unten links umklappen und das
  Blatt **dreht sich um.** 🔄
- **Textmarker:** Wichtiges in zarten Farben markieren.

### 🎨 Stimmung & Optik
- **Stimmungs-Eselsohr:** die Ecke umknicken — die Farbe verrät die Stimmung der Notiz.
- **Stimmungs-Filter:** die Punkteleiste fischt nur die Zettel einer Farbe heraus.
- **Papieroptik:** ein echter AGSL-Shader webt Faser, Korn & Vignette ins Papier
  ([`PaperBackground`](app/src/main/java/com/papernotes/ui/components/PaperBackground.kt),
  [`Shaders.kt`](app/src/main/java/com/papernotes/util/Shaders.kt)).
- **Mitternachtspapier:** ein warmer Dark Mode aus tiefem Espresso-Papier & Creme-Tinte 🌙 —
  plus weitere Themes, die beim Wechsel sanft überblenden.

### 🪄 Kleine Wunder
- **Knüddeln & Papierkorb:** Löschen **zerknüllt** die Karte zu einer Papierkugel und wirft sie
  in den Korb — dort wartet sie 30 Tage und lässt sich per Tap wieder **glattstreichen**
  ([`CrumpleOverlay`](app/src/main/java/com/papernotes/ui/components/CrumpleOverlay.kt)).
- **Glattstreichen-Streifen:** nach dem Zerknüllen oder Archivieren lugt kurz ein Papierstreifen
  hervor — ein Tap macht die letzte Aktion **rückgängig**
  ([`PaperSnackbar`](app/src/main/java/com/papernotes/ui/components/PaperSnackbar.kt)).
- **Radiergummi & Durchschlag:** im Editor lassen sich Text-, Rückseiten-, Marker- und
  Checklisten-Änderungen **rückgängig machen und wiederholen** (Undo/Redo).
- **Washi-Tape:** Lieblingszettel mit einem schief geklebten Streifen oben anpinnen.
- **Roter Faden:** zwei Notizen verknüpfen — ein Garn spannt sich zwischen ihnen, hängt durch
  und **atmet** ([`RedThreadOverlay`](app/src/main/java/com/papernotes/ui/components/RedThreadOverlay.kt)).
- **Büroklammer-Stapel:** zusammengehörige Zettel zu einem Bündel klammern.
- **Wachssiegel & Geheimtinte:** private Notizen versiegeln (Tap **bricht** das Siegel) oder mit
  unsichtbarer Tinte schreiben, die erst beim Gedrückthalten erscheint. 🤫
- **Glücks-Teebeutel:** morgens am Faden ziehen → ein kleiner Glücksmoment + Tagesstatistik
  ([`TeabagPull`](app/src/main/java/com/papernotes/ui/components/TeabagPull.kt)).

### ⏳ Zeit & Erinnerung
- **Erinnerungen — jetzt wiederkehrend:** einmalig, **täglich, werktags oder wöchentlich.**
  Fällige Notizen *flattern* sanft, der Papier-Reiter trägt bei Wiederholung ein „↻"
  ([`ReminderRule`](app/src/main/java/com/papernotes/domain/model/ReminderRule.kt)).
- **Schlummern:** direkt aus der Benachrichtigung um **+10 Minuten oder +1 Stunde** vertagen —
  wiederkehrende Serien bleiben davon unberührt.
- **🕯️ Zeitkapsel:** eine Notiz mit Wachs versiegeln **und** ein Öffnungsdatum setzen — sie
  bleibt verschlossen (Tap zeigt nur „Versiegelt bis …"), und am Tag X **bricht das Siegel von
  selbst** auf und meldet sich: *„Ein Brief hat sich geöffnet."*
  ([`CapsuleSheet`](app/src/main/java/com/papernotes/ui/components/CapsuleSheet.kt)).
- **Abreißkalender:** ein Zieldatum als Countdown-Blatt auf der Karte.
- **Vergängliche Notizen:** mit Ablaufzeit — kurz vor Schluss **vergilbt** das Papier und die
  Ecke rollt sich ein, dann zerknüllt sich der Zettel von selbst. 🍂

### ✈️ Teilen & Hereinholen
- **Als Papierkarte teilen:** die Notiz wird als **hübsches Karten-Bild** gerendert und faltet
  sich zum **Papierflieger**, der davonsegelt — dann öffnet sich die Teilen-Auswahl
  ([`ShareCardRenderer`](app/src/main/java/com/papernotes/util/ShareCardRenderer.kt),
  [`PaperPlaneOverlay`](app/src/main/java/com/papernotes/ui/components/PaperPlaneOverlay.kt)).
- **Einkleben:** Text oder Link aus einer anderen App teilen → landet als frischer Zettel. 📎
- **Schnellzettel:** App-Icon lang drücken → **„Neuer Zettel"** und sofort losschreiben.
- **🪟 Haftnotiz-Widget:** eine ausgewählte Notiz als echter Papierzettel auf dem Homescreen —
  **Farbe frei wählbar** (Stimmungsfarbe der Notiz oder ein festes Papier-Theme), Tippen öffnet
  sie; aktualisiert sich nach dem Bearbeiten. **Checklisten sind direkt im Widget abhakbar**
  (bis zu 6 Zeilen, Überhang als „+N weitere")
  ([`StickyNoteWidget`](app/src/main/java/com/papernotes/ui/widget/StickyNoteWidget.kt)).
- **Foto-Polaroid:** ein Bild anheften — es klebt als kleines Polaroid auf der Karte.
- **Sicherung:** alle Notizen (samt Fotos & Fäden) in eine ZIP exportieren und wieder
  einspielen — nicht-destruktiv ([`BackupManager`](app/src/main/java/com/papernotes/data/backup/BackupManager.kt)).

### 🗂️ Ordnung & Bewegung
- **Schreibtisch-Agenda:** alles mit Termin/Erinnerung/Ablauf auf einen Blick.
- **Anordnen-Modus:** Karten frei umsortieren — sie *wackeln* übermütig dabei.
- **Sortierung:** Pinnwand (frei angeordnet), zuletzt erstellt oder Titel A–Z — Washi-Tape-Zettel
  bleiben immer oben ([`SortSheet`](app/src/main/java/com/papernotes/ui/components/SortSheet.kt)).
- **Mehrfachauswahl:** Karte **gedrückt halten** → ausgewählte Zettel bekommen eine kleine rote
  Heftzwecke und lassen sich gesammelt anheften, umfärben, taggen, archivieren oder zerknüllen
  ([`SelectionBar`](app/src/main/java/com/papernotes/ui/components/SelectionBar.kt)).
- **Tinten-Suche:** Lupe antippen; findet auch **Karteireiter und Rückseiten**; Nicht-Treffer
  **verblassen wie verdünnte Tinte.**
- **Einstellungen-Zettel:** Papier-Theme, Sicherung und „Über" gebündelt in einem Sheet
  ([`SettingsSheet`](app/src/main/java/com/papernotes/ui/components/SettingsSheet.kt)).
- **Gesten:** Wisch-zum-Archivieren, Pinch-to-Zoom für die Spaltenzahl, schwebende Schatten.
- **Erledigt-Stempel:** fertige Notizen schräg abstempeln, statt sie zu löschen.
- **Haptik:** feine Ticks, ein satter „Thunk" beim Stempeln, Knister beim Knüllen.

> Alles bleibt **minimal & funktional** — jedes Gimmick hat einen Zweck, und nichts läuft, wenn
> es nicht gebraucht wird (gegatete Animationen halten das Papier leise und die App flott).

---

## 🛠️ Tech-Stack
- **Jetpack Compose** (Kotlin) · **Material 3**
- **minSdk 33** (Android 13) · compile/target **SDK 35** — `RuntimeShader`/AGSL fürs Papier
- **Room** (Notizen) · **DataStore** (Teebeutel & Theme) · **Hilt** (DI)
- Shared-Element-Transitions, Spring-Physik, AGSL-Shader, native Haptik, `AlarmManager`
- **Flott von der ersten Sekunde:** Baseline Profiles (AOT-Vorkompilierung), in ein Bitmap
  gebackene Shader-Textur, gegateter Animations-Overhead

## 🗺️ Projektstruktur
```
app/src/main/java/com/papernotes/
  domain/model/   Note, MoodCategory, ReminderRule, DailyDelight
  domain/         Codecs (Checkliste/Skizze/Stempel/Marker), NoteShare
  data/           Room (local), repository, backup, prefs (DataStore)
  di/             Hilt-Module
  reminder/       AlarmManager-Scheduler, Reminder-/Capsule-/Boot-Receiver
  ui/theme/       Glücks-Farbpalette, Typo, Shapes
  ui/components/  PaperBackground, NoteCard, DogEar, WaxSeal, CapsuleSheet, RedThreadOverlay …
  ui/notes/       Raster-Screen + ViewModel
  ui/editor/      Editor-Screen + ViewModel
  ui/agenda/      Schreibtisch-Agenda
  ui/navigation/  SharedTransition-Container
  util/           Haptics, Shaders, ShareCardRenderer, Context-Helper
baselineprofile/  Macrobenchmark, der das Baseline-/Startup-Profil erzeugt
```

## 🚀 Bauen & Starten
Voraussetzung: Android Studio (Ladybug+) oder Android SDK 35 + **JDK 17**.
Gebaut wird mit dem eingecheckten **`gradlew`-Wrapper** — keine lokale Gradle-Installation nötig.
Vorher die Umgebung setzen:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

### ✏️ Debug (schnell iterieren)
```bash
./gradlew :app:installDebug   # baut & installiert auf Gerät/Emulator (API 33+)
```
In Android Studio: Projekt öffnen → Gerät (API 33+) → **Run**.

> ⚠️ Der Debug-Build ist **absichtlich unoptimiert** (kein R8, `debuggable`, kein AOT-Profil) und
> ruckelt spürbar. Prima zum Entwickeln — aber **nie** zur Beurteilung der gefühlten Performance.

### 🌟 Release (so fühlt es sich wirklich an)
Der `release`-Build (R8 + Shrinking) ist mit dem Debug-Schlüssel signiert, also ohne eigenes
Keystore direkt installierbar:
```bash
./gradlew :app:installRelease
```
In Android Studio alternativ **Build → Select Build Variant → `:app` → `release`**, dann **Run**.
(WLAN-Debugging geht genauso — `adb` sieht das Gerät.)

### 🏪 Play-Store-Release
Der Release-Build signiert mit dem **Debug-Schlüssel**, solange kein eigener Upload-Key
eingerichtet ist — gut zum Testen, aber Google Play akzeptiert das nicht. Einmalig:

```bash
# 1. Upload-Keystore erzeugen (Passwort gut verwahren – ohne ihn kein Update!)
keytool -genkeypair -v -keystore papernotes-upload.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000

# 2. keystore.properties im Projekt-Root anlegen (liegt in .gitignore, bleibt lokal):
#    storeFile=papernotes-upload.jks
#    storePassword=…
#    keyAlias=upload
#    keyPassword=…
```

Dann baut `./gradlew :app:bundleRelease` das signierte Bundle
(`app/build/outputs/bundle/release/app-release.aab`) für die Play Console.

Für jedes weitere Release: `versionCode` in `app/build.gradle.kts` erhöhen.
In der Play Console außerdem nötig: Datenschutzerklärung (die App arbeitet komplett
offline, keine `INTERNET`-Berechtigung), Data-Safety-Formular, Screenshots & Grafiken.

### ⚡ Baseline Profile (Kaltstart-Zauber)
Ein eingecheckter Profil-Datensatz (`app/src/release/generated/baselineProfiles/`) wird bei jedem
Release-Build automatisch eingebacken — **pro Build kein Schritt nötig.** Neu erzeugen lohnt nur
nach größeren UI-Umbauten (Gerät verbunden):
```bash
./gradlew :app:generateBaselineProfile
```
ProfileInstaller wendet es kurz nach dem ersten Start an (der **zweite** Kaltstart ist der
schnellste). Sofort erzwingen:
```bash
adb shell cmd package compile -m speed-profile -f com.papernotes
```

Die `@Preview`-Composables in
[`Previews.kt`](app/src/main/java/com/papernotes/ui/preview/Previews.kt) zeigen Papier & Karten
direkt in der IDE.

> Hinweis: `local.properties` (SDK-Pfad) wird lokal erzeugt und nicht eingecheckt.

---

<p align="center"><i>Mit Tinte, Wachs und ein bisschen Konfetti gemacht. ✿</i></p>
