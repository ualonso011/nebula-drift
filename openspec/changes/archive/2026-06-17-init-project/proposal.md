# Proposal: Init Project — Nebula Drift v0.1 Foundation

## Intent

Deliver a playable vertical slice of Nebula Drift: libGDX + KTX project scaffold, core game loop (ship, gravity, asteroids, collision, shooting, HUD), and desktop launcher. Prove the architecture works end-to-end before adding Android, audio, or premium visuals.

## Scope

### In Scope
- Multi-module Gradle (core, android, desktop), dependency wiring, asset/i18n directory skeleton
- KtxGame + KtxScreen lifecycle (Menu → Game → Game Over → Menu)
- Player ship with gravity simulation, thrust (tap left), 3 lives & damage states
- Asteroids: 3 sizes, linear movement, degradation on hit, spawn wave
- Player shooting (tap right), bullet cooldown & lifetime
- Collision detection: bullet↔asteroid, ship↔asteroid
- Score (time + destruction), lives, timer in HUD overlay
- Desktop launcher via LWJGL3, Gradle wrapper, build verification

### Out of Scope
- Android launcher / APK pipeline
- Astronauts, space debris, enemies (fighters, frigate, destroyer, clone)
- Leaderboard & settings screens
- Audio — music, SFX, volume controls
- i18n translation files (Euskera/ES/EN)
- Premium visuals — nebulas, particles, glow, volumetric lighting
- Touch controls beyond tap-left/tap-right
- Version system & changelog

## Capabilities

### New Capabilities
- `project-setup`: Multi-module Gradle, dependency management, build config
- `game-loop`: KtxGame/KtxScreen lifecycle, delta-time, entity updates
- `physics-gravity`: Acceleration, velocity damping, screen-edge bounds
- `collision-detection`: Axis-aligned overlap, entity pair dispatch
- `asteroid-system`: Spawn wave, 3 size tiers, degradation, linear movement
- `player-shooting`: Bullet spawn, cooldown, lifetime, off-screen cleanup
- `scoring-hud`: Score, lives, timer; debug overlay via SpriteBatch/BitmapFont
- `screen-lifecycle`: Menu → Game → Game Over → Menu transitions
- `desktop-launcher`: Lwjgl3Application, window config, FPS cap

### Modified Capabilities
None — greenfield project.

## Approach

**Gradle**: Multi-module (`core`, `android`, `desktop`). `core` hosts all game logic; `desktop` provides the LWJGL3 launcher; `android` module created but inactive in v0.1.

**Architecture**: KtxGame + KtxScreen for screen management. Entity-component pattern via data classes (position, velocity, renderable, health). Systems per concern: physics, collision, asteroid spawner, player input, renderer.

**Rendering**: SpriteBatch for textures, ShapeRenderer for debug/collision boxes. Pixel-art placeholder assets until visual polish phase.

**Control**: Simple tap detection — left/right screen halves for thrust/shoot. Desktop maps to keyboard keys (left/right arrows or A/D + Space).

**Testing**: JUnit 5 + MockK for pure-logic tests (physics, collision math). HeadlessApplication for screen-transition integration tests.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `build.gradle.kts` (root) | New | Project config, Kotlin/Java versions, plugin declarations |
| `settings.gradle.kts` | New | Module includes |
| `core/` | New | All game code: screens, entities, systems, assets |
| `desktop/` | New | LWJGL3 launcher |
| `android/` | New | Shell module (inactive v0.1) |
| `assets/` | New | Placeholder textures, font files |
| `i18n/` | New | Locale skeleton (no translations) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| No JDK/libGDX toolchain installed | High | Document setup in README; `gradle wrapper` generates self-contained build |
| libGDX community decline | Low | Mature framework, no dependency on active community; LTS releases suffice for v0.1 |
| Shader-based visuals deferred to later phase | Medium | v0.1 uses SpriteBatch only — acceptable for MVP, no architectural debt |
| Android module without testing | Medium | Desktop-first validation; Android module is wired but not tested until mobile phase |

## Rollback Plan

Project is greenfield with no production code. Rollback = delete generated files and Gradle wrapper. OpenSpec artifacts (config, proposal) remain as reference.

## Dependencies

- JDK 17+ (documented in README)
- Gradle 8.x (wrapper generated during setup)
- libGDX 1.12.x / KTX 1.12.x (resolved via Maven coordinates)

## Success Criteria

- [ ] `./gradlew :desktop:run` launches a resizable window with main menu
- [ ] "Start" transitions to GameScreen; ship visible with gravity pull
- [ ] Left tap / A-key applies thrust; right tap / Space shoots bullet
- [ ] Asteroids spawn in waves, move linearly, degrade on hit, break into smaller sizes
- [ ] Ship-asteroid collision removes a life; 0 lives → Game Over screen
- [ ] Score increments over time and on asteroid destruction
- [ ] Game Over screen shows score, "Restart" returns to main menu
- [ ] `./gradlew build` passes without errors
