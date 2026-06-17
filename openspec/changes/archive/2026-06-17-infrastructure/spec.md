# Infrastructure v0.4.0 — Specifications

---

## settings-screen Specification (NEW)

### Purpose

Settings screen provides volume controls, language toggle, and version display. All UI uses custom rendering (ShapeRenderer + SpriteBatch + BitmapFont).

### Requirements

#### REQ-INFRA-001: Settings Screen Layout — P0

The system MUST display a SettingsScreen with: music volume slider, SFX volume slider, language toggle (3 options), version label, and back button. All elements MUST be vertically centered and use localized labels.

##### Scenario: Settings screen renders all elements

- GIVEN SettingsScreen shown
- WHEN screen renders
- THEN music slider, SFX slider, language toggle, version text ("v0.4.0"), and back button are visible

##### Scenario: Localized labels

- GIVEN locale = "eu"
- WHEN SettingsScreen renders
- THEN labels display in Euskera ("Musika", "Soinuak", "Hizkuntza", "Atzera")

---

#### REQ-INFRA-002: Volume Sliders — P0

Each slider MUST render a horizontal bar with draggable thumb. Dragging MUST update value (0.0–1.0). Value changes MUST call AudioManager.setMusicVolume/setSfxVolume immediately. Values MUST persist in Preferences.

##### Scenario: Drag slider to change volume

- GIVEN music slider at 0.5
- WHEN user drags thumb to 0.8 position
- THEN AudioManager.setMusicVolume(0.8) called immediately
- AND preference "music_volume" saved as 0.8

##### Scenario: Slider bounds clamped

- GIVEN slider at 0.0
- WHEN user drags left of bar start
- THEN value remains 0.0

---

#### REQ-INFRA-003: Language Toggle — P0

Language toggle MUST cycle through eu → es → en → eu on tap. On change, the system MUST call I18nManager.setLocale(newLocale) then transition to MenuScreen (recreated with new locale).

##### Scenario: Cycle language

- GIVEN current locale = "eu"
- WHEN language toggle tapped
- THEN locale changes to "es", I18nManager.setLocale("es") called
- AND transition to MenuScreen begins

##### Scenario: Full cycle

- GIVEN locale = "en"
- WHEN language toggle tapped
- THEN locale changes to "eu"

---

#### REQ-INFRA-004: Settings Access — P0

MenuScreen MUST display a "Settings" button (localized). Tapping MUST transition to SettingsScreen via fade transition.

##### Scenario: Navigate to settings

- GIVEN MenuScreen active
- WHEN "Settings" button tapped
- THEN fade transition to SettingsScreen

---

#### REQ-INFRA-005: UiComponents Utility — P1

UiComponents MUST provide reusable static methods: drawButton(renderer, batch, font, bounds, label) and drawSlider(renderer, batch, font, bounds, value). Used by SettingsScreen, LeaderboardScreen, and existing screens.

##### Scenario: drawButton renders rect + border + text

- GIVEN bounds rectangle and label "Play"
- WHEN drawButton called
- THEN filled rect, 2px border, centered text rendered

##### Scenario: drawSlider renders bar + thumb

- GIVEN bounds and value 0.6
- WHEN drawSlider called
- THEN background bar, filled portion (60%), and thumb circle rendered

---

## leaderboard-screen Specification (NEW)

### Purpose

Leaderboard screen displays top 10 local scores with predefined name selection. Data persisted as JSON in Preferences.

### Requirements

#### REQ-INFRA-006: Leaderboard Data Model — P0

LeaderboardEntry MUST contain: name (String), score (Int), time (Float), date (String). Entries MUST be sorted by score descending, then time ascending for ties. Maximum 10 entries stored.

##### Scenario: Sort by score descending

- GIVEN entries: [{score: 500}, {score: 1000}, {score: 300}]
- WHEN sorted
- THEN order: [1000, 500, 300]

##### Scenario: Tie-break by time ascending

- GIVEN entries: [{score: 500, time: 120.5}, {score: 500, time: 90.2}]
- WHEN sorted
- THEN order: [time=90.2, time=120.5]

