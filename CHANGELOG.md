# Changelog

Alle nennenswerten Änderungen an PaperNotes stehen hier.
Format nach [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
Versionierung nach [Semantic Versioning](https://semver.org/lang/de/).

## [1.0.0] - 2026-08-05

**Der erste Zettel liegt auf dem Tisch.** 🌼✒️

PaperNotes ist eine Notizen-App, die sich anfühlt wie ein liebevoll eingerichteter
Schreibtisch: cremefarbenes Papier mit echter Faser-Textur, Eselsohren in Stimmungsfarben,
Washi-Tape, Wachssiegel, roter Faden und ein Glücks-Teebeutel, an dem man morgens ziehen darf.
Alles läuft **komplett offline** — die App fragt nicht einmal nach einer Internet-Berechtigung.

### 🗒️ Zettel & Inhalte
- **Vier Papiersorten:** Fließtext, **Checkliste** (handgezeichnete Häkchen — beim letzten Haken
  regnet es Papier-Konfetti), **Stempelkarte** für Gewohnheiten und **Tinten-Skizze** zum Kritzeln.
- **Papier-Linierung:** blanko, liniert, kariert oder gepunktet.
- **Rückseite:** jede Karte hat eine Kehrseite — Eselsohr unten links umklappen, das Blatt dreht sich um.
- **Textmarker** für Wichtiges, in zarten Farben.
- **Radiergummi & Durchschlag:** Undo/Redo für Text, Rückseite, Marker und Checklisten.

### 🎨 Stimmung & Optik
- **Stimmungs-Eselsohr** und **Stimmungs-Filter** — die Punkteleiste fischt nur die Zettel einer Farbe heraus.
- **Papieroptik aus einem echten AGSL-Shader:** Faser, Korn und Vignette werden ins Papier gewebt.
- **Mitternachtspapier:** warmer Dark Mode aus tiefem Espresso-Papier und Creme-Tinte, plus weitere
  Papier-Themes, die beim Wechsel sanft überblenden.

### 🪄 Kleine Wunder
- **Knüddeln & Papierkorb:** Löschen zerknüllt die Karte zu einer Papierkugel — dort wartet sie
  30 Tage und lässt sich per Tap wieder glattstreichen.
- **Glattstreichen-Streifen:** nach Knüllen oder Archivieren macht ein Tap die letzte Aktion rückgängig.
- **Washi-Tape** zum Anpinnen, **Büroklammer-Stapel** zum Bündeln.
- **Roter Faden:** zwei Notizen verknüpfen — ein Garn spannt sich zwischen ihnen, hängt durch und atmet.
- **Wachssiegel & Geheimtinte:** Privates versiegeln (Tap bricht das Siegel) oder mit unsichtbarer
  Tinte schreiben, die erst beim Gedrückthalten erscheint.
- **Glücks-Teebeutel:** morgens am Faden ziehen — ein kleiner Glücksmoment plus Tagesstatistik.
- **Haptik:** feine Ticks, ein satter „Thunk" beim Stempeln, Knistern beim Knüllen.

### ⏳ Zeit & Erinnerung
- **Erinnerungen, auch wiederkehrend:** einmalig, täglich, werktags oder wöchentlich. Fällige
  Notizen flattern sanft, der Papier-Reiter trägt bei Wiederholung ein „↻".
- **Schlummern** direkt aus der Benachrichtigung: +10 Minuten oder +1 Stunde, ohne die Serie zu stören.
- **🕯️ Zeitkapsel:** Notiz versiegeln **und** ein Öffnungsdatum setzen — am Tag X bricht das Siegel
  von selbst auf und meldet sich.
- **Abreißkalender** als Countdown-Blatt auf der Karte.
- **Vergängliche Notizen:** kurz vor Ablauf vergilbt das Papier, die Ecke rollt sich ein, dann
  zerknüllt sich der Zettel von selbst.

### ✈️ Teilen & Hereinholen
- **Als Papierkarte teilen:** die Notiz wird als Karten-Bild gerendert und faltet sich zum
  Papierflieger, der davonsegelt — dann öffnet sich die Teilen-Auswahl.
- **Einkleben:** Text oder Link aus einer anderen App teilen landet als frischer Zettel.
- **Schnellzettel:** App-Icon lang drücken → „Neuer Zettel".
- **🪟 Haftnotiz-Widget:** eine Notiz als echter Papierzettel auf dem Homescreen, Farbe frei wählbar,
  **Checklisten direkt im Widget abhakbar** (bis zu 6 Zeilen, Überhang als „+N weitere").
- **Foto-Polaroid:** ein Bild anheften, es klebt als kleines Polaroid auf der Karte.
- **Sicherung:** alle Notizen samt Fotos und Fäden als ZIP exportieren und nicht-destruktiv
  wieder einspielen.

### 🗂️ Ordnung & Bewegung
- **Schreibtisch-Agenda:** alles mit Termin, Erinnerung oder Ablauf auf einen Blick.
- **Anordnen-Modus** mit übermütig wackelnden Karten, **Sortierung** nach Pinnwand, Datum oder Titel.
- **Mehrfachauswahl** per Gedrückthalten: gesammelt anheften, umfärben, taggen, archivieren, knüllen.
- **Tinten-Suche:** findet auch Karteireiter und Rückseiten, Nicht-Treffer verblassen wie verdünnte Tinte.
- **Gesten:** Wisch-zum-Archivieren, Pinch-to-Zoom für die Spaltenzahl.
- **Erledigt-Stempel:** fertige Notizen schräg abstempeln, statt sie zu löschen.

### ⚡ Technik & Qualität
- **Jetpack Compose** + **Material 3**, Room für die Notizen, DataStore für Einstellungen, Hilt für DI.
- **Baseline Profiles** werden in die APK eingebacken — der Kaltstart ist ab dem zweiten Start
  AOT-vorkompiliert; die Shader-Textur wird einmalig in ein Bitmap gebacken, Animationen sind gegatet.
- **Room-Migrationen bis Schema 17** samt Migrationstest von Version 1 an.
- **Unit-Test-Fundament** für die Domain-Schicht, CI baut und testet jeden Push auf `main`.
- **Release-Signing** über einen Upload-Key vorbereitet (`keystore.properties`, siehe README).

### 📋 Voraussetzungen
- **Android 13 (API 33)** oder neuer.
- Berechtigungen: Vibration, Benachrichtigungen, exakte Alarme (für Erinnerungen), Neustart-Empfang.
  **Keine Internet-Berechtigung** — die App sendet nichts nach draußen.

[1.0.0]: https://github.com/rbatmaz69/PaperNotes/releases/tag/v1.0.0
