# Init Project — Specifications (v0.1.0)

> Greenfield: all capabilities are new. Full specs, not deltas.

---

## 1. Project Setup & Build

### REQ-INIT-001: Multi-Module Gradle Structure — P0

The project MUST use multi-module Gradle (`core`, `android`, `desktop`) with Kotlin DSL. Root build MUST declare libGDX 1.12.x and KTX 1.12.x. `core` holds all game logic; `desktop` provides LWJGL3 launcher; `android` is a shell module.

- GIVEN a fresh clone with JDK 17+ → WHEN `./gradlew build` → THEN all modules compile without errors.
- GIVEN the root project → WHEN `./gradlew :core:dependencies` → THEN libGDX and KTX artifacts resolve from Maven.

---

## 2. Core Game Loop

### REQ-INIT-002: KtxGame Screen Lifecycle — P0

The system MUST use `KtxGame` to manage screens. Each screen MUST extend `KtxScreen` and implement `render(delta: Float)`. Delta time MUST be passed to all entity updates.

- GIVEN the desktop launcher is running → WHEN the game is active → THEN `render()` is called at ~60 FPS with correct delta.
- GIVEN a screen transition → WHEN `game.screen = newScreen` → THEN the new screen's `show()` fires and old screen's `hide()` fires.

---

## 3. Ship Mechanics

### REQ-INIT-003: Gravity and Thrust — P0

The ship MUST experience constant downward gravity. Tap-left (or A-key) MUST apply upward impulse. Velocity MUST be damped each frame. Ship MUST be clamped to screen bounds.

- GIVEN ship in play, no input → WHEN 1 second elapses → THEN ship has moved downward (gravity applied).
- GIVEN ship is falling → WHEN left-tap / A-key → THEN ship receives upward acceleration.
- GIVEN ship at bottom edge → WHEN gravity pulls further → THEN ship position is clamped to screen bounds.

### REQ-INIT-004: Ship Lives and Damage States — P0

The ship MUST start with 3 lives. Each collision MUST reduce lives by 1. Visual state MUST reflect lives: 3=full, 2=minor damage, 1=critical, 0=destroyed. A brief invulnerability period MUST follow each hit.

- GIVEN ship has 3 lives → WHEN collision occurs → THEN lives=2, visual updates to minor damage.
- GIVEN ship has 1 life → WHEN collision occurs → THEN Game Over triggers.
- GIVEN ship was just hit → WHEN within invulnerability window → THEN further collisions are ignored.

---

## 4. Asteroid System

### REQ-INIT-005: Asteroid Size Tiers and Degradation — P0

Three sizes MUST exist: large (3 HP), medium (2 HP), small (1 HP). Each laser hit reduces HP by 1. At 0 HP the asteroid is destroyed. Visual MUST degrade per HP lost. Large→medium→small transitions MUST spawn the next smaller size.

- GIVEN large asteroid (3 HP) → WHEN hit by laser → THEN HP=2, visual degrades.
- GIVEN large asteroid (1 HP remaining) → WHEN hit → THEN asteroid destroyed, medium asteroid spawns.
- GIVEN small asteroid (1 HP) → WHEN hit → THEN asteroid removed, points awarded.

### REQ-INIT-006: Asteroid Spawning and Movement — P0

Asteroids MUST spawn at a constant interval from screen edges. Movement MUST be linear (constant velocity). Spawn position and direction MUST vary. Asteroid spawn rate MUST scale with difficulty via DifficultyConfig.

- GIVEN game is active → WHEN spawn timer elapses → THEN new asteroid appears at screen edge with random trajectory.
- GIVEN asteroid in play → WHEN 2 seconds elapse → THEN asteroid has moved in a straight line.
- GIVEN difficulty config at t=15s: interval = 2.0s → WHEN time advances to t=60s, interval = 1.0s → THEN asteroids spawn twice as fast.

---

## 5. Collision & Combat

### REQ-INIT-007: Laser Shooting — P0

Tap-right (or Space) MUST fire a laser from the ship. A cooldown MUST prevent rapid fire. Lasers MUST be removed after exceeding max lifetime or leaving the screen.

- GIVEN cooldown elapsed → WHEN right-tap / Space → THEN laser spawns at ship position.
- GIVEN laser fired → WHEN cooldown not elapsed → WHEN tap-right → THEN no laser fires.
- GIVEN laser in flight → WHEN lifetime exceeded → THEN laser is removed.

### REQ-INIT-008: Collision Detection — P0

The system MUST detect AABB overlaps for: laser↔asteroid, ship↔asteroid, laser↔enemy, ship↔enemy, ship↔astronaut, laser↔astronaut, ship↔debris.

