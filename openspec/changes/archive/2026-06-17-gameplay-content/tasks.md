# Tasks: Gameplay Content (v0.2.0)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~670 (370 new + 300 modified) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Foundation + Entities → PR 2: Systems → PR 3: Integration → PR 4: Tests |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

```
Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High
```

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Constants + Entity classes + Ship.addLife + GameContext updates | PR 1 | Base for everything; merge to main |
| 2 | All new systems (DifficultyManager, spawn, mirror) + enemy rendering | PR 2 | Depends on PR 1; merge to main |
| 3 | CollisionSystem expansion + ScoreSystem + GameScreen wiring + HUD/GameOver | PR 3 | Depends on PR 1-2; merge to main |
| 4 | Full test suite (4 new + 4 extended) + i18n | PR 4 | Depends on PR 3; merge to main |

## Phase 1: Foundation & Entities

- [x] 1.1 Add ~60 Constants to `util/Constants.kt` (enemy stats, spawn rates, difficulty curves, score values, astronaut/debris)
- [x] 1.2 Create `entities/enemies/Enemy.kt` — abstract Enemy base, EnemyType/EnemyDamageState enums, + 4 concrete subclasses (LightFighter, MediumFrigate, HeavyDestroyer, DarkClone) in separate files
- [x] 1.3 Create `entities/Astronaut.kt` — Astronaut entity with FLOATING/RESCUED/DEAD state machine, rescue()/kill() methods, stateTimer, shouldRemove
- [x] 1.4 Create `entities/SpaceDebris.kt` — SpaceDebris entity with glowPhase for pulsing alpha effect
- [x] 1.5 Add `addLife()` method to `entities/Ship.kt` (capped at SHIP_LIVES)
- [x] 1.6 Update `systems/GameSystem.kt` — add enemies/astronauts/debris lists + elapsedTime to GameContext, add 4 new GameEvent subtypes

## Phase 2: New Systems

- [x] 2.1 Create `systems/DifficultyManager.kt` — DifficultyConfig data class, EnemyTypeDistribution, getConfig() with lerp + safe zone
- [x] 2.2 Create `systems/EnemySpawnSystem.kt` — spawn enemies from right edge, difficulty-based type distribution, respect safe zone
- [x] 2.3 Create `systems/MirrorSystem.kt` — 30-frame circular buffer queue, record player state, drive DarkClone Y + firing
- [x] 2.4 Create `systems/AstronautSpawnSystem.kt` — 10-15s interval, max 1 active
- [x] 2.5 Create `systems/DebrisSpawnSystem.kt` — 20-30s interval, max 1 active
- [x] 2.6 Update `systems/PhysicsSystem.kt` — add updateEnemies/updateAstronauts/updateDebris methods, update Ship.addLife removal check
- [x] 2.7 Update `systems/SpawnSystem.kt` — read DifficultyConfig for asteroid spawn rate

## Phase 3: Collision, Score & Integration

- [x] 3.1 Update `systems/CollisionSystem.kt` — add 5 new pair checkers (laser↔enemy, ship↔enemy, ship↔astronaut, laser↔astronaut, ship↔debris)
- [x] 3.2 Update `systems/ScoreSystem.kt` — handle EnemyDestroyed/AstronautRescued/AstronautKilled events, track stats (by type)
- [x] 3.3 Update `screens/GameScreen.kt` — init new entity lists, wire DifficultyManager, update system order (Physics→Spawn→EnemySpawn→AstronautSpawn→DebrisSpawn→Collision→Score), render new entities
- [x] 3.4 Update `rendering/HudRenderer.kt` — show astronauts rescued count (optional)
- [x] 3.5 Update `screens/GameOverScreen.kt` — display enemies destroyed, astronauts rescued/killed, extend GameSession stats

## Phase 4: Testing & i18n

- [x] 4.1 Create `EnemyTest.kt` — HP, damage states, points, movement for all 4 types
- [x] 4.2 Create `DifficultyTest.kt` — curve at 0s/15s/60s/180s, safe zone, interpolation, clamp
- [x] 4.3 Create `MirrorTest.kt` — queue recording, 30-frame delay, shot mirroring, reset
- [x] 4.4 Create `AstronautTest.kt` — state transitions, rescue/kill, shouldRemove timer
- [x] 4.5 Extend `CollisionTest.kt` — 5 new pairs (laser↔enemy, ship↔enemy, ship↔astronaut, laser↔astronaut, ship↔debris)
- [x] 4.6 Extend `PhysicsTest.kt` — enemy/astronaut/debris movement, off-screen removal
- [x] 4.7 Extend `ScoreTest.kt` — new point values, astronaut penalty, event counting
- [x] 4.8 Extend `SpawnTest.kt` — enemy/astronaut/debris spawning, rates, distribution, constraints
- [x] 4.9 i18n keys verified (all 14 keys present in all 4 property files)
