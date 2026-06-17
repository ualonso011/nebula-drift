# Design: Infrastructure (v0.4.0)

## Technical Approach

Add two new screens (Settings, Leaderboard), a shared `UiComponents` utility, and a `LeaderboardManager` singleton — all using the existing custom rendering pattern (`ShapeRenderer` + `SpriteBatch` + `BitmapFont`, world units via `FitViewport`). Extend `I18nManager` with `setLocale()`. Fix Android manifest orientation and version. Wire new navigation buttons into `MenuScreen` and `GameOverScreen`.

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|----------|----------|----------|--------|-----------|
| I18nManager shape | Add `setLocale()` to existing class | Convert to singleton object | **A** | Existing class is constructor-injected into all screens. Converting to object breaks DI pattern. Just add `setLocale(locale: String)` + `getLocale(): String`. |
| Leaderboard storage | `object` singleton + JSON prefs | SQLite / Room | **A** | Max 10 entries. libGDX `Json` serializer already in classpath. Matches `AudioManager` singleton pattern. No ORM overhead. |
| Slider units | World units (match viewport) | Pixel-based | **World units** | All existing screens use `FitViewport(16, 9)`. Slider bar: 4.0 x 0.2 world units, thumb: 0.4 diameter. Consistent with button sizing. |
| UiComponents | `object` with static draw helpers | Base class for screens | **Object** | Screens already inherit `KtxScreen`. Composition over inheritance. Eliminates `drawButton`/`drawButtonLabel` duplication in GameOverScreen. |
| Language switch | setLocale + transition to MenuScreen | Rebuild current screen in-place | **Transition to MenuScreen** | Existing `startTransition` handles screen recreation cleanly. All screens re-read i18n on `show()`. No partial state issues. |
| Name entry | 6 predefined name buttons | Text input / keyboard | **Predefined** | Out-of-scope: on-screen keyboard. Matches proposal. 6 buttons fit in world coords. |
| Leaderboard highlight | Top 3 gold/silver/bronze color | Index badges | **Color** | Simpler rendering. Gold `(0.8, 0.6, 0)`, Silver `(0.7, 0.7, 0.7)`, Bronze `(0.6, 0.4, 0.2)`. |

## Data Flow

```
MenuScreen ──[Settings]──→ SettingsScreen ──[Back]──→ MenuScreen
    │                                                    ↑
    └──[Leaderboard]──→ LeaderboardScreen ──[Back]──────┘

GameScreen ──[die]──→ GameOverScreen
                          │
                    isHighScore?
                     ├─ Yes → show 6 name buttons
                     │        └─[select name]──→ LeaderboardManager.addEntry()
                     │                           └──→ LeaderboardScreen
                     └─ No → show Leaderboard button
                              └──→ LeaderboardScreen

SettingsScreen sliders ──→ AudioManager.setMusicVolume/setSfxVolume
                          └──→ prefs.flush() (persist)

SettingsScreen language ──→ I18nManager.setLocale(newLocale)
                           └──→ game.startTransition { setScreen<MenuScreen>() }
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `screens/SettingsScreen.kt` | Create | Volume sliders (music/SFX), language toggle button, back button. World-coordinate touch handling. |
| `screens/LeaderboardScreen.kt` | Create | Top 10 list with gold/silver/bronze highlights. Back button. |
| `managers/LeaderboardManager.kt` | Create | Singleton object. JSON serialization via libGDX `Json`. Max 10 entries. `isHighScore()`, `addEntry()`, `getEntries()`. |
| `rendering/UiComponents.kt` | Create | `object` with `drawButton()` and `drawSlider()` helpers. Shared by all menu/settings/leaderboard screens. |
| `managers/I18nManager.kt` | Modify | Add `setLocale(locale: String)` and `getLocale(): String` methods. Reload bundle on locale change. |
| `screens/MenuScreen.kt` | Modify | Add Settings + Leaderboard buttons. Refactor to use `UiComponents.drawButton()`. |
| `screens/GameOverScreen.kt` | Modify | Add name entry UI (6 predefined names) when `isHighScore`. Add Leaderboard button. Refactor to `UiComponents`. |
| `NebulaDriftGame.kt` | Modify | Register `SettingsScreen` and `LeaderboardScreen` in `create()`. |
| `util/Constants.kt` | Modify | Add `GAME_VERSION = "0.4.0"`, `LEADERBOARD_MAX_ENTRIES = 10`, `PREDEFINED_NAMES` list. |
| `assets/i18n/messages_*.properties` | Modify | Add 9 new keys: settings, music_volume, sfx_volume, language, back, leaderboard, name, select_name, rank. |
| `android/src/main/AndroidManifest.xml` | Modify | `portrait` → `sensorLandscape`, add `keepScreenOn`, add `Theme.Black.NoTitleBar.Fullscreen`. |
| `android/build.gradle.kts` | Modify | `versionCode = 4`, `versionName = "0.4.0"`. |
| `CHANGELOG.md` | Create | Project root. Versions v0.1.0 through v0.4.0. |
| `test/.../LeaderboardManagerTest.kt` | Create | Insertion, sorting, max-10 cap, tie-breaking, JSON round-trip, `isHighScore`. |
| `test/.../I18nManagerTest.kt` | Create | `setLocale` changes bundle, `getLocale` returns current, fallback key. |
| `test/.../VersionTest.kt` | Create | `GAME_VERSION` non-empty. |

## Interfaces / Contracts

### LeaderboardManager

```kotlin
data class LeaderboardEntry(
    val name: String,
    val score: Int,
    val time: Float,
    val date: String
)

