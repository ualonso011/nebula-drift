# Archive Report: Init Project — Nebula Drift v0.1

**Status**: success — fully implemented and verified
**Archived**: 2026-06-17
**Change**: init-project
**GitHub**: https://github.com/ualonso011/nebula-drift

## Executive Summary

Nebula Drift v0.1.0 — A playable Android space survival game (vertical slice) built with libGDX + KTX (Kotlin). Multi-module Gradle project with core game loop (ship with gravity/thrust, asteroid spawning/degradation, laser shooting, collision detection), full screen lifecycle (Menu → Game → Game Over), HUD (lives, score, timer), i18n (Euskera/Spanish/English), placeholder textures, and 49 unit tests.

## What Was Built

- **Project foundation**: Multi-module Gradle (core, android, desktop), Kotlin DSL, version catalog, Gradle wrapper
- **Core game loop**: KtxGame + KtxScreen lifecycle, delta-time entity updates
- **Ship mechanics**: Gravity simulation, thrust input (tap left / A-key), velocity damping, screen-bounds clamping, 3 lives with damage states, invulnerability period
- **Asteroid system**: 3 size tiers (large=3HP, medium=2HP, small=1HP), HP degradation on hit, large→medium→small transitions, timer-based edge spawning, linear movement
- **Combat**: Laser shooting (tap right / Space), cooldown & lifetime, circle-circle collision detection for laser↔asteroid and ship↔asteroid
- **Scoring**: Time-based bonus (1pt/sec) + destruction points (small=100, medium=200, large=300)
- **HUD**: Lives (hearts), score (numeric), timer (M:SS) rendered via BitmapFont
- **Screens**: Main Menu → Game Screen → Game Over with full state reset on Retry
- **i18n**: Euskera (default), Spanish, English locale bundles
- **Placeholder assets**: Ship (64×64 triangle), asteroid sprites (128/96/64 circles), laser (32×8 rect), background
- **Desktop launcher**: LWJGL3, 1280×720, 60 FPS cap
- **Unit tests**: 49 tests across 4 test files — collision (9), physics (14), score (13), spawn (13)

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Screen Management | KtxGame + KtxScreen | Type-safe, built-in lifecycle, minimal boilerplate |
| Entity Modeling | Data classes with methods | Simple, testable, easy to migrate to ECS later |
| Collision Detection | Circle-circle (radius overlap) | Fast, O(1) per pair, good fit for circular entities |
| Input Handling | InputMultiplexer | Composable, separates game input from UI |
| i18n Default | Euskera (eu) | Project locale priority |
| World Units | Abstract units | Resolution-independent rendering |

## Files Created

| Module | Key Files |
|--------|-----------|
| Root | `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `local.properties` |
| Core | `NebulaDriftGame.kt`, `Constants.kt`, `CameraSetup.kt`, `GameInputProcessor.kt`, `HudRenderer.kt`, `I18nManager.kt` |
| Core Entities | `Entity.kt` (interface), `Ship.kt`, `Asteroid.kt`, `Laser.kt` |
| Core Systems | `GameSystem.kt` (interface), `PhysicsSystem.kt`, `SpawnSystem.kt`, `CollisionSystem.kt`, `ScoreSystem.kt` |
| Core Screens | `GameScreen.kt`, `MenuScreen.kt`, `GameOverScreen.kt` |
| Desktop | `DesktopLauncher.kt` |
| Android | `build.gradle.kts`, `AndroidManifest.xml` (shell) |
| Assets | `ship.png`, `asteroid_large.png`, `asteroid_medium.png`, `asteroid_small.png`, `laser.png`, `background.png` |
| i18n | `messages_eu.properties`, `messages_es.properties`, `messages_en.properties`, `messages.properties` |
| Tests | `CollisionTest.kt` (9), `PhysicsTest.kt` (14), `ScoreTest.kt` (13), `SpawnTest.kt` (13) |

## Commits

1. `chore: add .gitignore`
2. `feat: project foundation - Gradle build, libGDX setup, desktop launcher` (18 files, 1239 lines)
3. `feat: core gameplay - entities, input, physics, spawning, collisions` (10 files, 671 lines)
4. `feat: UI, screens, i18n, and placeholder assets` (13 files, 450 lines)
5. `test: unit tests for collision, physics, score, and spawn systems` (4 files, 926 lines)

## Engram Artifact Lineage

| Artifact | Observation ID |
|----------|---------------|
| proposal | #116 |
| design | #117 |
| spec | #118 |
| tasks | #119 (reconciled stale checkboxes) |
| apply-progress | #120 |
| archive-report | (current) |

**Reconciliation note**: The Engram tasks observation (#119) contained stale unchecked checkboxes for Phases 2-4 because sdd-apply did not update the Engram copy (only the openspec filesystem copy was updated). The openspec `tasks.md` had all tasks correctly marked `[x]`. All 15 tasks were verified complete via apply-progress (#120) and user confirmation. Checkboxes in the Engram observation were reconciled during archive.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| game | Created | Full spec copied to main specs (greenfield — no delta to merge) |

## Source of Truth Updated

- `openspec/specs/game/spec.md` — now reflects v0.1.0 system behavior

## What's Next (v0.2.0)

- Enemy ships (fighters, frigate, destroyer, clone)
- Astronaut rescue mechanics
- Particle system (explosions, thrust trails)
- Shaders (nebula background, glow effects)
- Audio (music, SFX, volume controls)
- Android launcher / APK pipeline
- Leaderboard & settings screens
