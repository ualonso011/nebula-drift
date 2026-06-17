# Design: Gameplay Content (v0.2.0)

## Technical Approach

Extend the existing ECS-light architecture by adding new entity types (Enemy hierarchy, Astronaut, SpaceDebris), new systems (EnemySpawnSystem, MirrorSystem, AstronautSpawnSystem, DebrisSpawnSystem), and a difficulty management layer (DifficultyManager). All additions follow established patterns: Entity interface implementation, GameSystem contract, GameContext mutation, ShapeRenderer placeholders, and JUnit 5 testing.

The design maintains backward compatibility—v0.1.0 behavior is preserved since all changes are additive. System update order becomes: Physics → Spawn → EnemySpawn → Mirror → Collision → Score.

## Architecture Decisions

### Decision: Entity Hierarchy with Abstract Enemy Base

**Choice**: Abstract `Enemy` class implementing `Entity`, with 4 concrete subclasses (LightFighter, MediumFrigate, HeavyDestroyer, DarkClone).

**Alternatives considered**:
- Interface-based composition (IEnemy with multiple interfaces)
- Single Enemy class with type enum and conditional logic

**Rationale**: Matches existing pattern where Ship has damage states and Asteroid has size tiers. Abstract base provides shared behavior (health, takeDamage, getDamageState) while subclasses define type-specific constants (maxHealth, points, speed, radius). This is idiomatic Kotlin and aligns with the codebase's preference for class hierarchies over composition.

### Decision: Separate Spawn Systems per Entity Type

**Choice**: Keep SpawnSystem for asteroids, add EnemySpawnSystem, AstronautSpawnSystem, DebrisSpawnSystem as separate systems.

**Alternatives considered**:
- Merge all spawning into one SpawnSystem with entity-type branches
- Generic SpawnSystem<T> with type-specific spawners

**Rationale**: Each entity type has distinct spawn logic (enemies need difficulty-based type distribution, astronauts need max-1-active constraint, debris needs rare intervals). Separate systems follow Single Responsibility and make testing easier. The existing SpawnSystem already works for asteroids—no reason to refactor it.

### Decision: DifficultyManager as Stateless Provider (Not a GameSystem)

**Choice**: DifficultyManager is a plain class that calculates `DifficultyConfig` from elapsed time. GameScreen owns it and passes config to spawn systems each frame.

**Alternatives considered**:
- Make DifficultyManager a GameSystem that mutates context
- Embed difficulty logic directly in each spawn system

**Rationale**: DifficultyManager doesn't mutate entities—it's a pure function of elapsed time. Making it a GameSystem would violate the pattern (systems mutate context). Keeping it stateless and external makes it testable in isolation and avoids coupling spawn systems to a shared mutable state.

### Decision: MirrorSystem as GameSystem with Queue-Based Delay

**Choice**: MirrorSystem is a GameSystem that records player state (position, thrust, shoot) each frame into a circular buffer. DarkClone reads from the buffer with a 30-frame (~0.5s) delay.

**Alternatives considered**:
- DarkClone uses simplified AI (patrol + periodic shots)
- MirrorSystem stores full input events instead of state snapshots

**Rationale**: The spec requires mirroring with ~0.5s delay. A queue-based approach is straightforward and testable. Storing state snapshots (position, isThrusting, isShooting) is simpler than storing input events and achieves the same visual effect. The 30-frame queue is a fixed-size circular buffer to avoid unbounded memory growth.

### Decision: CollisionSystem Expansion with Private Methods per Pair

**Choice**: Add 5 new private methods to CollisionSystem following the existing pattern: `checkLaserEnemyCollisions`, `checkShipEnemyCollisions`, `checkShipAstronautCollisions`, `checkLaserAstronautCollisions`, `checkShipDebrisCollisions`.

**Alternatives considered**:
- Generic collision dispatcher with pair-type registry
- Separate CollisionSystem per entity type

**Rationale**: The existing CollisionSystem already uses private methods per pair (checkLaserAsteroidCollisions, checkShipAsteroidCollisions). Extending this pattern maintains consistency. A generic dispatcher would be over-engineering for 7 total pairs. Separate systems would fragment collision logic and make it harder to reason about interactions.