object LeaderboardManager {
    fun getEntries(): List<LeaderboardEntry>
    fun addEntry(entry: LeaderboardEntry)
    fun isHighScore(score: Int): Boolean
    fun clear()  // for testing
}
```

### I18nManager additions

```kotlin
// Existing class — add two methods:
fun setLocale(locale: String)  // "eu" | "es" | "en" → reloads bundle
fun getLocale(): String        // returns current locale code
```

### UiComponents

```kotlin
object UiComponents {
    fun drawButton(
        sr: ShapeRenderer, sb: SpriteBatch, font: BitmapFont,
        bounds: Rectangle, label: String,
        bgColor: Color = Color(0.15f, 0.35f, 0.6f, 1f),
        borderColor: Color = Color(0.3f, 0.5f, 0.8f, 1f)
    )

    fun drawSlider(
        sr: ShapeRenderer, sb: SpriteBatch, font: BitmapFont,
        bounds: Rectangle, value: Float, label: String
    )
}
```

### LeaderboardEntry JSON schema

```json
[
  {"name": "Ace", "score": 1500, "time": 120.5, "date": "2026-06-17"}
]
```

Stored in `Gdx.app.getPreferences("nebula-drift")` under key `"leaderboard"`.

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | LeaderboardManager insertion + sort | Add entries, verify descending score order |
| Unit | LeaderboardManager max 10 | Add 11 entries, verify `getEntries().size == 10` |
| Unit | LeaderboardManager tie-breaking | Same score, verify lower time ranks higher |
| Unit | LeaderboardManager JSON round-trip | Save → load → verify all fields intact |
| Unit | LeaderboardManager.isHighScore | Empty list → true; below lowest → false; above lowest → true |
| Unit | I18nManager.setLocale | Set "es", verify `getLocale() == "es"` and `get("play")` returns Spanish |
| Unit | GAME_VERSION | Assert non-empty, matches expected format |
| Integration | Settings → AudioManager | Slider drag → verify `AudioManager.musicVolume` updated |
| Regression | All 257 existing tests | Must pass unchanged |

## Migration / Rollout

No data migration. Leaderboard prefs key is new (`"leaderboard"`). Existing `"nebula-drift"` prefs (highScore, music_volume, sfx_volume) untouched.

Two PRs:
1. **PR #1**: Core infrastructure — LeaderboardManager, UiComponents, I18nManager.setLocale, Constants.GAME_VERSION, i18n keys, Android manifest, CHANGELOG.md, all tests.
2. **PR #2**: Screens — SettingsScreen, LeaderboardScreen, MenuScreen/GameOverScreen wiring, name entry flow.

## Open Questions

- [ ] **Date format for leaderboard entries**: Use `java.time.LocalDate.now().toString()` (ISO 8601) or SimpleDateFormat? *Default: ISO 8601 via LocalDate.*
- [ ] **Slider visual feedback**: Show percentage text above slider or beside it? *Default: label above, percentage right-aligned.*
