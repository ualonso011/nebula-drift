# Tasks: Infrastructure v0.4.0

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~705 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Foundation) → PR 2 (Screens) |
| Delivery strategy | ask-always |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Foundation + Managers + Android + Tests | PR 1 | Base: main. Constants, UiComponents, LeaderboardManager, I18nManager.setLocale, i18n keys, Android build/manifest, CHANGELOG, all tests. ~310 lines. |
| 2 | Screens + Wiring | PR 2 | Base: main (stacked after PR1). SettingsScreen, LeaderboardScreen, MenuScreen/GameOverScreen updates, NebulaDriftGame registration, name entry flow. ~395 lines. |

## Phase 1: Foundation

- [x] 1.1 Add `GAME_VERSION`, `LEADERBOARD_MAX_ENTRIES`, `PREDEFINED_NAMES` to `util/Constants.kt`
- [x] 1.2 Create `CHANGELOG.md` (root) with v0.1.0–v0.4.0 per Keep a Changelog
- [x] 1.3 Create `rendering/UiComponents.kt` — `object` with `drawButton()`/`drawSlider()`
- [x] 1.4 Add `setLocale(locale: String)` + `getLocale(): String` to `managers/I18nManager.kt`
- [x] 1.5 Add 9 new i18n keys to `assets/i18n/messages_*.properties`

## Phase 2: Leaderboard

- [x] 2.1 Create `LeaderboardEntry` data class (name, score, time, date)
- [x] 2.2 Create `managers/LeaderboardManager.kt` — JSON prefs, add/get/isHighScore, max 10
- [x] 2.3 Create `screens/LeaderboardScreen.kt` — top 10 list, gold/silver/bronze, back button
- [x] 2.4 Add predefined name buttons (6) to `screens/GameOverScreen.kt` when isHighScore

## Phase 3: Settings

- [x] 3.1 Create `screens/SettingsScreen.kt` — music/SFX sliders, language toggle, version, back
- [x] 3.2 Add slider touch drag: world-coords, clamp 0.0–1.0, update AudioManager real-time
- [x] 3.3 Language toggle: cycle eu→es→en→eu, setLocale(), transition to MenuScreen

## Phase 4: Integration

- [x] 4.1 Add Settings + Leaderboard buttons to `screens/MenuScreen.kt`; refactor to UiComponents
- [x] 4.2 Add Leaderboard button + name entry trigger to `screens/GameOverScreen.kt`
- [x] 4.3 Register SettingsScreen + LeaderboardScreen in `NebulaDriftGame.kt`
- [x] 4.4 Wire all fade transitions for new routes in screen transition system

## Phase 5: Android

- [x] 5.1 Fix `AndroidManifest.xml` — sensorLandscape, keepScreenOn, fullscreen theme
- [x] 5.2 Update `android/build.gradle.kts` — versionCode=4, versionName="0.4.0"

## Phase 6: Testing

- [x] 6.1 Create `LeaderboardManagerTest` — insert, sort, max-10, JSON round-trip, isHighScore
- [x] 6.2 Create `I18nManagerTest` — setLocale changes bundle, getLocale returns current, fallback
- [x] 6.3 Create `VersionTest` — GAME_VERSION non-empty, matches semver pattern
- [x] 6.4 Verify all 257 existing tests pass unchanged (regression guard)
