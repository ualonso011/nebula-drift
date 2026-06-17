# Animations Specification

## Purpose

Frame-based entity animations using libGDX `Animation<TextureRegion>`. Provides visual feedback for thrust, explosions, astronaut states, and enemy damage. All animation frames are procedurally generated (included in TextureAtlas).

## Requirements

### REQ-ANIM-001: Ship Thrust Flicker — P0

Ship engine glow MUST alternate between 2 frames (flicker effect) while thrusting. Animation MUST loop at ~8 FPS (0.125s per frame). When not thrusting, engine glow MUST NOT render.

#### Scenario: Thrust flicker visible

- GIVEN ship thrusting
- WHEN 0.25 seconds elapse
- THEN engine glow has alternated between frame 0 and frame 1 at least once

#### Scenario: No thrust = no glow

- GIVEN ship not thrusting
- WHEN render() called
- THEN no engine glow sprite rendered

---

### REQ-ANIM-002: Explosion Animation — P0

Explosion MUST play a 4–6 frame spritesheet animation (procedurally generated). Animation MUST play once (no loop) then disappear. Duration: ~0.4–0.6s total. Used for asteroid/enemy/ship destruction.

#### Scenario: Explosion plays once on destroy

- GIVEN asteroid destroyed at position (x, y)
- WHEN explosion animation starts
- THEN 4-6 frames play over ~0.5s
- WHEN animation completes
- THEN explosion sprite removed

---

### REQ-ANIM-003: Astronaut Animations — P0

Two astronaut state animations:

| State | Animation | Behavior |
|-------|-----------|----------|
| RESCUED | Wave goodbye | Sine Y-offset, 0.5–1s, then remove |
| DEAD | Fall down | Move down + alpha fade, 0.5–1s, then remove |

Animations MUST play once then remove the astronaut entity.

#### Scenario: Rescue wave animation

- GIVEN astronaut rescued
- WHEN RESCUED state entered
- THEN astronaut bobs up/down (sine wave) for 0.5–1s then removed

#### Scenario: Death fall animation

- GIVEN astronaut killed
- WHEN DEAD state entered
- THEN astronaut moves down + fades out over 0.5–1s then removed

---

### REQ-ANIM-004: Enemy Damage Flash — P1

Enemy taking damage MUST show a brief spark/flash overlay (~0.1s). Flash MUST NOT interfere with sprite visibility. Applies to all enemy types with >1 HP.

#### Scenario: Enemy flashes on hit

- GIVEN MediumFrigate at 2 HP
- WHEN hit by laser (HP → 1)
- THEN brief flash overlay visible for ~0.1s
- WHEN flash duration elapsed
- THEN normal damaged sprite shown

---

### REQ-ANIM-005: Asteroid Rotation — P0

Asteroids MUST render with their existing `rotation` field applied via `SpriteBatch`. Rotation MUST be continuous (updated each frame by existing rotation speed).

#### Scenario: Asteroid rotates continuously

- GIVEN asteroid with rotation speed = 30°/s
- WHEN 1 second elapses
- THEN asteroid sprite has rotated 30°

---

## Coverage Summary

| Requirement | Priority | Scenarios |
|-------------|----------|-----------|
| REQ-ANIM-001 Thrust Flicker | P0 | 2 |
| REQ-ANIM-002 Explosion | P0 | 1 |
| REQ-ANIM-003 Astronaut | P0 | 2 |
| REQ-ANIM-004 Enemy Flash | P1 | 1 |
| REQ-ANIM-005 Asteroid Rotation | P0 | 1 |
| **Total** | | **7** |
