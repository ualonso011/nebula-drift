# Particle Effects Specification

## Purpose

Pool-backed particle system that spawns visual effects on game events. Provides feedback for destruction, collection, thrust, and damage. Performance budget: max 300 active particles.

## Requirements

### REQ-PARTICLE-001: ParticleManager Lifecycle — P0

`ParticleManager` MUST maintain a pool of particles (max 300 active). MUST expose `spawn(effect, x, y)`, `update(delta)`, `render(batch)`. Inactive particles MUST be recycled to pool. MUST update and render all active particles each frame.

#### Scenario: Particle pool recycling

- GIVEN 300 active particles (pool full)
- WHEN new particle requested
- THEN oldest expired particle is recycled OR new spawn is skipped (no crash)

#### Scenario: Update and render cycle

- GIVEN 50 active particles
- WHEN update(delta) then render(batch) called
- THEN all 50 particles updated by delta and drawn via SpriteBatch

---

### REQ-PARTICLE-002: Effect Templates — P0

Six effect templates MUST be defined:

| Effect | Trigger | Color | Behavior |
|--------|---------|-------|----------|
| Explosion | Asteroid/Enemy destroyed | Orange/red | Expand + fade, 20-40 particles |
| Engine trail | Ship thrusting | Blue/white | Small, short-lived, continuous |
| Rescue sparkle | Astronaut rescued | Green/yellow | Sparkle pattern, 15-25 particles |
| Debris sparkle | Debris collected | Golden | Sparkle pattern, 10-15 particles |
| Damage sparks | Ship takes damage | Red/orange | Brief burst, 10-20 particles |
| Ship destruction | Ship destroyed | Orange/red/yellow | Massive, 60-100 particles, screen-filling |

#### Scenario: Explosion on asteroid destroy

- GIVEN asteroid at (400, 300) destroyed
- WHEN AsteroidDestroyed event fires
- THEN explosion effect spawns at (400, 300) with orange/red particles

#### Scenario: Engine trail while thrusting

- GIVEN ship thrusting continuously
- WHEN 0.5 seconds elapse
- THEN engine trail particles spawn continuously behind ship

#### Scenario: Ship destruction massive explosion

- GIVEN ship destroyed
- WHEN ShipDestroyed event fires
- THEN 60-100 particles spawn at ship position filling screen area

---

### REQ-PARTICLE-003: Event Hooks — P0

ParticleManager MUST subscribe to GameEvent stream and auto-spawn effects:

| GameEvent | Effect |
|-----------|--------|
| AsteroidDestroyed | Explosion at asteroid position |
| EnemyDestroyed | Explosion at enemy position |
| ShipDestroyed | Ship destruction at ship position |
| AstronautRescued | Rescue sparkle at astronaut position |
| DebrisCollected | Debris sparkle at debris position |
| ShipDamaged | Damage sparks at ship position |
| ShipThrusting (continuous) | Engine trail behind ship |

#### Scenario: All event types trigger effects

- GIVEN ParticleManager subscribed to GameEvent stream
- WHEN each of the 7 event types fires
- THEN corresponding particle effect spawns at event position

#### Scenario: Thrust effect stops when input released

- GIVEN ship thrusting (engine trail active)
- WHEN thrust input released
- THEN engine trail stops spawning (existing particles fade naturally)

---

## Coverage Summary

| Requirement | Priority | Scenarios |
|-------------|----------|-----------|
| REQ-PARTICLE-001 Lifecycle | P0 | 2 |
| REQ-PARTICLE-002 Templates | P0 | 3 |
| REQ-PARTICLE-003 Event Hooks | P0 | 2 |
| **Total** | | **7** |
