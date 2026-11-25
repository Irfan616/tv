# Grace TV — Android IPTV Player

Grace TV is a modern, ad-free Android IPTV Player built with **Kotlin**, **Jetpack Compose (Material 3)**, **AndroidX Media3 ExoPlayer**, **Room**, **DataStore**, and **Retrofit/OkHttp**.

> **Player-Only Notice:** Grace TV does not bundle, host, or provide any IPTV channels, streams, or subscriptions. Users must supply their own legally authorized playlists or credentials.

---

## Core Features

- **Playlist Management (`PlaylistScreen`, `PlaylistRepository`)**
  - **Xtream Codes API:** Authenticate via `player_api.php`, retrieve account metadata, live categories/channels, VOD categories/movies, and series categories/shows/episodes.
  - **M3U / M3U8 URL:** Streaming buffered parser (`M3uParser`) extracting channel names, stream URLs, `group-title`, `tvg-id`, `tvg-name`, `tvg-logo`, and `url-tvg` EPG headers without freezing the UI.
  - **Local M3U File Import:** Storage Access Framework (`ActivityResultContracts.OpenDocument`) with zero broad storage permissions.
  - Edit, delete, refresh, and set default across multiple playlists.
- **Home Dashboard (`HomeScreen`)**
  - Active playlist status, HTTP security warning when connecting over unencrypted `http://`, quick-launch shortcuts, Continue Watching, Recently Watched, and Recently Added rows.
- **Live TV, Movies & Series (`LiveTvScreen`, `MoviesScreen`, `MovieDetailScreen`, `SeriesScreen`, `SeriesDetailScreen`)**
  - Category filtering, search, TV-style list & grid views, on-demand VOD/Series detail fetching, season tabs, episode playback, and favorites.
- **Media3 ExoPlayer (`GracePlayerManager`, `PlayerScreen`)**
  - Supports HLS (`.m3u8`), MPEG-DASH (`.mpd`), and progressive MP4/TS/MKV streams.
  - Play/Pause, seekbar for seekable VOD, 10s rewind/forward, full-screen landscape toggle, aspect ratio cycling, volume/mute, playback speed (`0.5x`–`2.0x`), audio & subtitle track selection, next/previous channel or episode switching, Picture-in-Picture (PiP), error recovery with Retry, and clean player release.
- **XMLTV Electronic Program Guide (`EpgScreen`, `XmltvParser`, `EpgRepository`)**
  - Download, parse, and cache XMLTV EPG feeds, match channels by `tvg-id`, handle timezone offsets, and view Now/Next & full programme timelines.
- **Favorites, Global Search & Parental Controls**
  - Persistent Room favorites filtered by Live TV, Movies, Series, and Episodes.
  - Instant case-insensitive local search across channels, movies, series, and categories with recent searches.
  - Salted SHA-256 PIN lock enforced across restricted categories and channels.
- **Security**
  - Sensitive Xtream credentials encrypted with Android Keystore AES-GCM (`KeystoreCredentialCipher`).
  - No hardcoded credentials or credential logging.
- **Contact & Support (`SupportScreen`)**
  - Direct Phone dialer (`03066000616`), WhatsApp (`03066000616`), and Email (`irfanjameel293@gmail.com`) actions.

---

## Project Structure

- `app/src/main/java/com/example/MainActivity.kt` — App entry activity (`enableEdgeToEdge`) launching `GraceTvApp`.
- `app/src/main/java/com/gracetv/iptv/`
  - `data/local/` — Room database (`GraceTvDatabase`), 11 entities, DAOs, and DataStore `PreferencesManager`.
  - `data/remote/` — `XtreamApiService`, `XtreamDto`, `M3uParser`, `XmltvParser`.
  - `data/repository/` — `PlaylistRepository`, `ContentRepository`, `EpgRepository`, `NetworkErrorMapper`.
  - `domain/model/` — Domain enums and parsed models.
  - `player/` — `GracePlayerManager` (Media3 ExoPlayer wrapper).
  - `security/` — `KeystoreCredentialCipher` and `PinHasher`.
  - `ui/` — Theme (`Color.kt`, `Type.kt`, `Theme.kt`), reusable components, navigation (`GraceTvApp.kt`, `Routes.kt`), and feature screens/ViewModels.

---

## Building the APK

### Debug APK (Command Line)
```bash
./gradlew assembleDebug
```
*(In environments without the Gradle wrapper script, run `gradle assembleDebug`.)*

Output APK path:
`app/build/outputs/apk/debug/app-debug.apk`

### Signed Release APK (Android Studio)
1. Open the project folder in **Android Studio**.
2. Wait for Gradle sync to finish.
3. Choose **Build > Generate Signed App Bundle / APK...**
4. Select **APK** (or **Android App Bundle** for Google Play) and click **Next**.
5. Choose an existing `.jks` keystore or click **Create new...**, enter keystore/key passwords and alias, then click **Next**.
6. Select the **release** build variant and click **Create**.

### Running Unit & Robolectric Tests
```bash
./gradlew :app:testDebugUnitTest
```

---

## Notes & Verification Scope
- **Live Stream Verification:** Because Grace TV ships with zero bundled IPTV streams, end-to-end playback against a live provider stream must be verified by the user on a physical device or emulator using their own authorized Xtream Codes or M3U playlist.
- **Android TV:** D-pad focus highlights (`tvFocusHighlight`), `LEANBACK_LAUNCHER` intent category, and TV-friendly channel grids are implemented; testing on a physical Android TV / Google TV device is recommended for remote-control ergonomics.