- **laser↔asteroid**: asteroid loses 1 HP, laser consumed.
- **ship↔asteroid** (no invulnerability): ship loses 1 life, invulnerability starts.
- **laser↔enemy**: enemy loses 1 HP, laser consumed.
- **ship↔enemy** (no invulnerability): ship loses 1 life, enemy destroyed, invulnerability starts.
- **ship↔astronaut** (not invulnerable): astronaut RESCUED (+500), wave animation.
- **laser↔astronaut**: astronaut DEAD (-300), sad animation.
- **ship↔debris**: +1 life (capped at 3), debris removed, positive feedback.

(Previously: Only laser↔asteroid and ship↔asteroid pairs detected)

#### Scenario: Laser hits enemy

- GIVEN laser overlaps enemy hitbox
- WHEN collision check runs
- THEN enemy HP -= 1, laser removed

#### Scenario: Ship collides with enemy

- GIVEN ship overlaps enemy (not invulnerable)
- WHEN collision check runs
- THEN ship loses 1 life, enemy destroyed, invulnerability starts

#### Scenario: Ship rescues astronaut

- GIVEN ship overlaps astronaut (not invulnerable)
- WHEN collision check runs
- THEN astronaut state = RESCUED, score += 500

#### Scenario: Laser kills astronaut

- GIVEN laser overlaps astronaut
- WHEN collision check runs
- THEN astronaut state = DEAD, score -= 300

#### Scenario: Ship collects debris

- GIVEN ship overlaps debris
- WHEN collision check runs
- THEN ship lives += 1 (capped at 3), debris removed

---

## 6. Scoring & Timer

### REQ-INIT-009: Score System — P0

Points MUST be awarded on:

- Asteroid destruction: small=100, medium=200, large=300.
- Enemy destruction: LightFighter=+150, MediumFrigate=+250, HeavyDestroyer=+400, DarkClone=+500.
- Astronaut rescue: +500.
- Astronaut kill: -300 (penalty).
- Time-survived bonus: 1 point/second.

Score MUST display in HUD. Stats MUST track: enemies destroyed (by type), astronauts rescued, astronauts killed.

(Previously: Only asteroid destruction and time bonus)

#### Scenario: Enemy destroyed awards points

- GIVEN LightFighter destroyed
- WHEN HP reaches 0
- THEN score += 150, enemiesDestroyed[LightFighter] += 1

#### Scenario: Astronaut rescue awards points

- GIVEN astronaut rescued
- WHEN rescue animation completes
- THEN score += 500, astronautsRescued += 1

#### Scenario: Astronaut kill penalizes

- GIVEN astronaut killed by laser
- WHEN death animation completes
- THEN score -= 300, astronautsKilled += 1

#### Scenario: Stats tracked for GameOver

- GIVEN game in progress
- WHEN game ends
- THEN stats available: enemiesDestroyed (by type), astronautsRescued, astronautsKilled

### REQ-INIT-010: Survival Timer — P1

Elapsed time since game start MUST be tracked and displayed in HUD as `M:SS`.

- GIVEN game started → WHEN 65 seconds elapsed → THEN timer displays "1:05".

---

## 7. UI / Screens

### REQ-INIT-011: Main Menu Screen — P0

Main menu MUST display game title, "Play" button, "Settings" button (localized), "Leaderboard" button (localized), and current high score. Tapping "Play" MUST transition to GameScreen. Tapping "Settings" MUST transition to SettingsScreen. Tapping "Leaderboard" MUST transition to LeaderboardScreen.

#### Scenario: Play button

- GIVEN main menu visible
- WHEN "Play" tapped
- THEN GameScreen activates with fresh state

#### Scenario: Settings button

- GIVEN main menu visible
- WHEN "Settings" tapped
- THEN fade transition to SettingsScreen

#### Scenario: Leaderboard button

- GIVEN main menu visible
- WHEN "Leaderboard" tapped
- THEN fade transition to LeaderboardScreen

### REQ-INIT-012: Game Over Screen — P0

Game over MUST display: final score, time survived, stats summary (enemies destroyed by type, astronauts rescued/killed), "Retry" button, "Main Menu" button. When new high score qualifies for top 10, MUST also display name selection (6 predefined names: Pilot, Ace, Nova, Stryker, Vega, Orion) and "Leaderboard" button. "Retry" MUST restart with reset state. "Main Menu" MUST return to menu.

#### Scenario: Retry

- GIVEN game over
- WHEN "Retry" tapped
- THEN new game starts (lives=3, score=0, stats reset, difficulty reset)

#### Scenario: Main Menu

- GIVEN game over
- WHEN "Main Menu" tapped
- THEN main menu displays

#### Scenario: Stats summary

