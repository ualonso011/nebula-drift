# Game Rendering Specification

## Purpose

Replaces all `ShapeRenderer` placeholder drawing with `SpriteBatch` + procedural `TextureAtlas`. `GameRenderer` owns all entity draw calls. Sprites are generated at runtime via `Pixmap` — no external art dependencies.

## Requirements

### REQ-VISUAL-001: GameRenderer Extraction — P0

`GameRenderer` MUST own all entity rendering. `GameScreen` MUST delegate draw calls to `GameRenderer`. `GameRenderer` MUST use a single `SpriteBatch` and a procedural `TextureAtlas`. Zero `ShapeRenderer` calls in normal (non-debug) mode.

#### Scenario: GameRenderer renders all entities

- GIVEN GameScreen with active ship, asteroids, enemies, astronauts, debris
- WHEN render() is called
- THEN GameRenderer draws all entities via SpriteBatch (zero ShapeRenderer calls)

#### Scenario: Debug mode fallback

- GIVEN debug toggle enabled
- WHEN render() is called
- THEN ShapeRenderer MAY be used for hitbox overlays

---

### REQ-VISUAL-002: Procedural TextureAtlas — P0

All sprites MUST be generated at init via `Pixmap`. Atlas MUST contain sprites for: ship (3 damage states), asteroids (3 sizes × 3 damage states = 9), enemies (4 types × damage states), astronaut (3 states), debris (1 + glow), laser (1 + glow). Atlas MUST be built once and shared.

#### Scenario: Atlas contains all required sprites

- GIVEN game initialized
- WHEN TextureAtlas queried for any entity sprite key
- THEN a valid TextureRegion is returned (no missing sprites)

#### Scenario: Ship damage state sprites

- GIVEN ship at 3 lives (pristine), 2 lives (damaged), 1 life (critical)
- WHEN GameRenderer draws ship
- THEN correct sprite selected per damage state (color/alpha differs)

---

### REQ-VISUAL-003: Entity Sprite Mapping — P0

Each entity type MUST map to its atlas region(s):

| Entity | Sprites | Selection Key |
|--------|---------|---------------|
| Ship | 3 (pristine/damaged/critical) | `ship_{state}` |
| Asteroid | 9 (3 sizes × 3 HP) | `asteroid_{size}_{hp}` |
| LightFighter | 1–2 | `enemy_fighter_{state}` |
| MediumFrigate | 2 | `enemy_frigate_{state}` |
| HeavyDestroyer | 3 | `enemy_destroyer_{state}` |
| DarkClone | 1 (player-like) | `enemy_clone` |
| Astronaut | 3 (floating/rescued/dead) | `astro_{state}` |
| Debris | 1 + glow | `debris` |
| Laser | 1 + glow | `laser` |

#### Scenario: Asteroid sprite changes on damage

- GIVEN large asteroid at 3 HP
- WHEN hit by laser (HP → 2)
- THEN sprite changes from `asteroid_large_3` to `asteroid_large_2`

#### Scenario: Enemy sprite changes on damage

- GIVEN HeavyDestroyer at 3 HP
- WHEN hit (HP → 2)
- THEN sprite changes from `enemy_destroyer_3` to `enemy_destroyer_2`

---

### REQ-VISUAL-004: Parallax Background — P1

Background MUST render 2–3 parallax layers (far stars, mid nebulas, near planets). Layers MUST scroll at different speeds (far=slow, near=fast). Background MUST be procedurally generated or use static textures.

#### Scenario: Parallax depth perception

- GIVEN 3 background layers scrolling
- WHEN 1 second elapses
- THEN far layer has moved least, near layer has moved most

---

## Coverage Summary

| Requirement | Priority | Scenarios |
|-------------|----------|-----------|
| REQ-VISUAL-001 GameRenderer | P0 | 2 |
| REQ-VISUAL-002 TextureAtlas | P0 | 2 |
| REQ-VISUAL-003 Sprite Mapping | P0 | 2 |
| REQ-VISUAL-004 Parallax BG | P1 | 1 |
| **Total** | | **7** |
