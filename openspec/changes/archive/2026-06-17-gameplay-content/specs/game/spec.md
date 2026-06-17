# Delta for Game (Modified Capability)

## MODIFIED Requirements

### Requirement: Collision Detection (REQ-INIT-008)

The system MUST detect AABB overlaps for: laser↔asteroid, ship↔asteroid, **laser↔enemy, ship↔enemy, ship↔astronaut, laser↔astronaut, ship↔debris**.

- **laser↔asteroid**: asteroid loses 1 HP, laser consumed.
- **ship↔asteroid** (no invulnerability): ship loses 1 life, invulnerability starts.
- **laser↔enemy**: enemy loses 1 HP, laser consumed.
- **ship↔enemy** (no invulnerability): ship loses 1 life, enemy destroyed, invulnerability starts.
- **ship↔astronaut** (not invulnerable): astronaut RESCUED (+500), wave animation.
- **laser↔astronaut**: astronaut DEAD (-300), sad animation.
- **ship↔debris**: +1 life (capped at 3), debris removed, positive feedback.

(Previously: Only laser↔asteroid and ship↔asteroid pairs detected)

#### Scenario: Laser hits enemy

- GIVEN laser overlaps enemy hitbox
- WHEN collision check runs
- THEN enemy HP -= 1, laser removed

#### Scenario: Ship collides with enemy

- GIVEN ship overlaps enemy (not invulnerable)
- WHEN collision check runs
- THEN ship loses 1 life, enemy destroyed, invulnerability starts

#### Scenario: Ship rescues astronaut

- GIVEN ship overlaps astronaut (not invulnerable)
- WHEN collision check runs
- THEN astronaut state = RESCUED, score += 500

#### Scenario: Laser kills astronaut

- GIVEN laser overlaps astronaut
- WHEN collision check runs
- THEN astronaut state = DEAD, score -= 300

#### Scenario: Ship collects debris

- GIVEN ship overlaps debris
- WHEN collision check runs
- THEN ship lives += 1 (capped at 3), debris removed

---

### Requirement: Score System (REQ-INIT-009)

Points MUST be awarded on:

- Asteroid destruction: small=100, medium=200, large=300.
- **Enemy destruction**: LightFighter=+150, MediumFrigate=+250, HeavyDestroyer=+400, DarkClone=+500.
- **Astronaut rescue**: +500.
- **Astronaut kill**: -300 (penalty).
- Time-survived bonus: 1 point/second.

Score MUST display in HUD. Stats MUST track: enemies destroyed (by type), astronauts rescued, astronauts killed.

(Previously: Only asteroid destruction and time bonus)

#### Scenario: Enemy destroyed awards points

- GIVEN LightFighter destroyed
- WHEN HP reaches 0
- THEN score += 150, enemiesDestroyed[LightFighter] += 1

#### Scenario: Astronaut rescue awards points

- GIVEN astronaut rescued
- WHEN rescue animation completes
- THEN score += 500, astronautsRescued += 1

#### Scenario: Astronaut kill penalizes

- GIVEN astronaut killed by laser
- WHEN death animation completes
- THEN score -= 300, astronautsKilled += 1

#### Scenario: Stats tracked for GameOver

- GIVEN game in progress
- WHEN game ends
- THEN stats available: enemiesDestroyed (by type), astronautsRescued, astronautsKilled

---

### Requirement: HUD Overlay (REQ-INIT-013)

During gameplay, HUD MUST show: lives (heart icons), score (numeric), timer (M:SS). **HUD MAY display enemy/astronaut stats (optional, P2).** HUD MUST be rendered above game entities.

(Previously: Only lives, score, timer)

#### Scenario: HUD displays core stats

- GIVEN 2 lives, score 150, time 30s
- WHEN rendering
- THEN HUD shows "♥♥", "150", "0:30"

---

### Requirement: Game Over Screen (REQ-INIT-012)

Game over MUST display: final score, time survived, **"Retry" button, "Main Menu" button, stats summary (enemies destroyed by type, astronauts rescued/killed)**. "Retry" MUST restart with reset state. "Main Menu" MUST return to menu.

(Previously: Only final score, time survived, Retry, Main Menu)

#### Scenario: Game over shows stats

- GIVEN game ended with 5 fighters, 2 frigates destroyed, 3 astronauts rescued, 1 killed
- WHEN GameOver screen renders
- THEN stats summary displays: "Fighters: 5, Frigates: 2, Rescued: 3, Killed: 1"

#### Scenario: Retry resets all state

- GIVEN game over
- WHEN "Retry" tapped
- THEN new game starts (lives=3, score=0, stats reset, difficulty reset)

---

## ADDED Requirements

### Requirement: GameScreen Entity Lists

GameScreen MUST maintain 3 new entity lists: `enemies: MutableList<Enemy>`, `astronauts: MutableList<Astronaut>`, `debris: MutableList<SpaceDebris>`. Lists MUST be initialized empty and cleared on game reset.

#### Scenario: Entity lists initialized

- GIVEN GameScreen created
- WHEN game starts
- THEN enemies, astronauts, debris lists are empty

#### Scenario: Entity lists cleared on retry

- GIVEN game over, player taps Retry
- WHEN new game starts
- THEN all 3 lists cleared

---

### Requirement: System Update Order

GameScreen.update() MUST call systems in this order: Physics → Spawn → EnemySpawn → Mirror → Collision → Score. Each system receives delta time and GameContext.

#### Scenario: Systems execute in order

- GIVEN frame N begins
- WHEN update() called
- THEN systems execute: Physics, Spawn, EnemySpawn, Mirror, Collision, Score (in that order)

---

### Requirement: Constants Expansion

Constants MUST define ~150 new values across: enemy HP/speed/score, astronaut spawn interval/score, debris spawn interval/life cap, difficulty curve start/end/duration, collision radii, visual colors.

#### Scenario: Constants accessible

- GIVEN Constants object
- WHEN enemy/astronaut/debris/difficulty values accessed
- THEN all values defined and non-null

---

## Coverage Summary

| Area | Reqs Added | Reqs Modified | Scenarios |
|------|------------|---------------|-----------|
| CollisionSystem | 0 | 1 (REQ-INIT-008) | 5 new scenarios |
| ScoreSystem | 0 | 1 (REQ-INIT-009) | 4 new scenarios |
| HUD | 0 | 1 (REQ-INIT-013) | 1 scenario (unchanged) |
| GameOver | 0 | 1 (REQ-INIT-012) | 2 new scenarios |
| GameScreen | 3 | 0 | 4 scenarios |
| **Total** | **3 added** | **4 modified** | **16 scenarios** |
