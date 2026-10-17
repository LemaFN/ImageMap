# ImageCanvas (Paper 26.2+)

Bilder als Map-Art – hochladen über eine Webseite, per Auto-Place aufhängen, und sie **überleben Server-Neustarts**.

## So läuft es ab

1. Im Spiel `/imagemap upload` eingeben (Alias: `/ic upload`).
2. Den Link im Chat anklicken. Die Seite (GitHub Pages) öffnet sich.
3. Bild auswählen. Es wird hochgeladen, die Seite schlägt eine passende Größe vor.
4. Auf **Befehl kopieren** klicken, im Spiel mit `T` den Chat öffnen, mit `Strg+V` einfügen, Enter.
5. Mit `-auto`: vorher auf den **obersten linken Item-Frame** des Rasters schauen. Das Plugin befüllt das Raster
   nach rechts und unten. Fehlende Frames werden übersprungen, die Karten landen im Inventar.

## Befehle

Permission: `imagecanvas.use` (Standard: OP)

| Befehl | Beschreibung |
| --- | --- |
| `/imagemap upload` | Link zur Upload-Seite |
| `/imagemap create <name> <url> [BxH] [-auto] [-HD]` | Bild von URL laden und in Karten zerlegen (Standard 1x1) |
| `/imagemap give <name>` | Karten eines Bildes erneut erhalten |
| `/imagemap delete <name>` | Bild löschen |
| `/imagemap list` | Alle Bilder anzeigen |
| `/imagemap reload` | Bilder neu laden und Karten neu verbinden |

Das Bild wird auf das Raster **gestreckt**. Wähle die Größe passend zum Seitenverhältnis (die Seite macht Vorschläge).
Maximal 16 Karten pro Seite und 100 Karten insgesamt.

## Der Neustart-Fix

Minecraft speichert die Renderer von Plugins nicht. Nach einem Neustart gibt es Karten und Item-Frames noch, aber ohne Bild.

- Beim Erstellen wird das Bild in `plugins/ImageCanvas/images/<name>.png` gespeichert, die Map-IDs, URL und Größe in `data.yml`.
- Beim Start werden alle Bilder neu aufgebaut und die Renderer wieder an die Karten gehängt – zusätzlich bei jedem `MapInitializeEvent`.
- Fehlt die lokale Datei, wird sie von der gespeicherten URL neu geladen.

## Einrichten auf GitHub

1. Alte Dateien im Repo löschen, den Inhalt dieses ZIPs hineinkopieren und pushen.
2. **Settings → Pages → Source: GitHub Actions** einstellen (einmalig).
3. Der Workflow baut die Jar (**Actions → Artifacts**) und veröffentlicht die Seite aus dem Ordner `site/`.
4. Die Seite liegt dann unter `https://<dein-github-name>.github.io/<repo-name>/`. Steht dort etwas anderes als
   `https://lemafn.github.io/ImageMap/`, trage es in `plugins/ImageCanvas/config.yml` bei `upload-url` ein.

Bei Tags wie `v1.0.0` hängt der Workflow die Jar zusätzlich an ein Release.
Lokal bauen: JDK 25 und `gradle build`. Andere Paper-Version: `gradle build -PpaperApi=26.3.build.+`.

## Hinweise zur Upload-Seite

- Sie lädt Bilder anonym bei **freeimage.host** hoch (öffentlicher Demo-Key aus der Doku). Jeder mit dem Link kann das Bild sehen.
  Endpunkt und Key stehen oben im Skript in `site/index.html` (`CONFIG`).
- Bilder über 2048 Pixel werden vor dem Upload verkleinert (16 Karten × 128 Pixel).
- Der `?key=` im Link sperrt die Seite nur für Besucher, die sie nicht über `/imagemap upload` öffnen. Er ist kein Zugriffsschutz:
  Wer den Befehl dann im Spiel ausführt, braucht die Permission `imagecanvas.use`.
- Das Plugin speichert eine eigene Kopie. Verschwindet das Bild beim Host später, bleiben die Karten trotzdem erhalten.

## Installieren

Jar in `plugins/` legen, Paper 26.2 (Java 25) neu starten.
