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

Asteroids MUST spawn at a constant interval from screen edges. Movement MUST be linear (constant velocity). Spawn position and direction MUST vary.

- GIVEN game is active → WHEN spawn timer elapses → THEN new asteroid appears at screen edge with random trajectory.
- GIVEN asteroid in play → WHEN 2 seconds elapse → THEN asteroid has moved in a straight line.

---

## 5. Collision & Combat

### REQ-INIT-007: Laser Shooting — P0

Tap-right (or Space) MUST fire a laser from the ship. A cooldown MUST prevent rapid fire. Lasers MUST be removed after exceeding max lifetime or leaving the screen.

- GIVEN cooldown elapsed → WHEN right-tap / Space → THEN laser spawns at ship position.
- GIVEN laser fired → WHEN cooldown not elapsed → WHEN tap-right → THEN no laser fires.
- GIVEN laser in flight → WHEN lifetime exceeded → THEN laser is removed.

### REQ-INIT-008: Collision Detection — P0

The system MUST detect AABB overlaps for: laser↔asteroid and ship↔asteroid. On laser↔asteroid: asteroid loses 1 HP, laser is consumed. On ship↔asteroid: ship loses 1 life, invulnerability starts.

- GIVEN laser overlaps asteroid → WHEN collision check runs → THEN asteroid HP−1, laser removed.
- GIVEN ship overlaps asteroid (no invulnerability) → WHEN collision check runs → THEN ship loses 1 life.

---

## 6. Scoring & Timer

### REQ-INIT-009: Score System — P0

Points MUST be awarded on asteroid destruction: small=100, medium=200, large=300. Time-survived bonus MUST accrue (1 point/second). Score MUST display in HUD.

- GIVEN small asteroid destroyed → WHEN HP reaches 0 → THEN score += 100.
- GIVEN 10 seconds of survival → WHEN check score → THEN time-bonus contributed ≥ 10 points.

### REQ-INIT-010: Survival Timer — P1

Elapsed time since game start MUST be tracked and displayed in HUD as `M:SS`.

- GIVEN game started → WHEN 65 seconds elapsed → THEN timer displays "1:05".

---

## 7. UI / Screens

### REQ-INIT-011: Main Menu Screen — P0

Main menu MUST display game title, "Play" button, and current high score. Tapping "Play" MUST transition to GameScreen.

- GIVEN main menu visible → WHEN "Play" tapped → THEN GameScreen activates with fresh state.

### REQ-INIT-012: Game Over Screen — P0

Game over MUST display: final score, time survived, "Retry" button, "Main Menu" button. "Retry" MUST restart with reset state. "Main Menu" MUST return to menu.

- GIVEN game over → WHEN "Retry" tapped → THEN new game starts (lives=3, score=0).
- GIVEN game over → WHEN "Main Menu" tapped → THEN main menu displays.

### REQ-INIT-013: HUD Overlay — P0

During gameplay, HUD MUST show: lives (heart icons), score (numeric), timer (M:SS). HUD MUST be rendered above game entities.

- GIVEN 2 lives, score 150, time 30s → WHEN rendering → THEN HUD shows "♥♥", "150", "0:30".

---

## 8. Internationalization

### REQ-INIT-014: i18n String Foundation — P1

The system MUST load UI strings from locale bundles. Supported locales: `eu` (Euskera, default), `es` (Spanish), `en` (English). Locale directory skeleton MUST exist even if translations are incomplete.

- GIVEN no preference set → WHEN strings loaded → THEN Euskera strings used.
- GIVEN locale=es → WHEN strings loaded → THEN Spanish strings used.
- GIVEN a missing key in locale → WHEN string requested → THEN Euskera fallback is used.

---

## 9. Desktop Development

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
| Asteroid System | 2 | 2 | 0 | 4 |
| Collision & Combat | 2 | 2 | 0 | 5 |
| Scoring & Timer | 2 | 1 | 1 | 3 |
| UI / Screens | 3 | 3 | 0 | 4 |
| Internationalization | 1 | 0 | 1 | 3 |
| Desktop Launcher | 1 | 1 | 0 | 2 |
| **Total** | **15** | **13** | **2** | **31** |