---

#### REQ-INFRA-007: Leaderboard Persistence — P0

Leaderboard MUST serialize List<LeaderboardEntry> to JSON and store in Preferences key "leaderboard". Load MUST deserialize JSON back to List<LeaderboardEntry>. Invalid/missing data MUST return empty list.

##### Scenario: Save and load round-trip

- GIVEN 3 entries saved
- WHEN leaderboard loaded
- THEN same 3 entries returned with correct fields

##### Scenario: Missing data returns empty

- GIVEN no "leaderboard" preference key
- WHEN load called
- THEN empty list returned

---

#### REQ-INFRA-008: Leaderboard Screen Layout — P0

LeaderboardScreen MUST display: localized title, up to 10 entries (position, name, score, time in M:SS), top 3 highlighted (gold #1, silver #2, bronze #3), and back button.

##### Scenario: Display top 3 with colors

- GIVEN 5 entries in leaderboard
- WHEN rendered
- THEN position 1 = gold highlight, 2 = silver, 3 = bronze, 4-5 = default

##### Scenario: Empty leaderboard

- GIVEN no saved entries
- WHEN LeaderboardScreen shown
- THEN title displayed, no entries shown, back button visible

---

#### REQ-INFRA-009: Name Entry Flow — P0

When a new high score qualifies for top 10, GameOverScreen MUST show 6 predefined names (Pilot, Ace, Nova, Stryker, Vega, Orion). Player taps one name. Score MUST be saved with selected name. If score doesn't qualify, skip name entry.

##### Scenario: New record triggers name selection

- GIVEN player score qualifies for top 10
- WHEN GameOverScreen shown
- THEN 6 name buttons displayed

##### Scenario: Name selected saves entry

- GIVEN name selection shown
- WHEN "Nova" tapped
- THEN LeaderboardEntry created with name="Nova", score=current score
- AND entry inserted into leaderboard (sorted, max 10)

##### Scenario: Non-qualifying score skips

- GIVEN player score does NOT qualify for top 10
- WHEN GameOverScreen shown
- THEN no name selection displayed

---

#### REQ-INFRA-010: Leaderboard Access — P0

MenuScreen MUST display "Leaderboard" button (localized). GameOverScreen MUST display "Leaderboard" button when new record achieved. Both MUST transition via fade.

##### Scenario: Menu to leaderboard

- GIVEN MenuScreen active
- WHEN "Leaderboard" tapped
- THEN fade transition to LeaderboardScreen

##### Scenario: Game over to leaderboard

- GIVEN GameOverScreen with new record
- WHEN "Leaderboard" tapped
- THEN fade transition to LeaderboardScreen

---

## android-launcher Specification (NEW)

### Purpose

Android launcher configuration for correct mobile deployment: landscape orientation, wake lock, version alignment.

### Requirements

#### REQ-INFRA-011: AndroidManifest Configuration — P0

AndroidManifest.xml MUST set screenOrientation="sensorLandscape", keepScreenOn="true". Activity MUST extend AndroidApplication.

##### Scenario: Manifest orientation

- GIVEN AndroidManifest.xml
- WHEN inspected
- THEN screenOrientation = "sensorLandscape"

##### Scenario: Screen stays on

- GIVEN game running on Android
- WHEN no touch input for 5 minutes
- THEN screen does NOT turn off

---

#### REQ-INFRA-012: Android Build Versions — P0

build.gradle.kts MUST set: versionName = GAME_VERSION from Constants, versionCode = 4, minSdk = 21, targetSdk = 34.

##### Scenario: Version alignment

- GIVEN Constants.GAME_VERSION = "0.4.0"
- WHEN Android build configured
- THEN versionName = "0.4.0", versionCode = 4

---

#### REQ-INFRA-013: Android Lifecycle — P0

AndroidLauncher MUST handle pause/resume via AudioManager. Touch input MUST route through GameInputProcessor. No code changes to AndroidLauncher.kt beyond manifest/build config.

##### Scenario: Audio pauses on app background

- GIVEN game playing on Android
- WHEN home button pressed
- THEN AudioManager.pause() called, BGM stops

---

## version-system Specification (NEW)

### Purpose

Single source of truth for game version, used across desktop and Android builds.

### Requirements

#### REQ-INFRA-014: GAME_VERSION Constant — P0

Constants.kt MUST define `const val GAME_VERSION = "0.4.0"`. Used by SettingsScreen display and Android versionName.

##### Scenario: Version constant accessible

- GIVEN Constants object
- WHEN GAME_VERSION accessed
- THEN returns "0.4.0"

---

#### REQ-INFRA-015: CHANGELOG.md — P1

CHANGELOG.md MUST exist in project root. MUST document versions v0.1.0 through v0.4.0 with date and Added sections per Keep a Changelog format.

##### Scenario: Changelog contains all versions

- GIVEN CHANGELOG.md
- WHEN read
- THEN sections for [0.4.0], [0.3.0], [0.2.0], [0.1.0] present

---

## Delta: audio

### MODIFIED Requirements

#### REQ-AUDIO-004: Volume Controls — P0

Music volume and SFX volume MUST be independently controllable (0.0 to 1.0). Values MUST persist in `Gdx.app.getPreferences("nebula-drift")`. Volume = 0.0 means muted. SettingsScreen sliders MUST call setMusicVolume/setSfxVolume in real-time on drag (not just on release).

(Previously: Volume controllable and persisted, but no real-time slider integration specified)

##### Scenario: Volume persisted across sessions

- GIVEN music volume set to 0.3
- WHEN app restarted
- THEN music volume loaded as 0.3 from preferences

##### Scenario: Mute via zero volume

- GIVEN SFX volume set to 0.0
- WHEN any SFX triggered
- THEN no audible sound

##### Scenario: Real-time slider update

- GIVEN SettingsScreen active, music slider at 0.5
- WHEN user drags slider thumb to 0.8
- THEN AudioManager.setMusicVolume(0.8) called immediately during drag
- AND music volume changes audibly without releasing thumb

---

## Delta: screen-transitions

### MODIFIED Requirements

#### REQ-TRANS-002: Applied Transitions — P0

Fade transition MUST apply to ALL screen switches:

| From | To |
|------|----|
| MenuScreen | GameScreen |
| GameScreen | GameOverScreen |
| GameOverScreen | MenuScreen |
| GameOverScreen | GameScreen (retry) |
| MenuScreen | SettingsScreen |
| SettingsScreen | MenuScreen |
| MenuScreen | LeaderboardScreen |
| LeaderboardScreen | MenuScreen |
| GameOverScreen | LeaderboardScreen |
| LeaderboardScreen | GameOverScreen |

(Previously: Only Menu↔Game, GameOver↔Menu, GameOver↔Game routes)

##### Scenario: Menu to Game transition

- GIVEN MenuScreen active, player taps Play
- WHEN transition starts
- THEN fade-to-black → GameScreen loads → fade-from-black

##### Scenario: Game Over retry transition

- GIVEN GameOverScreen active, player taps Retry
- WHEN transition starts
- THEN fade-to-black → GameScreen loads (fresh state) → fade-from-black

##### Scenario: Game Over to Menu transition

- GIVEN GameOverScreen active, player taps Main Menu
- WHEN transition starts
- THEN fade-to-black → MenuScreen loads → fade-from-black

##### Scenario: Menu to Settings transition

- GIVEN MenuScreen active, player taps Settings
- WHEN transition starts
- THEN fade-to-black → SettingsScreen loads → fade-from-black

##### Scenario: Settings to Menu transition

- GIVEN SettingsScreen active, player taps Back
- WHEN transition starts
- THEN fade-to-black → MenuScreen loads → fade-from-black

##### Scenario: Menu to Leaderboard transition

- GIVEN MenuScreen active, player taps Leaderboard
- WHEN transition starts
- THEN fade-to-black → LeaderboardScreen loads → fade-from-black

##### Scenario: GameOver to Leaderboard transition

- GIVEN GameOverScreen active, player taps Leaderboard
- WHEN transition starts
- THEN fade-to-black → LeaderboardScreen loads → fade-from-black

---

## Delta: game

### MODIFIED Requirements

#### REQ-INIT-011: Main Menu Screen — P0

Main menu MUST display game title, "Play" button, "Settings" button (localized), "Leaderboard" button (localized), and current high score. Tapping "Play" MUST transition to GameScreen. Tapping "Settings" MUST transition to SettingsScreen. Tapping "Leaderboard" MUST transition to LeaderboardScreen.

(Previously: Only "Play" button and high score displayed)

##### Scenario: Play button

- GIVEN main menu visible
- WHEN "Play" tapped
- THEN GameScreen activates with fresh state

##### Scenario: Settings button

- GIVEN main menu visible
- WHEN "Settings" tapped
- THEN fade transition to SettingsScreen

##### Scenario: Leaderboard button

- GIVEN main menu visible
- WHEN "Leaderboard" tapped
- THEN fade transition to LeaderboardScreen

---

#### REQ-INIT-012: Game Over Screen — P0

Game over MUST display: final score, time survived, stats summary (enemies destroyed by type, astronauts rescued/killed), "Retry" button, "Main Menu" button. When new high score qualifies for top 10, MUST also display name selection (6 predefined names: Pilot, Ace, Nova, Stryker, Vega, Orion) and "Leaderboard" button. "Retry" MUST restart with reset state. "Main Menu" MUST return to menu.

(Previously: No name entry or leaderboard access from game over)

##### Scenario: Retry

- GIVEN game over
- WHEN "Retry" tapped
- THEN new game starts (lives=3, score=0, stats reset, difficulty reset)

##### Scenario: Main Menu

- GIVEN game over
- WHEN "Main Menu" tapped
- THEN main menu displays

##### Scenario: Stats summary

- GIVEN game ended with 5 fighters, 2 frigates destroyed, 3 astronauts rescued, 1 killed
- WHEN GameOver screen renders
- THEN stats summary displays: "Fighters: 5, Frigates: 2, Rescued: 3, Killed: 1"

##### Scenario: Game over with new record

- GIVEN player achieved top-10 score
- WHEN GameOverScreen shown
- THEN name selection (6 names) and "Leaderboard" button displayed

##### Scenario: Game over without new record

- GIVEN player score does NOT qualify for top 10
- WHEN GameOverScreen shown
- THEN no name selection, no "Leaderboard" button

---

#### REQ-INIT-014: i18n String Foundation — P1

The system MUST load UI strings from locale bundles. Supported locales: `eu` (Euskera, default), `es` (Spanish), `en` (English). I18nManager MUST expose `setLocale(locale: String)` that reloads the I18NBundle for the given locale and updates currentLocale. Locale directory skeleton MUST exist even if translations are incomplete. Missing keys MUST fall back to Euskera.

(Previously: Locale loading at startup only, no runtime switching)

##### Scenario: Default locale

- GIVEN no preference set
- WHEN strings loaded
- THEN Euskera strings used

##### Scenario: Spanish locale

- GIVEN locale=es
- WHEN strings loaded
- THEN Spanish strings used

##### Scenario: Missing key fallback

- GIVEN a missing key in locale
- WHEN string requested
- THEN Euskera fallback is used

##### Scenario: Runtime locale switch

- GIVEN locale = "eu"
- WHEN I18nManager.setLocale("es") called
- THEN I18NBundle reloaded with Spanish strings
- AND currentLocale = "es"

---

## Coverage Summary

| Domain | Type | Requirements | Scenarios |
|--------|------|-------------|-----------|
| settings-screen | New | 5 added | 10 |
| leaderboard-screen | New | 5 added | 11 |
| android-launcher | New | 3 added | 4 |
| version-system | New | 2 added | 2 |
| audio | Modified | 1 modified (+1 scenario) | 3 |
| screen-transitions | Modified | 1 modified (+4 scenarios) | 7 |
| game | Modified | 3 modified (+5 scenarios) | 11 |
| **Total** | | **19 reqs (15 new, 3 modified)** | **48 total (10 new)** |