### Decision: GameContext Expansion with New Entity Lists

**Choice**: Add `enemies: MutableList<Enemy>`, `astronauts: MutableList<Astronaut>`, `debris: MutableList<SpaceDebris>` to GameContext. Add `difficultyManager: DifficultyManager` and `mirrorSystem: MirrorSystem` as references.

**Alternatives considered**:
- Keep new entity lists in GameScreen, pass separately to systems
- Use a generic `entities: MutableList<Entity>` and type-check

**Rationale**: The existing GameContext already holds asteroids and lasers as typed lists. Adding typed lists for new entities maintains type safety and avoids casting. Including difficultyManager and mirrorSystem in context allows systems to access them without tight coupling to GameScreen.

### Decision: Event System Extension with New Event Types

**Choice**: Extend the `GameEvent` sealed class with: `EnemyDestroyed(enemy: Enemy, points: Int)`, `AstronautRescued(astronaut: Astronaut)`, `AstronautKilled(astronaut: Astronaut)`, `DebrisCollected(debris: SpaceDebris)`.

**Alternatives considered**:
- Use a generic `ScoreEvent(points: Int, reason: String)`
- Skip events and let systems directly mutate score

**Rationale**: The existing event system (AsteroidDestroyed, ShipHit, LaserAsteroidHit) provides a clean separation between collision detection and score tracking. Extending it with new event types maintains this pattern and makes ScoreSystem's job trivial (just count events).

### Decision: Dark Clone Implementation with Queue-Based Mirroring

**Choice**: DarkClone reads from MirrorSystem's queue with 30-frame delay. It mirrors player Y-position and fires lasers when the player fired 30 frames ago. DarkClone moves left at a constant speed (like other enemies) but its Y-position is overridden by the mirror system.

**Alternatives considered**:
- Simplified AI: patrol in sine wave, fire aimed shots every 2-3s
- Full input recording with replay

