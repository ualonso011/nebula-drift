# Space Debris Specification

## Purpose

Defines space debris entities that provide health recovery (+1 life, capped at 3). Creates health management gameplay. Rare spawn with glow visual effect.

## Requirements

### Requirement: Space Debris Entity

SpaceDebris MUST float leftward at slow speed. Visual: golden/yellow with glow effect (layered circles or alpha pulse). MUST be visually distinct from enemies, astronauts, and asteroids.

#### Scenario: Debris spawns and drifts

- GIVEN debris spawned at x=800, y=random
- WHEN 5 seconds elapse
- THEN debris has moved leftward, glow effect visible

---

### Requirement: Debris Collection

Ship↔debris overlap MUST trigger collection. Effect: +1 life (never exceeds 3 max). Visual feedback: brief positive animation. Debris removed after collection.

#### Scenario: Ship collects debris (below max lives)

- GIVEN ship has 2 lives, debris at FLOATING
- WHEN ship overlaps debris
- THEN ship lives = 3, positive visual feedback, debris removed

#### Scenario: Ship collects debris (at max lives)

- GIVEN ship has 3 lives (max), debris at FLOATING
- WHEN ship overlaps debris
- THEN ship lives remain 3 (no excess), debris removed, feedback still plays

---

### Requirement: Debris Spawning

Debris MUST spawn at very rare intervals (20-30 seconds). Y position: random within screen bounds. Maximum 1 debris active at a time.

#### Scenario: Debris spawn interval

- GIVEN no debris active
- WHEN 25 seconds elapse
- THEN debris spawns at x=screen_width, y=random

#### Scenario: No duplicate debris

- GIVEN debris already active
- WHEN spawn timer elapses
- THEN no new debris spawns

---

## Coverage Summary

| Event | Effect | Visual | Notes |
|-------|--------|--------|-------|
| Spawn | Debris appears | Glow effect | Every 20-30s |
| Collection (lives < 3) | +1 life | Positive feedback | Debris removed |
| Collection (lives = 3) | No change | Positive feedback | Debris removed |

Total: 3 requirements, 5 scenarios
