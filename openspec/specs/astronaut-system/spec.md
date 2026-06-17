# Astronaut System Specification

## Purpose

Defines astronaut entities that float in space. Ship contact rescues them (+500 pts); laser contact kills them (-300 pts). Creates risk/reward gameplay around target priority.

## Requirements

### Requirement: Astronaut Entity and State Machine

Astronaut MUST have states: FLOATING, RESCUED, DEAD. Movement: slow leftward drift. Visual: green/teal with helmet ring. MUST be visually distinct from enemies and debris.

#### Scenario: Astronaut spawns and floats

- GIVEN astronaut spawned at x=800, y=random
- WHEN 5 seconds elapse
- THEN astronaut has moved leftward at slow constant speed, state = FLOATING

---

### Requirement: Astronaut Rescue

Ship↔astronaut overlap (ship not invulnerable) MUST trigger RESCUED state. Score: +500. Animation: brief wave goodbye (0.5-1s). Astronaut removed after animation.

#### Scenario: Ship rescues astronaut

- GIVEN astronaut at FLOATING, ship not invulnerable
- WHEN ship overlaps astronaut
- THEN state = RESCUED, score += 500, wave animation plays
- WHEN animation completes
- THEN astronaut removed from game

#### Scenario: Invulnerable ship does NOT rescue

- GIVEN astronaut at FLOATING, ship invulnerable
- WHEN ship overlaps astronaut
- THEN no rescue occurs, astronaut remains FLOATING

---

### Requirement: Astronaut Kill (Penalty)

Laser↔astronaut overlap MUST trigger DEAD state. Score: -300 (penalty). Animation: brief sad animation (0.5-1s). Astronaut removed after animation.

#### Scenario: Laser kills astronaut

- GIVEN astronaut at FLOATING
- WHEN laser overlaps astronaut
- THEN state = DEAD, score -= 300, sad animation plays
- WHEN animation completes
- THEN astronaut removed from game

---

### Requirement: Astronaut Spawning

Astronauts MUST spawn at rare intervals (10-15 seconds). Y position: random within screen bounds. Maximum 1 astronaut active at a time.

#### Scenario: Astronaut spawn interval

- GIVEN no astronaut active
- WHEN 12 seconds elapse
- THEN astronaut spawns at x=screen_width, y=random

#### Scenario: No duplicate astronauts

- GIVEN astronaut already active (FLOATING/RESCUED/DEAD)
- WHEN spawn timer elapses
- THEN no new astronaut spawns

---

## Coverage Summary

| State | Trigger | Score | Animation | Duration |
|-------|---------|-------|-----------|----------|
| FLOATING | Spawn | 0 | Float drift | Until rescue/kill |
| RESCUED | Ship contact | +500 | Wave goodbye | 0.5-1s |
| DEAD | Laser hit | -300 | Sad animation | 0.5-1s |

Total: 4 requirements, 7 scenarios
