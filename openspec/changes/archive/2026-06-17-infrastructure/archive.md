# Archive Report: Infrastructure v0.4.0

**Archived**: 2026-06-17
**Status**: Complete — all 24 tasks implemented, verified (273 tests passing), committed, and pushed

## Executive Summary

Nebula Drift v0.4.0 — Infrastructure milestone. Added SettingsScreen (volume sliders, language toggle, version display), LeaderboardScreen (top 10 scores with gold/silver/bronze), LeaderboardManager (JSON persistence), UiComponents utility, I18nManager runtime locale switching, Android launcher fixes (sensorLandscape, keepScreenOn, fullscreen), and version system (GAME_VERSION constant, CHANGELOG.md). 16 new unit tests, 273 total, all passing.

## Key Decisions

- **I18nManager**: Added `setLocale()` to existing class rather than converting to singleton — preserves existing DI pattern
- **Leaderboard storage**: libGDX Json serializer (already in classpath) rather than SQLite — max 10 entries don't warrant ORM
- **Slider units**: World units (4.0 x 0.2) matching FitViewport(16,9) pattern rather than pixels
- **UiComponents**: Object with static helpers rather than base class — composition over inheritance
- **Language switch**: setLocale + transition to MenuScreen rather than in-place rebuild — existing transition system handles screen recreation cleanly
- **Name entry**: 6 predefined buttons (Pilot, Ace, Nova, Stryker, Vega, Orion) rather than text input — matches out-of-scope keyboard constraint
- **LeaderboardEntry fields**: Changed `val` to `var` with defaults for libGDX Json deserialization compatibility
- **Two-PR rollout**: PR1 (core infra: managers, utils, constants, tests) → PR2 (screens + wiring)

## Files Created

- `core/src/main/kotlin/com/nebuladrift/screens/SettingsScreen.kt`
- `core/src/main/kotlin/com/nebuladrift/screens/LeaderboardScreen.kt`
- `core/src/main/kotlin/com/nebuladrift/managers/LeaderboardManager.kt`
- `core/src/main/kotlin/com/nebuladrift/rendering/UiComponents.kt`
- `core/src/main/kotlin/com/nebuladrift/util/LeaderboardEntry.kt`
- `core/src/test/kotlin/com/nebuladrift/managers/LeaderboardManagerTest.kt` (9 tests)
- `core/src/test/kotlin/com/nebuladrift/managers/I18nManagerTest.kt` (4 tests)
- `core/src/test/kotlin/com/nebuladrift/util/VersionTest.kt` (3 tests)
- `CHANGELOG.md`

## Files Modified

- `core/src/main/kotlin/com/nebuladrift/util/Constants.kt` — GAME_VERSION, LEADERBOARD_MAX_ENTRIES, PREDEFINED_NAMES
- `core/src/main/kotlin/com/nebuladrift/managers/I18nManager.kt` — setLocale(), getLocale()
- `core/src/main/kotlin/com/nebuladrift/screens/MenuScreen.kt` — Settings + Leaderboard buttons, UiComponents refactor
- `core/src/main/kotlin/com/nebuladrift/screens/GameOverScreen.kt` — name entry, Leaderboard button, UiComponents refactor
- `core/src/main/kotlin/com/nebuladrift/NebulaDriftGame.kt` — screen registration
- `core/build.gradle.kts` — test resources configuration
- `assets/i18n/messages_eu.properties` — 11 new keys
- `assets/i18n/messages_es.properties` — 11 new keys
- `assets/i18n/messages_en.properties` — 11 new keys
- `android/src/main/AndroidManifest.xml` — sensorLandscape, keepScreenOn, fullscreen theme
- `android/build.gradle.kts` — versionCode=4, versionName=0.4.0

## Tests Added

- **LeaderboardManagerTest** (9 tests): addEntry, sort by score, tie-breaking, max-10 cap, isHighScore (true/false/above lowest/below highest), JSON round-trip, empty, clear
- **I18nManagerTest** (4 tests): setLocale changes bundle, invalid locale fallback, getLocale returns current, get text
- **VersionTest** (3 tests): GAME_VERSION non-empty, semver pattern matches, exact version

**Total**: 273 tests (257 existing + 16 new), all passing with no regressions.

## Gotchas / Learnings

- libGDX Json cannot deserialize Kotlin `val` data classes — needed `var` with defaults
- `HeadlessApplication` must be initialized once; second instance throws
- `Gdx.files.internal()` resolves relative to JVM working directory, not classpath — needed test resources config

## GitHub Repository

https://github.com/ualonso011/nebula-drift

## Commits

1. `feat: settings screen, leaderboard, version system` (17 files, 897 lines)
2. `feat: android launcher config + infrastructure tests` (5 files, 247 lines)

## Engram Artifact IDs

| Artifact | Observation ID |
|----------|---------------|
| proposal | #147 |
| spec | #149 |
| design | #148 |
| tasks | #150 |
| apply-progress | #151 |

**Note**: No formal verify-report artifact was persisted. Verification was performed inline as part of apply-progress (Phase 6 — Testing). All 273 tests pass, 2 commits pushed to GitHub, no CRITICAL issues.

## Main Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| audio | Updated | REQ-AUDIO-004: added real-time slider integration (+1 scenario) |
| screen-transitions | Updated | REQ-TRANS-002: added 6 new routes (+4 scenarios) |
| game | Updated | REQ-INIT-011/012/014: Settings/Leaderboard buttons, name entry, setLocale (+5 scenarios) |
| settings-screen | Created | 5 requirements, 9 scenarios |
| leaderboard-screen | Created | 5 requirements, 11 scenarios |
| android-launcher | Created | 3 requirements, 4 scenarios |
| version-system | Created | 2 requirements, 2 scenarios |

## What's Next (v0.5.0)

- Glow/bloom shaders for visual polish
- Real art assets (ship sprites, enemy designs, backgrounds)
- Real audio assets (BGM tracks, SFX replacements)
- Async resource loading with loading screen
- Achievement system
- Online leaderboard (optional)
