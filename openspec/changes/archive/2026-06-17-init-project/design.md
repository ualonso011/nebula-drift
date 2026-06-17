# Design: Init Project — Nebula Drift v0.1 Foundation

## Technical Approach

Greenfield libGDX + KTX project implementing a playable vertical slice. Architecture follows hybrid OOP: entities as data classes with behavior methods, systems as stateless processors, screens as lifecycle managers via KtxGame/KtxScreen. Desktop-first development with LWJGL3 launcher. All game logic lives in `core` module for future Android reuse.

## Architecture Decisions

### Decision: Screen Management

| Option | Tradeoff | Decision |
|--------|----------|----------|
| KtxGame + KtxScreen | Type-safe switching, automatic disposal, built-in collection | **Chosen** — idiomatic KTX, minimal boilerplate |
| Manual ApplicationListener | Full control | Rejected — reinvents screen lifecycle |
| Scene2D Stage per screen | UI widgets built-in | Rejected — overkill for v0.1, adds coupling |

### Decision: Entity Modeling

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Data classes with methods | Simple, testable, no framework dependency | **Chosen** — fits v0.1 scope, easy to migrate to ECS later |
| Artemis-ODB ECS | Scalable, component-system separation | Rejected — premature for <100 entities |
| Pure OOP inheritance | Familiar | Rejected — rigid hierarchy, harder to compose |

### Decision: Collision Detection

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Circle-circle (radius overlap) | Fast, simple, good enough for circular asteroids/ship | **Chosen** — matches visual shape, O(1) per pair |
| AABB | Slightly faster | Rejected — poor fit for rotated/irregular shapes |
| Box2D | Full physics simulation | Rejected — overkill for v0.1, adds dependency weight |

### Decision: Input Handling

| Option | Tradeoff | Decision |
|--------|----------|----------|
| InputMultiplexer (screen regions) | Clean separation: game input vs UI buttons | **Chosen** — composable, testable |
| Single InputProcessor with conditionals | Simpler | Rejected — mixes concerns, harder to test |

## Data Flow

```
DesktopLauncher
    └── Lwjgl3Application
        └── NebulaDriftGame (KtxGame)
            ├── MenuScreen ──[Play]──→ GameScreen ──[0 lives]──→ GameOverScreen
            │                              │                            │
            │                              ├── PhysicsSystem            │
            │                              ├── CollisionSystem          │
            │                              ├── SpawnSystem              │
            │                              ├── ScoreSystem              │
            │                              └── InputProcessor           │
            │                                                           │
            └───────────────────[Restart]───────────────────────────────┘
```

