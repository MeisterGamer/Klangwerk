# Klangwerk

**Klangwerk** ist ein professioneller, lokal arbeitender Musik- und Video-Player für Android mit Material You Dynamic Color im Dunkelmodus, integrierter Ordner- und Medienverwaltung, Wiedergabelisten, Sofortsuche sowie einem 5-Band-Equalizer.

---

## Funktionsumfang

- **Medienbibliothek & Ordneransicht**:
  - Automatischer Scan lokaler Audiodateien über `MediaStore` (MP3, FLAC, AAC/M4A, OGG, Opus, WAV, ALAC u. a.).
  - Manueller Einzel-, Mehrfach- und Ordnerimport über das Storage Access Framework (`OpenDocument` / `OpenDocumentTree`) sowie den Android Photo Picker für Videos.
  - Übersichtliche Filter-Tabs: **Titel**, **Alben**, **Interpreten**, **Ordner** (zeigt Geräte- und verknüpfte Ordner gruppiert mit Titelanzahl, Gesamtdauer und Direkt-Wiedergabe) sowie **Importiert**.
  - Sortierung nach Titel, Interpret, Dauer oder Hinzufügedatum sowie globale Sofortsuche über Titel, Alben, Interpreten, Playlists und Videos.
- **Wiedergabelisten (Playlists)**:
  - Automatische Listen für *Favoriten* und *Zuletzt gespielt*.
  - Eigene Playlists lokal über Room speichern, umbenennen, löschen und Titelreihenfolge anpassen.
- **Audio- & Video-Player (Jetpack Media3)**:
  - `ExoPlayer` + `MediaSessionService` für zuverlässige Hintergrundwiedergabe, Medienbenachrichtigung und Headset-/Sperrbildschirm-Steuerung.
  - Schwebender Mini-Player und vollflächiges *Now-Playing*-Overlay mit Warteschlangen-Verwaltung und Wiedergabegeschwindigkeit (`0.75x`–`2.0x`).
  - Lokaler Video-Player mit Vollbild-/Querformat-Umschalter, 10-Sekunden-Sprüngen, Seitenverhältnis-Wechsel und aktiver Anbindung an den Equalizer.
- **5-Band-Equalizer & Klangstudio**:
  - Interaktiver 5-Band-Frequenzgang (`60 Hz`–`14 kHz`) mit grafischer Kurve, Klangprofilen (*Linear*, *Bass-Boost*, *Rock*, *Pop*, *Jazz*, *Klassik*, *Sprache*, *Benutzerdefiniert*), **Bass-Boost** und **3D-Virtualizer**.
- **Akkuschonendes Design**:
  - Material You Dynamic Color im Dunkelmodus (`dynamicDarkColorScheme` ab Android 12) + optionales **reines AMOLED-Schwarz** in den Einstellungen.
  - Adaptives Layout mit `NavigationBar` auf Smartphones und `NavigationRail` auf Tablets/Foldables.

---

## Wichtiger Hinweis zu PWABuilder (`pwabuilder.com`)

Die Haupt-App (**Klangwerk** unter `app/`) ist eine **native Android-App** (Kotlin, Jetpack Compose, Media3, Room, `android.media.audiofx`). Eine native APK- oder Play-Store-AAB-Datei wird daher direkt mit Gradle erzeugt – **PWABuilder wird für die native Android-App selbst nicht benötigt**.

Falls du zusätzlich eine Web-/PWA-Variante über **[https://www.pwabuilder.com/](https://www.pwabuilder.com/)** paketieren möchtest (z. B. für Browser, Windows Store oder TWA), liegt im Verzeichnis **`pwa/`** ein vollständig vorbereitetes PWABuilder-Paket bei.

### Inhalt des `pwa/`-Verzeichnisses

- `pwa/manifest.webmanifest` – Vollständiges Web App Manifest (`standalone`, Icons `192x192`, `512x512` & `maskable`, `categories`, `file_handlers`).
- `pwa/sw.js` – Offline-fähiger Service Worker für das App-Shell-Caching.
- `pwa/index.html` – Eigenständige Web-Companion-Oberfläche mit Datei-/Ordner-Auswahl, Suche und Web-Audio-5-Band-Equalizer.
- `pwa/icons/` – Generierte PNG-Icons in `192x192`, `512x512` und `512x512 (maskable)`.
- `pwa/.well-known/assetlinks.json` – Vorlage für Digital Asset Links mit der Android-Paket-ID `com.aistudio.klangwerk.m3avkp`.

### Schritte für PWABuilder

1. **HTTPS-Hosting**: Lade den Inhalt des Ordners `pwa/` auf einen HTTPS-Webspace hoch (z. B. GitHub Pages, Cloudflare Pages, Netlify oder Vercel), sodass `https://<deine-domain>/manifest.webmanifest` und `https://<deine-domain>/.well-known/assetlinks.json` öffentlich erreichbar sind.
2. **Mit PWABuilder prüfen**: Öffne [https://www.pwabuilder.com/](https://www.pwabuilder.com/), gib deine HTTPS-URL ein und prüfe Manifest, Service Worker und Icons.
3. **Digital Asset Links (`assetlinks.json`) verknüpfen**:
   - Trage in `pwa/.well-known/assetlinks.json` deinen tatsächlichen **SHA-256 Signing-Fingerprint** (aus deinem Release-Keystore bzw. aus der Google Play Console unter *App-Signatur*) ein.
   - Um die URL nativ in der Android-App zu verifizieren, ergänze bei Bedarf in `app/src/main/AndroidManifest.xml` ein `<meta-data>`-Tag für `asset_statements` mit deiner produktiven HTTPS-Domain.

---

## Build & Tests (Android)

### Voraussetzungen

- JDK 11+
- Android SDK (API 36, `minSdk = 24`)

### Befehle

```bash
# Debug-APK bauen
gradle :app:assembleDebug

# Release-Bundle (.aab) für Google Play bauen
gradle :app:bundleRelease

# Lokale Unit- & Robolectric-Tests ausführen
gradle :app:testDebugUnitTest
```

### Verwendete Berechtigungen

- `READ_MEDIA_AUDIO` (Android 13+) bzw. `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`): Automatischer Scan lokaler Audiodateien via `MediaStore`.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Hintergrundwiedergabe über Media3 `MediaSessionService`.
- `POST_NOTIFICATIONS` (Android 13+): Anzeige der Mediensteuerung in den Systembenachrichtigungen.
- `WAKE_LOCK`: Stabile Audio-Wiedergabe bei ausgeschaltetem Bildschirm.