**Rationale**: The spec requires mirroring with ~0.5s delay. Queue-based approach is simple and testable. DarkClone still moves left (so it doesn't linger forever) but its Y-position is controlled by the mirror. When the mirrored state includes `isShooting=true`, DarkClone fires a laser (separate from player lasers, stored in a `darkCloneLasers` list or added to the main lasers list with a flag).

### Decision: Astronaut State Machine with Animation Timers

**Choice**: Astronaut has states: FLOATING, RESCUED, DEAD. On rescue/kill, set state and start a `stateTimer`. When `stateTimer > 0.5f`, remove the astronaut. During RESCUED/DEAD, play a simple animation (vertical oscillation for rescued, fall-down for dead).

**Alternatives considered**:
- Immediate removal on rescue/kill (no animation)
- Complex animation system with keyframes

**Rationale**: The spec requires a brief animation (0.5-1s). A simple timer-based approach is sufficient. The animation is just a visual effect (position offset) during the timer—no need for a full animation system. This matches the codebase's preference for minimal, pragmatic solutions.

### Decision: Space Debris with Pulsing Glow Effect

**Choice**: SpaceDebris has a `glowPhase: Float` that increments each frame. Rendering draws two circles: inner solid gold, outer with alpha = 0.3 + 0.2 * sin(glowPhase). This creates a pulsing glow.

**Alternatives considered**:
- Shader-based glow
- Particle effect

**Rationale**: The spec explicitly states "no glow shaders / volumetric lighting" (out of scope). A sine-wave alpha pulse on a second circle is trivial to implement and achieves a similar visual effect. This matches the codebase's ShapeRenderer-only rendering approach.

### Decision: PhysicsSystem Expansion with Entity-Specific Update Methods

**Choice**: Add `updateEnemies`, `updateAstronauts`, `updateDebris` private methods to PhysicsSystem, following the existing pattern (updateShip, updateLasers, updateAsteroids).

**Alternatives considered**:
- Each entity type calls its own `update(delta)` method
- Generic entity update loop

**Rationale**: The existing PhysicsSystem already has entity-specific update methods. Extending this pattern maintains consistency. Each entity type has slightly different physics (enemies move left at their speed, astronauts drift slowly, debris drifts slowly), so entity-specific logic is appropriate. DarkClone's movement is handled by MirrorSystem, not PhysicsSystem.

### Decision: SpawnSystem Modification for Difficulty-Aware Asteroids

**Choice**: Modify SpawnSystem to accept a `DifficultyConfig` parameter (or read from GameContext) and use `config.asteroidSpawnRate` instead of `Constants.ASTEROID_SPAWN_INTERVAL`.

**Alternatives considered**:
- Keep SpawnSystem unchanged, create a separate DifficultyAwareSpawnSystem
- Pass spawn rate as a constructor parameter

**Rationale**: The existing SpawnSystem uses a constant interval. Making it difficulty-aware is a minimal change (replace constant with config value). This avoids creating a parallel system and keeps asteroid spawning in one place.

## Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ GameScreen                                                       │
│                                                                  │
│  ┌──────────────┐                                               │
│  │ Difficulty   │──────┐                                        │
│  │ Manager      │      │ provides DifficultyConfig               │
│  └──────────────┘      │                                        │
│                        ▼                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ GameContext                                               │  │
│  │  - ship, asteroids, lasers, enemies, astronauts, debris   │  │
│  │  - difficultyManager, mirrorSystem                        │  │
│  │  - events, score                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                        │                                        │
│                        ▼                                        │
│  ┌─────────┐  ┌──────────┐  ┌────────────┐  ┌──────────┐      │
│  │ Physics │→ │  Spawn   │→ │EnemySpawn  │→ │ Mirror   │      │
│  │ System  │  │  System  │  │  System    │  │  System  │      │
│  └─────────┘  └──────────┘  └────────────┘  └──────────┘      │
│       │             │              │               │            │
│       └─────────────┴──────────────┴───────────────┘            │
│                        │                                        │
│                        ▼                                        │
│              ┌──────────────────┐                              │
│              │ CollisionSystem  │                              │
│              └──────────────────┘                              │
│                        │                                        │
│                        ▼                                        │
│              ┌──────────────────┐                              │
│              │  ScoreSystem     │                              │
│              └──────────────────┘                              │
│                        │                                        │
│                        ▼                                        │
│              ┌──────────────────┐                              │
│              │    Rendering     │                              │
│              │  (ShapeRenderer) │                              │
│              └──────────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

**Frame Update Sequence**:
1. **PhysicsSystem**: Update ship (gravity, thrust, damping, bounds), lasers (position, expiry), asteroids (position, off-screen removal), enemies (position, off-screen removal), astronauts (position, state timer), debris (position, glow phase, off-screen removal).
2. **SpawnSystem**: Spawn asteroids based on `config.asteroidSpawnRate`.
3. **EnemySpawnSystem**: Spawn enemies based on `config.enemySpawnRate` and `config.enemyTypeDistribution`. Respect safe zone (no enemies before 15s).
4. **MirrorSystem**: Record player state (position, isThrusting, isShooting) into queue. DarkClone reads from queue with 30-frame delay and updates its position/firing state.
5. **CollisionSystem**: Check all 7 collision pairs (laser↔asteroid, ship↔asteroid, laser↔enemy, ship↔enemy, ship↔astronaut, laser↔astronaut, ship↔debris). Emit events.
6. **ScoreSystem**: Accumulate time bonus, count events (asteroid/enemy destroyed, astronaut rescued/killed), update score.
7. **Rendering**: Draw background, entities (asteroids, lasers, enemies, astronauts, debris, ship), HUD overlay.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `entities/Enemy.kt` | Create | Abstract Enemy base class + LightFighter, MediumFrigate, HeavyDestroyer, DarkClone subclasses |
| `entities/Astronaut.kt` | Create | Astronaut entity with FLOATING/RESCUED/DEAD state machine |
| `entities/SpaceDebris.kt` | Create | SpaceDebris entity with glowPhase for pulsing effect |
| `systems/DifficultyManager.kt` | Create | Calculates DifficultyConfig from elapsed time (linear interpolation, safe zone) |
| `systems/EnemySpawnSystem.kt` | Create | Spawns enemies based on difficulty config, manages enemy type distribution |
| `systems/MirrorSystem.kt` | Create | Records player state, provides delayed playback for DarkClone |
| `systems/AstronautSpawnSystem.kt` | Create | Spawns astronauts at rare intervals (10-15s), max 1 active |
| `systems/DebrisSpawnSystem.kt` | Create | Spawns debris at very rare intervals (20-30s) |
| `systems/GameSystem.kt` | Modify | Add enemies, astronauts, debris lists to GameContext. Add difficultyManager, mirrorSystem references. Extend GameEvent sealed class. |
| `systems/CollisionSystem.kt` | Modify | Add 5 new private collision methods (laser↔enemy, ship↔enemy, ship↔astronaut, laser↔astronaut, ship↔debris) |
| `systems/ScoreSystem.kt` | Modify | Add counters for enemies destroyed (by type), astronauts rescued, astronauts killed. Handle new event types. |
| `systems/PhysicsSystem.kt` | Modify | Add updateEnemies, updateAstronauts, updateDebris methods. Handle DarkClone movement via MirrorSystem. |
| `systems/SpawnSystem.kt` | Modify | Accept DifficultyConfig (or read from context) for asteroid spawn rate. |
| `screens/GameScreen.kt` | Modify | Initialize new systems, add new entity lists, update all systems in render(), render new entities, wire DifficultyManager. |
| `rendering/HudRenderer.kt` | Modify | Add astronaut count rescued to HUD display. |
| `screens/GameOverScreen.kt` | Modify | Display enemies destroyed (by type or total), astronauts rescued, astronauts killed. |
| `screens/GameOverScreen.kt` | Modify | Extend GameSession object with new stat fields. |
| `util/Constants.kt` | Modify | Add ~150 new constants: enemy stats (HP, speed, radius, points), astronaut/debris spawn intervals, difficulty curve params, collision radii, score values. |
| `managers/I18nManager.kt` | No change | No code changes needed—just add new keys to i18n bundle files. |
| `assets/i18n/messages*.properties` | Modify | Add new keys: enemies_destroyed, astronauts_rescued, astronauts_killed, enemy names (optional). |
| `test/.../EnemyTest.kt` | Create | Test enemy HP, damage states, points, movement |
| `test/.../DifficultyTest.kt` | Create | Test difficulty curve, safe zone, interpolation |
| `test/.../MirrorTest.kt` | Create | Test DarkClone mirroring logic, queue delay |
| `test/.../AstronautTest.kt` | Create | Test astronaut state transitions, rescue/kill |
| `test/.../CollisionTest.kt` | Modify | Add tests for 5 new collision pairs |
| `test/.../PhysicsTest.kt` | Modify | Add tests for enemy/astronaut/debris movement |
| `test/.../ScoreTest.kt` | Modify | Add tests for new point values, penalties, event counting |
| `test/.../SpawnTest.kt` | Modify | Add tests for enemy/astronaut/debris spawning |

## Interfaces / Contracts

### Enemy Hierarchy

```kotlin
// entities/Enemy.kt

abstract class Enemy(
    position: Vector2,
    velocity: Vector2,
    override val radius: Float
) : Entity {
    abstract val maxHealth: Int
    abstract val points: Int
    abstract val speed: Float
    
    var health: Int = maxHealth
        private set
    
    val isDestroyed: Boolean get() = health <= 0
    
    /**
     * Reduce HP by 1.
     * @return true if enemy is now destroyed
     */
    fun takeDamage(): Boolean {
        health--
        return isDestroyed
    }
    
    /**
     * Visual damage state based on remaining HP.
     * Used by renderer to change color/alpha.
     */
    fun getDamageState(): EnemyDamageState {
        val ratio = health.toFloat() / maxHealth.toFloat()
        return when {
            ratio > 0.66f -> EnemyDamageState.PRISTINE
            ratio > 0.33f -> EnemyDamageState.DAMAGED
            else -> EnemyDamageState.CRITICAL
        }
    }
}

enum class EnemyDamageState {
    PRISTINE, DAMAGED, CRITICAL
}

class LightFighter(position: Vector2) : Enemy(
    position = position,
    velocity = Vector2(-Constants.ENEMY_LIGHT_SPEED, 0f),
    radius = Constants.ENEMY_LIGHT_RADIUS
) {
    override val maxHealth = 1
    override val points = 150
    override val speed = Constants.ENEMY_LIGHT_SPEED
}

class MediumFrigate(position: Vector2) : Enemy(
    position = position,
    velocity = Vector2(-Constants.ENEMY_MEDIUM_SPEED, 0f),
    radius = Constants.ENEMY_MEDIUM_RADIUS
) {
    override val maxHealth = 2
    override val points = 250
    override val speed = Constants.ENEMY_MEDIUM_SPEED
}

class HeavyDestroyer(position: Vector2) : Enemy(
    position = position,
    velocity = Vector2(-Constants.ENEMY_HEAVY_SPEED, 0f),
    radius = Constants.ENEMY_HEAVY_RADIUS
) {
    override val maxHealth = 3
    override val points = 400
    override val speed = Constants.ENEMY_HEAVY_SPEED
}

class DarkClone(position: Vector2) : Enemy(
    position = position,
    velocity = Vector2(-Constants.ENEMY_CLONE_SPEED, 0f),
    radius = Constants.SHIP_RADIUS  // Same size as player
) {
    override val maxHealth = if (kotlin.random.Random.nextBoolean()) 2 else 3
    override val points = 500
    override val speed = Constants.ENEMY_CLONE_SPEED
    
    /** Whether the clone is currently firing (set by MirrorSystem) */
    var isFiring: Boolean = false
}
```

### Astronaut Entity

```kotlin
// entities/Astronaut.kt

class Astronaut(
    position: Vector2,
    velocity: Vector2 = Vector2(-Constants.ASTRONAUT_SPEED, 0f)
) : Entity {
    
    enum class State { FLOATING, RESCUED, DEAD }
    
    override val position: Vector2 = position.cpy()
    override val velocity: Vector2 = velocity.cpy()
    override val radius: Float = Constants.ASTRONAUT_RADIUS
    
    var state: State = State.FLOATING
        private set
    
    /** Time in current state (for animation) */
    var stateTimer: Float = 0f
        private set
    
    /** Whether the astronaut should be removed (animation complete) */
    val shouldRemove: Boolean get() = state != State.FLOATING && stateTimer > 0.5f
    
    fun rescue() {
        if (state == State.FLOATING) {
            state = State.RESCUED
            stateTimer = 0f
        }
    }
    
    fun kill() {
        if (state == State.FLOATING) {
            state = State.DEAD
            stateTimer = 0f
        }
    }
    
    override fun update(delta: Float) {
        position.mulAdd(velocity, delta)
        stateTimer += delta
    }
}
```

### SpaceDebris Entity

```kotlin
// entities/SpaceDebris.kt

class SpaceDebris(
    position: Vector2,
    velocity: Vector2 = Vector2(-Constants.DEBRIS_SPEED, 0f)
) : Entity {
    
    override val position: Vector2 = position.cpy()
    override val velocity: Vector2 = velocity.cpy()
    override val radius: Float = Constants.DEBRIS_RADIUS
    
    /** Phase for pulsing glow effect (radians) */
    var glowPhase: Float = 0f
        private set
    
    override fun update(delta: Float) {
        position.mulAdd(velocity, delta)
        glowPhase += delta * Constants.DEBRIS_GLOW_SPEED
    }
}
```

### DifficultyManager

```kotlin
// systems/DifficultyManager.kt

data class DifficultyConfig(
    val scrollSpeed: Float,
    val asteroidSpawnRate: Float,
    val enemySpawnRate: Float,
    val enemyTypeDistribution: EnemyTypeDistribution
)

data class EnemyTypeDistribution(
    val lightFighterWeight: Float,
    val mediumFrigateWeight: Float,
    val heavyDestroyerWeight: Float,
    val darkCloneWeight: Float
)

class DifficultyManager {
    
    /**
     * Calculate difficulty config for the given elapsed time.
     * Uses linear interpolation from start to end values.
     * Safe zone: first 15s (only asteroids, slow speed).
     */
    fun getConfig(elapsedTime: Float): DifficultyConfig {
        val t = (elapsedTime - Constants.DIFFICULTY_SAFE_ZONE).coerceAtLeast(0f)
        val progress = (t / Constants.DIFFICULTY_RAMP_DURATION).coerceIn(0f, 1f)
        
        return DifficultyConfig(
            scrollSpeed = lerp(Constants.DIFFICULTY_START_SCROLL_SPEED, 
                              Constants.DIFFICULTY_END_SCROLL_SPEED, 
                              progress),
            asteroidSpawnRate = lerp(Constants.DIFFICULTY_START_ASTEROID_RATE,
                                    Constants.DIFFICULTY_END_ASTEROID_RATE,
                                    progress),
            enemySpawnRate = if (elapsedTime < Constants.DIFFICULTY_SAFE_ZONE) {
                0f  // No enemies in safe zone
            } else {
                lerp(Constants.DIFFICULTY_START_ENEMY_RATE,
                    Constants.DIFFICULTY_END_ENEMY_RATE,
                    progress)
            },
            enemyTypeDistribution = EnemyTypeDistribution(
                lightFighterWeight = lerp(1.0f, 0.4f, progress),
                mediumFrigateWeight = lerp(0.0f, 0.3f, progress),
                heavyDestroyerWeight = lerp(0.0f, 0.2f, progress),
                darkCloneWeight = lerp(0.0f, 0.1f, progress)
            )
        )
    }
    
    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }
}
```

### MirrorSystem

```kotlin
// systems/MirrorSystem.kt

data class PlayerSnapshot(
    val position: Vector2,
    val isThrusting: Boolean,
    val isShooting: Boolean
)

class MirrorSystem : GameSystem {
    
    private val queue = ArrayDeque<PlayerSnapshot>()
    private val maxQueueSize = 30  // ~0.5s at 60 FPS
    
    override fun update(delta: Float, context: GameContext) {
        // Record current player state
        queue.addLast(PlayerSnapshot(
            position = context.ship.position.cpy(),
            isThrusting = context.ship.isThrusting,
            isShooting = false  // Set by GameScreen when laser fires
        ))
        
        // Keep queue bounded
        while (queue.size > maxQueueSize) {
            queue.removeFirst()
        }
        
        // Update DarkClones with delayed state
        val delayedIndex = 0  // Oldest entry (30 frames ago)
        if (queue.size >= maxQueueSize) {
            val delayedState = queue[delayedIndex]
            for (clone in context.enemies.filterIsInstance<DarkClone>()) {
                clone.position.y = delayedState.position.y
                clone.isFiring = delayedState.isShooting
            }
        }
    }
    
    /** Call when player fires a laser to record the shot in the queue */
    fun recordShot() {
        if (queue.isNotEmpty()) {
            val last = queue.removeLast()
            queue.addLast(last.copy(isShooting = true))
        }
    }
    
    fun reset() {
        queue.clear()
    }
}
```

### Updated GameContext

```kotlin
// systems/GameSystem.kt (modified)

data class GameContext(
    val ship: Ship,
    val asteroids: MutableList<Asteroid>,
    val lasers: MutableList<Laser>,
    val enemies: MutableList<Enemy>,
    val astronauts: MutableList<Astronaut>,
    val debris: MutableList<SpaceDebris>,
    val events: MutableList<GameEvent>,
    var score: Int,
    val difficultyManager: DifficultyManager,
    val mirrorSystem: MirrorSystem
)

sealed class GameEvent {
    // Existing
    data class AsteroidDestroyed(val asteroid: Asteroid, val points: Int) : GameEvent()
    data class ShipHit(val remainingLives: Int) : GameEvent()
    data class LaserAsteroidHit(val laser: Laser, val asteroid: Asteroid) : GameEvent()
    
    // New
    data class EnemyDestroyed(val enemy: Enemy, val points: Int) : GameEvent()
    data class AstronautRescued(val astronaut: Astronaut) : GameEvent()
    data class AstronautKilled(val astronaut: Astronaut) : GameEvent()
    data class DebrisCollected(val debris: SpaceDebris) : GameEvent()
}
```

### CollisionSystem Additions

```kotlin
// systems/CollisionSystem.kt (additions)

override fun update(delta: Float, context: GameContext) {
    // Existing
    checkLaserAsteroidCollisions(context)
    checkShipAsteroidCollisions(context)
    
    // New
    checkLaserEnemyCollisions(context)
    checkShipEnemyCollisions(context)
    checkShipAstronautCollisions(context)
    checkLaserAstronautCollisions(context)
    checkShipDebrisCollisions(context)
}

private fun checkLaserEnemyCollisions(context: GameContext) {
    val lasersToRemove = mutableListOf<Laser>()
    val enemiesToRemove = mutableListOf<Enemy>()
    
    for (laser in context.lasers) {
        if (lasersToRemove.contains(laser)) continue
        
        for (enemy in context.enemies) {
            if (enemiesToRemove.contains(enemy)) continue
            
            if (overlap(laser, enemy)) {
                lasersToRemove.add(laser)
                
                if (enemy.takeDamage()) {
                    enemiesToRemove.add(enemy)
                    context.events.add(GameEvent.EnemyDestroyed(enemy, enemy.points))
                    context.score += enemy.points
                }
                break
            }
        }
    }
    
    context.lasers.removeAll(lasersToRemove)
    context.enemies.removeAll(enemiesToRemove)
}

private fun checkShipEnemyCollisions(context: GameContext) {
    val ship = context.ship
    if (ship.isInvulnerable || ship.isDestroyed) return
    
    val toRemove = mutableListOf<Enemy>()
    
    for (enemy in context.enemies) {
        if (overlap(ship, enemy)) {
            ship.takeDamage()
            context.events.add(GameEvent.ShipHit(ship.lives))
            toRemove.add(enemy)
            break
        }
    }
    
    context.enemies.removeAll(toRemove)
}

private fun checkShipAstronautCollisions(context: GameContext) {
    val ship = context.ship
    if (ship.isInvulnerable || ship.isDestroyed) return  // Invulnerable ship does NOT rescue
    
    for (astronaut in context.astronauts) {
        if (astronaut.state == Astronaut.State.FLOATING && overlap(ship, astronaut)) {
            astronaut.rescue()
            context.events.add(GameEvent.AstronautRescued(astronaut))
            context.score += Constants.SCORE_ASTRONAUT_RESCUE
            break
        }
    }
}

private fun checkLaserAstronautCollisions(context: GameContext) {
    val lasersToRemove = mutableListOf<Laser>()
    
    for (laser in context.lasers) {
        if (lasersToRemove.contains(laser)) continue
        
        for (astronaut in context.astronauts) {
            if (astronaut.state == Astronaut.State.FLOATING && overlap(laser, astronaut)) {
                lasersToRemove.add(laser)
                astronaut.kill()
                context.events.add(GameEvent.AstronautKilled(astronaut))
                context.score -= Constants.SCORE_ASTRONAUT_KILL_PENALTY
                break
            }
        }
    }
    
    context.lasers.removeAll(lasersToRemove)
}

private fun checkShipDebrisCollisions(context: GameContext) {
    val ship = context.ship
    if (ship.lives >= Constants.SHIP_LIVES) return  // Already at max lives
    
    val toRemove = mutableListOf<SpaceDebris>()
    
    for (debris in context.debris) {
        if (overlap(ship, debris)) {
            ship.addLife()  // New method: increment lives, capped at SHIP_LIVES
            context.events.add(GameEvent.DebrisCollected(debris))
            toRemove.add(debris)
            break
        }
    }
    
    context.debris.removeAll(toRemove)
}
```

### Ship Addition (addLife method)

```kotlin
// entities/Ship.kt (addition)

/**
 * Add one life, capped at [Constants.SHIP_LIVES].
 * @return true if a life was added, false if already at max
 */
fun addLife(): Boolean {
    if (lives >= Constants.SHIP_LIVES) return false
    lives++
    return true
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Enemy HP, damage states, points | Create each enemy type, call `takeDamage()`, verify HP and `getDamageState()`. Verify `points` constant. |
| Unit | Astronaut state transitions | Create astronaut, call `rescue()` and `kill()`, verify state and `stateTimer`. Verify `shouldRemove` after 0.5s. |
| Unit | DifficultyManager curve | Call `getConfig()` at 0s, 15s, 60s, 180s. Verify safe zone (enemySpawnRate=0 before 15s). Verify interpolation at boundaries. |
| Unit | MirrorSystem queue | Record 30 frames, verify DarkClone reads from queue with 30-frame delay. Verify `recordShot()` sets `isShooting=true`. |
| Integration | CollisionSystem new pairs | Create overlapping entities (laser↔enemy, ship↔enemy, ship↔astronaut, laser↔astronaut, ship↔debris). Verify correct events emitted, HP/lives changed, entities removed. |
| Integration | PhysicsSystem new entities | Create enemies/astronauts/debris, call `update()`, verify position changes and off-screen removal. |
| Integration | ScoreSystem new events | Emit EnemyDestroyed, AstronautRescued, AstronautKilled events. Verify score changes and counters increment. |
| Integration | SpawnSystems | Verify EnemySpawnSystem spawns enemies at correct rate and type distribution. Verify AstronautSpawnSystem respects max-1-active. Verify DebrisSpawnSystem spawns at rare intervals. |

**New Test Files**:
- `EnemyTest.kt`: ~10 tests (HP, damage states, points, movement for each enemy type)
- `DifficultyTest.kt`: ~8 tests (safe zone, interpolation, boundary values, config at 0s/15s/60s/180s)
- `MirrorTest.kt`: ~6 tests (queue recording, 30-frame delay, shot mirroring, reset)
- `AstronautTest.kt`: ~7 tests (state transitions, rescue/kill, animation timer, shouldRemove)

**Extended Test Files**:
- `CollisionTest.kt`: +10 tests (5 new collision pairs × 2 scenarios each)
- `PhysicsTest.kt`: +6 tests (enemy/astronaut/debris movement, off-screen removal)
- `ScoreTest.kt`: +6 tests (enemy points, astronaut rescue/kill penalties, event counting)
- `SpawnTest.kt`: +8 tests (enemy/astronaut/debris spawning, rate, distribution, constraints)

## Migration / Rollout

No migration required. All changes are additive—v0.1.0 behavior is preserved. New entity lists start empty, new systems are initialized in GameScreen.show(), and existing systems continue to work unchanged (except for new collision pairs and event types, which are opt-in via the new entities).

**Rollout Plan**:
1. Implement entities + Constants (no behavior change yet)
2. Implement new systems (DifficultyManager, MirrorSystem, spawn systems)
3. Update CollisionSystem, ScoreSystem, PhysicsSystem (new behavior for new entities)
4. Update GameScreen (wire everything together)
5. Update rendering, HUD, GameOverScreen
6. Add tests
7. Update i18n bundles

Each step is independently testable. If issues arise, new systems can be disabled by removing them from the update order in GameScreen.

## Open Questions

- [ ] **DarkClone laser storage**: Should DarkClone lasers be stored in the main `lasers` list (with a flag to distinguish them) or in a separate `darkCloneLasers` list? *Decision: Use the main `lasers` list for simplicity. DarkClone lasers move right like player lasers and collide with asteroids/enemies. If this causes issues, split later.*
- [ ] **Astronaut rescue during invulnerability**: The spec says "invulnerable ship does NOT rescue" (intentional design). This means players must risk taking damage to rescue astronauts. *Confirm this is the desired behavior.*
- [ ] **Enemy type distribution weights**: The proposal suggests early game = mostly Fighters, late game = more Frigates/Destroyers/Clones. The exact weights need tuning. *Start with: 0s = 100% Fighter, 180s = 40% Fighter / 30% Frigate / 20% Destroyer / 10% Clone. Adjust based on playtesting.*
- [ ] **Difficulty curve endpoints**: The exact values for `DIFFICULTY_END_*` constants need tuning. *Start with: scrollSpeed 2→5, asteroidSpawnRate 2s→0.5s, enemySpawnRate 0→3s. Adjust based on playtesting.*