- GIVEN game ended with 5 fighters, 2 frigates destroyed, 3 astronauts rescued, 1 killed
- WHEN GameOver screen renders
- THEN stats summary displays: "Fighters: 5, Frigates: 2, Rescued: 3, Killed: 1"

#### Scenario: Game over with new record

- GIVEN player achieved top-10 score
- WHEN GameOverScreen shown
- THEN name selection (6 names) and "Leaderboard" button displayed

#### Scenario: Game over without new record

- GIVEN player score does NOT qualify for top 10
- WHEN GameOverScreen shown
- THEN no name selection, no "Leaderboard" button

### REQ-INIT-013: HUD Overlay — P0

During gameplay, HUD MUST show: lives (heart icons), score (numeric), timer (M:SS). HUD MAY display enemy/astronaut stats (optional, P2). HUD MUST be rendered above game entities.

(Previously: Only lives, score, timer)

- GIVEN 2 lives, score 150, time 30s → WHEN rendering → THEN HUD shows "♥♥", "150", "0:30".

---

## 8. GameScreen Systems

### REQ-INIT-016: GameScreen Entity Lists — P0 (Added)

GameScreen MUST maintain 3 new entity lists: `enemies: MutableList<Enemy>`, `astronauts: MutableList<Astronaut>`, `debris: MutableList<SpaceDebris>`. Lists MUST be initialized empty and cleared on game reset.

- GIVEN GameScreen created → WHEN game starts → THEN enemies, astronauts, debris lists are empty.
- GIVEN game over, player taps Retry → WHEN new game starts → THEN all 3 lists cleared.

### REQ-INIT-017: System Update Order — P0 (Added)

GameScreen.update() MUST call systems in this order: Physics → Spawn → EnemySpawn → Mirror → Collision → Score. Each system receives delta time and GameContext.

- GIVEN frame N begins → WHEN update() called → THEN systems execute: Physics, Spawn, EnemySpawn, Mirror, Collision, Score (in that order).

---

## 9. Constants Expansion

### REQ-INIT-018: Constants Expansion — P0 (Added)

Constants MUST define ~150 new values across: enemy HP/speed/score, astronaut spawn interval/score, debris spawn interval/life cap, difficulty curve start/end/duration, collision radii, visual colors.

- GIVEN Constants object → WHEN enemy/astronaut/debris/difficulty values accessed → THEN all values defined and non-null.

---

## 10. Internationalization

### REQ-INIT-014: i18n String Foundation — P1

The system MUST load UI strings from locale bundles. Supported locales: `eu` (Euskera, default), `es` (Spanish), `en` (English). I18nManager MUST expose `setLocale(locale: String)` that reloads the I18NBundle for the given locale and updates currentLocale. Locale directory skeleton MUST exist even if translations are incomplete. Missing keys MUST fall back to Euskera.

#### Scenario: Default locale

- GIVEN no preference set
- WHEN strings loaded
- THEN Euskera strings used

#### Scenario: Spanish locale

- GIVEN locale=es
- WHEN strings loaded
- THEN Spanish strings used

#### Scenario: Missing key fallback

- GIVEN a missing key in locale
- WHEN string requested
- THEN Euskera fallback is used

#### Scenario: Runtime locale switch

- GIVEN locale = "eu"
- WHEN I18nManager.setLocale("es") called
- THEN I18NBundle reloaded with Spanish strings
- AND currentLocale = "es"

---

## 11. Desktop Development

### REQ-INIT-015: Desktop Launcher — P0

The `desktop` module MUST launch via `Lwjgl3Application`. Window MUST be resizable. FPS MUST be capped at 60. Default resolution SHOULD be 800×600.

- GIVEN JDK 17+ installed → WHEN `./gradlew :desktop:run` → THEN game window opens showing main menu.
- GIVEN game running → WHEN window edge dragged → THEN window resizes and game content adapts.

---

## Coverage Summary

| Area | Reqs | P0 | P1 | Scenarios |
|------|------|----|----|-----------|
| Project Setup | 1 | 1 | 0 | 2 |
| Game Loop | 1 | 1 | 0 | 2 |
| Ship Mechanics | 2 | 2 | 0 | 6 |
| Asteroid System | 2 | 2 | 0 | 5 |
| Collision & Combat | 2 | 2 | 0 | 8 |
| Scoring & Timer | 2 | 1 | 1 | 7 |
| UI / Screens | 3 | 3 | 0 | 10 |
| GameScreen Systems | 2 | 2 | 0 | 4 |
| Constants Expansion | 1 | 1 | 0 | 1 |
| Internationalization | 1 | 0 | 1 | 4 |
| Desktop Launcher | 1 | 1 | 0 | 2 |
| **Total** | **18** | **16** | **2** | **51** |