**Game loop per frame (GameScreen.render):**
1. `clearScreen()` — black background
2. `inputProcessor.process()` — queue thrust/shoot actions
3. `physicsSystem.update(entities, delta)` — apply gravity, velocity, bounds
4. `spawnSystem.update(delta)` — spawn asteroids on timer
5. `collisionSystem.check(ship, asteroids, lasers)` — detect overlaps, dispatch events
6. `scoreSystem.update(delta, events)` — increment time score, process destruction points
7. `batch.begin()` → draw background, entities, HUD → `batch.end()`

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `build.gradle.kts` (root) | Create | Kotlin DSL, libGDX 1.12.x + KTX 1.12.x plugins, allprojects config |
| `settings.gradle.kts` | Create | Include `:core`, `:desktop`, `:android` modules |
| `gradle/libs.versions.toml` | Create | Version catalog: libGDX, KTX, JUnit 5, MockK, Kotlin |
| `core/build.gradle.kts` | Create | Kotlin JVM, libGDX + KTX deps, JUnit 5 + MockK test deps |
| `desktop/build.gradle.kts` | Create | LWJGL3 dep, `application` plugin, `:desktop:run` task |
| `android/build.gradle.kts` | Create | Android plugin shell (inactive v0.1) |
| `core/.../NebulaDriftGame.kt` | Create | KtxGame subclass, registers screens, sets MenuScreen |
| `core/.../screens/MenuScreen.kt` | Create | Title + "Play" button (Scene2D), transitions to GameScreen |
| `core/.../screens/GameScreen.kt` | Create | Main game loop: owns entities, systems, batch, camera |
| `core/.../screens/GameOverScreen.kt` | Create | Score display + "Restart" button, transitions to MenuScreen |
| `core/.../entities/Ship.kt` | Create | Data class: position, velocity, lives, damageState, hitbox radius |
| `core/.../entities/Asteroid.kt` | Create | Data class: position, velocity, size (S/M/L), health, radius |
| `core/.../entities/Laser.kt` | Create | Data class: position, velocity, lifetime, radius |
| `core/.../systems/PhysicsSystem.kt` | Create | Applies gravity (constant downward accel), velocity integration, screen-wrap/bounce |
| `core/.../systems/CollisionSystem.kt` | Create | Circle-circle overlap check, returns collision events |
| `core/.../systems/SpawnSystem.kt` | Create | Timer-based asteroid spawning, difficulty scaling (rate + size mix) |
| `core/.../systems/ScoreSystem.kt` | Create | Time accumulator, destruction points, lives tracking |
| `core/.../input/GameInputProcessor.kt` | Create | InputMultiplexer: left-half = thrust, right-half = shoot, Scene2D buttons |
| `core/.../rendering/CameraSetup.kt` | Create | OrthographicCamera + FitViewport (16:9), world units config |
| `core/.../util/Constants.kt` | Create | Gravity, ship speed, laser lifetime, spawn intervals, colors |
| `desktop/.../DesktopLauncher.kt` | Create | Lwjgl3Application config: 1280x720, title, FPS cap |
| `assets/i18n/messages_eu.properties` | Create | Euskera strings (default locale) |
| `assets/i18n/messages_es.properties` | Create | Spanish strings |
| `assets/i18n/messages_en.properties` | Create | English strings |
| `assets/fonts/default.fnt` + `.png` | Create | Default BitmapFont for HUD/menus |
| `assets/textures/placeholder-ship.png` | Create | Simple triangle sprite (32x32) |
| `assets/textures/placeholder-asteroid-*.png` | Create | Circle sprites: S (16x16), M (32x32), L (48x48) |
| `assets/textures/placeholder-laser.png` | Create | Small rectangle sprite (4x12) |
| `core/src/test/.../` | Create | JUnit 5 tests for collision, score, spawn, physics |

## Interfaces / Contracts

```kotlin
// Entity base contract
interface Entity {
    val position: Vector2
    val velocity: Vector2
    val radius: Float  // collision circle
    fun update(delta: Float)
}

// System contract — stateless processors
interface GameSystem {
    fun update(delta: Float, context: GameContext)
}

data class GameContext(
    val ship: Ship,
    val asteroids: MutableList<Asteroid>,
    val lasers: MutableList<Laser>,
    val events: MutableList<GameEvent>,
    val score: ScoreState
)

sealed class GameEvent {
    data class AsteroidDestroyed(val asteroid: Asteroid, val points: Int) : GameEvent()
    data class ShipHit(val remainingLives: Int) : GameEvent()
    data class LaserAsteroidHit(val laser: Laser, val asteroid: Asteroid) : GameEvent()
}

// Screen transition contract
// MenuScreen → GameScreen: game.setScreen<GameScreen>()
// GameScreen → GameOverScreen: game.setScreen<GameOverScreen>() (carries final score via companion object or singleton)
// GameOverScreen → MenuScreen: game.setScreen<MenuScreen>()
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Collision circle overlap math | JUnit 5 — pure functions, no libGDX deps |
| Unit | Score calculation (time + destruction) | JUnit 5 — pure logic |
| Unit | Spawn system timing and difficulty curve | JUnit 5 + MockK — mock timer |
| Unit | Ship physics (gravity, thrust, bounds) | JUnit 5 — Vector2 math |
| Integration | Screen transitions (Menu → Game → GameOver) | HeadlessApplication — verify setScreen calls |
| Integration | Asset loading pipeline | HeadlessApplication — verify no crashes on load |
| NOT tested | Rendering output, input timing, shader effects | Manual testing via `:desktop:run` |

## Migration / Rollout

No migration required — greenfield project. Rollback = delete generated files.

## Open Questions

- [ ] Should `GameOverScreen` receive score via constructor parameter or a shared `GameState` singleton? Leaning singleton for simplicity in v0.1.
- [ ] World units: 1 unit = 1 pixel, or abstract units (e.g., 1920x1080 world)? Leaning abstract units for resolution independence.
