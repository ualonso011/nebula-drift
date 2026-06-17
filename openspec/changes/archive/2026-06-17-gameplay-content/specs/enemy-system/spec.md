# Enemy System Specification

## Purpose

Defines 4 enemy types with unique movement, damage states, and scoring. Enemies spawn from the right side and move left, creating combat encounters beyond asteroids.

## Requirements

### Requirement: Enemy Base Class

The system MUST provide an abstract `Enemy` class implementing `Entity` with position, velocity, HP, damage state, score value, and hitbox. All enemy subtypes MUST inherit from this base.

#### Scenario: Enemy lifecycle

- GIVEN an enemy spawned on screen
- WHEN its HP reaches 0
- THEN the enemy is removed and its score value is awarded

---

### Requirement: Light Fighter

LightFighter MUST be small, fast, 1 HP. Movement: constant leftward velocity. Score: +150 on destruction.

#### Scenario: Light Fighter destroyed in one hit

- GIVEN a LightFighter at x=700
- WHEN hit by laser
- THEN HP=0, enemy removed, score += 150

#### Scenario: Light Fighter movement

- GIVEN LightFighter at x=700, y=300
- WHEN 1 second elapses at 60 FPS
- THEN x has decreased by velocity * 60 frames

---

### Requirement: Medium Frigate

MediumFrigate MUST be medium-sized, 2 HP, slower than Fighter. First hit MUST change visual (color/alpha). Score: +250 on destruction.

#### Scenario: Medium Frigate takes first hit

- GIVEN MediumFrigate at 2 HP
- WHEN hit by laser
- THEN HP=1, visual changes (color shift or alpha reduction)

#### Scenario: Medium Frigate destroyed

- GIVEN MediumFrigate at 1 HP (damaged visual)
- WHEN hit by laser
- THEN HP=0, enemy removed, score += 250

---

### Requirement: Heavy Destroyer

HeavyDestroyer MUST be large, 3 HP, slowest enemy. Visual MUST change per hit (3 distinct states). Score: +400 on destruction.

#### Scenario: Heavy Destroyer damage progression

- GIVEN HeavyDestroyer at 3 HP
- WHEN hit by laser (first time)
- THEN HP=2, visual state = "damaged-1"
- WHEN hit again
- THEN HP=1, visual state = "damaged-2"
- WHEN hit again
- THEN HP=0, removed, score += 400

---

### Requirement: Dark Clone

DarkClone MUST mirror player Y-position with ~0.5s delay (30-frame queue). MUST mirror player shot state (fires when player fires). 2-3 HP. Similar appearance to player ship. Score: +500 on destruction.

#### Scenario: Dark Clone mirrors player position

- GIVEN player moves from y=300 to y=100 over 30 frames
- WHEN DarkClone mirror system processes queue
- THEN DarkClone Y follows player Y with ~30 frame delay

#### Scenario: Dark Clone mirrors player shots

- GIVEN player fires laser at frame N
- WHEN MirrorSystem processes frame N+30
- THEN DarkClone fires a laser (mirrored shot)

#### Scenario: Dark Clone destroyed

- GIVEN DarkClone at 2 HP
- WHEN hit twice by laser
- THEN HP=0, removed, score += 500

---

### Requirement: Enemy Spawning

EnemySpawnSystem MUST spawn enemies from right edge at random Y. Spawn rate MUST increase with difficulty. Early game: mostly Fighters. Late game: more Frigates, Destroyers, Clones.

#### Scenario: Enemy spawn during gameplay

- GIVEN game elapsed time > 15s (safe zone passed)
- WHEN enemy spawn timer elapses
- THEN enemy spawns at x=screen_width, y=random, type based on difficulty

#### Scenario: No enemies in safe zone

- GIVEN game elapsed time < 15s
- WHEN spawn tick occurs
- THEN zero enemies spawn (only asteroids)

---

### Requirement: Enemy Collision

CollisionSystem MUST detect laser↔enemy and ship↔enemy overlaps. Laser↔enemy: enemy loses 1 HP, laser consumed. Ship↔enemy (no invulnerability): ship loses 1 life, enemy destroyed.

#### Scenario: Laser hits enemy

- GIVEN laser overlaps enemy hitbox
- WHEN collision check runs
- THEN enemy HP -= 1, laser removed

#### Scenario: Ship collides with enemy

- GIVEN ship overlaps enemy (not invulnerable)
- WHEN collision check runs
- THEN ship loses 1 life, enemy destroyed, invulnerability starts

---

## Coverage Summary

| Enemy Type | HP | Speed | Score | Visual States |
|------------|----|----|-------|---------------|
| LightFighter | 1 | Fast | +150 | 1 |
| MediumFrigate | 2 | Medium | +250 | 2 |
| HeavyDestroyer | 3 | Slow | +400 | 3 |
| DarkClone | 2-3 | Mirrors | +500 | 1 (player-like) |

Total: 4 enemy types, 12 scenarios
