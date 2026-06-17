# Difficulty System Specification

## Purpose

Provides time-based progressive difficulty via DifficultyManager. All spawn rates, speeds, and enemy variety scale smoothly over elapsed time. 15-second safe zone at start.

## Requirements

### Requirement: DifficultyManager

DifficultyManager MUST provide DifficultyConfig each frame based on elapsed game time. Config includes: scroll speed multiplier, asteroid spawn interval, enemy spawn interval, enemy type weights. All values MUST interpolate linearly from start to end over time.

#### Scenario: Difficulty config at safe zone boundary

- GIVEN game elapsed time = 15s
- WHEN DifficultyManager.getConfig() called
- THEN config returns end-of-safe-zone values (slow speed, low spawn rate, only fighters)

#### Scenario: Difficulty config at mid-game

- GIVEN game elapsed time = 60s
- WHEN DifficultyManager.getConfig() called
- THEN config returns interpolated values (medium speed, medium spawn rate, mixed enemy types)

#### Scenario: Difficulty config at late game

- GIVEN game elapsed time = 180s
- WHEN DifficultyManager.getConfig() called
- THEN config returns end-curve values (fast speed, high spawn rate, all enemy types including clones)

---

### Requirement: Safe Zone

First 15 seconds MUST be safe zone. During safe zone: only asteroids spawn (no enemies, no astronauts, no debris). Scroll speed MUST be at minimum.

#### Scenario: No enemies in safe zone

- GIVEN game elapsed time < 15s
- WHEN EnemySpawnSystem.update() called
- THEN zero enemies spawn

#### Scenario: Enemies start after safe zone

- GIVEN game elapsed time = 15.1s
- WHEN EnemySpawnSystem.update() called
- THEN enemies MAY spawn (difficulty > 0)

---

### Requirement: Difficulty Curve Interpolation

All difficulty parameters MUST use linear interpolation: `value = start + (end - start) * (elapsed / totalDuration)`. Start values apply at t=15s. End values apply at t=totalDuration (e.g., 180s). All curves MUST be smooth (no jumps).

#### Scenario: Linear interpolation at midpoint

- GIVEN start speed = 1.0, end speed = 3.0, totalDuration = 180s
- WHEN elapsed = 97.5s (midpoint between 15s and 180s)
- THEN speed ≈ 2.0 (midpoint between 1.0 and 3.0)

#### Scenario: Difficulty clamped to end values

- GIVEN elapsed > totalDuration
- WHEN DifficultyManager.getConfig() called
- THEN all params clamped to end values (no further increase)

---

### Requirement: Difficulty-Driven Spawning

SpawnSystem (asteroids) and EnemySpawnSystem MUST consume DifficultyConfig each frame. Asteroid spawn interval and speed MUST scale with difficulty. Enemy spawn interval and type weights MUST scale with difficulty.

#### Scenario: Asteroid spawn rate increases

- GIVEN difficulty config at t=15s: asteroid interval = 2.0s
- WHEN time advances to t=60s, interval = 1.0s
- THEN asteroids spawn twice as fast

#### Scenario: Enemy type variety increases

- GIVEN difficulty config at t=15s: Fighter weight = 100%, others = 0%
- WHEN time advances to t=180s: Fighter = 30%, Frigate = 30%, Destroyer = 20%, Clone = 20%
- THEN enemy spawns reflect new weights

---

## Coverage Summary

| Time | Safe Zone | Scroll Speed | Enemy Spawn | Enemy Types |
|------|-----------|--------------|-------------|-------------|
| 0-15s | Yes | Min (1.0x) | None | None |
| 15-60s | No | 1.0x → 2.0x | Low → Med | Fighters only → mixed |
| 60-180s | No | 2.0x → 3.0x | Med → High | All types, clones appear |

Total: 4 requirements, 8 scenarios
