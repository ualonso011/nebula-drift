# Tasks: Init Project — Nebula Drift v0.1

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1400 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 → PR 4 |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | PR | Notes |
|------|------|----|-------|
| 1 | Build + empty game loop | PR 1 | Gradle, screens, launcher. Runs window. |
| 2 | Ship + asteroids + combat | PR 2 | Entities, physics, collision, spawning |
| 3 | Score + HUD + menus | PR 3 | Screens, scoring, i18n, placeholders |
| 4 | Unit tests | PR 4 | JUnit 5 for collision/physics/score/spawn |

## Phase 1: Foundation

- [x] 1.1 Gradle: `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, module builds (`core`, `desktop`, `android`)
- [x] 1.2 `util/Constants.kt` — gravity, thrust, spawn rate, laser lifetime, world units
- [x] 1.3 `rendering/CameraSetup.kt` — OrthographicCamera + FitViewport (16:9)
- [x] 1.4 `NebulaDriftGame.kt` — KtxGame subclass, registers MenuScreen as initial
- [x] 1.5 `screens/GameScreen.kt` — render loop skeleton with clearScreen + delta
- [x] 1.6 `desktop/.../DesktopLauncher.kt` — Lwjgl3Application, 1280×720, 60 FPS

## Phase 2: Core Gameplay

- [x] 2.1 `entities/Ship.kt`, `Asteroid.kt`, `Laser.kt` — data classes: pos, vel, radius, health, lives
- [x] 2.2 `input/GameInputProcessor.kt` — InputMultiplexer: left-half=thrust, right-half=shoot, keyboard (A/Space)
- [x] 2.3 `systems/PhysicsSystem.kt` — gravity, velocity damping, screen-bounds clamp
- [x] 2.4 `systems/SpawnSystem.kt` — timer-based asteroid spawning at edges, random trajectory
- [x] 2.5 `systems/CollisionSystem.kt` — circle-circle: laser↔asteroid (HP-1), ship↔asteroid (life lost + invuln)

## Phase 3: UI & Assets

- [x] 3.1 `systems/ScoreSystem.kt` — time score, destruction points (100/200/300), lives tracking
- [x] 3.2 HUD in GameScreen — lives (hearts), score (numeric), timer (M:SS) via BitmapFont
- [x] 3.3 `screens/MenuScreen.kt` — title + Play button, transitions to GameScreen
- [x] 3.4 `screens/GameOverScreen.kt` — final score + Retry / Main Menu buttons
- [x] 3.5 Screen transitions: Menu → Game → GameOver → Menu with full state reset
- [x] 3.6 i18n: `messages_eu.properties` (default), `messages_es.properties`, `messages_en.properties`
- [x] 3.7 Placeholder textures: ship (64×64 triangle), asteroid circles (128/96/64), laser (32×8 rect)

## Phase 4: Testing

- [x] 4.1 `CollisionTest.kt` — circle overlap true/false, tangent, zero distance, no false negative
- [x] 4.2 `PhysicsTest.kt` — gravity pulls down, thrust counters, bounds clamp
- [x] 4.3 `ScoreTest.kt` — destruction points per tier, time accumulator, zero initial
- [x] 4.4 `SpawnTest.kt` — spawn interval, size distribution, entity cap
