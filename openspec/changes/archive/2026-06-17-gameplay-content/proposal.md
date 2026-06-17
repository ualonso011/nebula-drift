# Proposal: Gameplay Content (v0.2.0)

## Intent

Transform Nebula Drift from an asteroid dodger into a full space survival game by adding enemies, astronauts, space debris, and progressive difficulty. Each new entity creates distinct gameplay decisions: target priority (enemies vs. asteroids), risk/reward (rescue astronauts vs. accidentally killing them), and health management (debris recovery).

## Scope

### In Scope
- 4 enemy types: LightFighter, MediumFrigate, HeavyDestroyer, DarkClone with unique movement and damage states
- Astronaut entity with FLOATING/RESCUED/DEAD state machine (+500 rescue / -300 kill)
- SpaceDebris entity with glow rendering, +1 life recovery (capped at 3)
- DifficultyManager with time-based linear interpolation and 15s safe zone
- MirrorSystem for Dark Clone (30-frame delay queue)
- EnemySpawnSystem with separate enemy spawning logic
- CollisionSystem: 5 new pairs (laser↔enemy, ship↔enemy, ship↔astronaut, laser↔astronaut, ship↔debris)
- ScoreSystem: enemy destruction points, astronaut events
- GameScreen: new entity lists, rendering integration, difficulty wiring
- i18n bundle updates for new strings
- 4 new test files (EnemyTest, DifficultyTest, MirrorTest, AstronautTest) + 4 existing tests extended

### Out of Scope
- Particle effects (explosions, engine trails)
- Glow shaders / volumetric lighting
- Music / dynamic audio
- Settings screen, leaderboard screen
- Android launcher
- Premium art assets (all ShapeRenderer placeholders)
- Sound effects
- Enemy shooting (only DarkClone mirrors shots; fighters/frigates/destroyers never shoot back)

## Capabilities

### New Capabilities
- `enemy-system`: Enemy abstract class, 4 subtypes, movement patterns, damage states, scoring values, EnemySpawnSystem, MirrorSystem
- `astronaut-system`: Astronaut entity, state machine, spawning limits, rescue/kill detection and scoring
- `space-debris`: Spawn timer, drift movement, ship overlap → life recovery (capped), glow visual via layered circles
- `difficulty-system`: DifficultyManager with time-based curves, config consumption by spawn/physics systems, 15s safe zone, boundary interpolation

### Modified Capabilities
- `game`: CollisionSystem (5 new pair types), ScoreSystem (enemy/astronaut event counting), GameScreen (3 new entity lists + rendering), Constants (150+ new values), SpawnSystem (difficulty-aware params)

## Approach

Extend existing ECS-light patterns. New abstract `Enemy` base class implementing `Entity`, 4 concrete subclasses. `GameContext` gets `enemies`, `astronauts`, `debris` lists. New `EnemySpawnSystem` + `MirrorSystem` inserted between SpawnSystem and CollisionSystem. `DifficultyManager` provides `DifficultyConfig` consumed by spawn/physics systems each frame. Rendering stays ShapeRenderer with damage-based color shifts. CollisionSystem gets 5 new private pair methods. System update order: Physics → Spawn → EnemySpawn → Mirror → Collision → Score.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `entities/Enemy.kt` | New | Abstract base + 4 subclasses |
| `entities/Astronaut.kt` | New | Entity with state machine |
| `entities/SpaceDebris.kt` | New | Entity with glow visual |
| `systems/DifficultyManager.kt` | New | Time curve provider + DifficultyConfig |
| `systems/EnemySpawnSystem.kt` | New | Enemy spawn timer + type selection |
| `systems/MirrorSystem.kt` | New | Queue recording + delayed playback |
| `systems/GameSystem.kt` | Modified | GameContext: 3 new lists |
| `systems/CollisionSystem.kt` | Modified | 5 new collision pair methods |
| `systems/ScoreSystem.kt` | Modified | New event types + counters |
| `systems/SpawnSystem.kt` | Modified | Difficulty-aware asteroid params |
| `screens/GameScreen.kt` | Modified | Entity lists, render calls, wiring |
| `util/Constants.kt` | Modified | ~150 new values across all domains |
| `rendering/HudRenderer.kt` | Modified | Enemy/astro stats display |
| `managers/I18nManager.kt` | Modified | New i18n keys (enemy names, astronaut events) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Dark Clone mirroring logic too complex | Med | Fallback: fixed patrol + periodic aimed shots |
| Difficulty balance feels wrong | Med | All curves tunable in Constants; test at 0s/15s/60s/180s |
| Entity count perf regression | Low | 50+ entities is trivial for libGDX; profile early if concerned |
| Invulnerability ↔ astronaut rescue interaction | Med | Intentional: invulnerable ship does NOT rescue (risky design) |
| 400-line PR budget exceeded (~1000 lines expected) | High | Use stacked PRs: (1) entities+Constants, (2) systems, (3) GameScreen+rendering, (4) tests |

## Rollback Plan

Remove new entity lists from GameContext, delete EnemySpawnSystem + MirrorSystem + DifficultyManager from update order, revert CollisionSystem + ScoreSystem to v0.1.0 signatures, remove Constants additions. ~10 min revert. All v0.1.0 behavior unaffected since additions are purely additive.

## Dependencies

- None. All code within `core` module. No new libraries or external deps.

## Success Criteria

- [ ] All 4 enemy types spawn, move correctly, take damage, and award correct points on destruction
- [ ] Dark Clone mirrors player position with ~0.5s delay and mirrors shot state
- [ ] Astronauts spawn at 10-15s intervals; rescue awards +500, laser hit deducts -300
- [ ] SpaceDebris spawns at 20-30s; collection adds 1 life (never exceeds 3)
- [ ] Difficulty curves produce expected params at 0s/15s/60s/180s boundaries
- [ ] 15s safe zone: zero enemies before 15s elapsed
- [ ] Ship invulnerability prevents astronaut rescue (intentional)
- [ ] All 4 new test files + 4 extended test classes pass
