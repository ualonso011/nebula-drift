# Tasks: Visual & Audio Polish (v0.3.0)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1100–1300 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-always |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Sprite infra + GameRenderer | PR 1 → main | Tests included. Zero ShapeRenderer in normal mode. |
| 2 | Particles + Audio + event hooks | PR 2 → main | Depends on PR 1 atlas. Adds 7 event hooks. |
| 3 | Animations + Transitions + Parallax | PR 3 → main | Depends on PR 1 rendering infra. Final polish. |

## Phase 1: Sprite Infrastructure (PR 1)

- [x] 1.1 Create `rendering/SpriteGenerator.kt` — Pixmap generator for all sprite keys (ship 3 states, asteroid 3×3, enemy 4×states, astronaut 3, debris 2, laser 2)
- [x] 1.2 Add ship thrust (2-frame), explosion (6-frame) pixmaps to SpriteGenerator
- [x] 1.3 Create `rendering/GameRenderer.kt` — owns SpriteBatch + TextureAtlas, 8 render methods per entity type, debug ShapeRenderer toggle
- [x] 1.4 Update `screens/GameScreen.kt` — remove inline ShapeRenderer rendering, delegate to GameRenderer, pass GameContext + stateTime
- [x] 1.5 Update `NebulaDriftGame.kt` — init SpriteGenerator atlas in create(), pass to GameScreen
- [x] 1.6 Add ~15 sprite constants to `util/Constants.kt` (pixmap sizes, atlas keys)
- [x] 1.7 Write `SpriteGeneratorTest` — verify pixmap dimensions > 0, atlas key coverage for all entity types
- [x] 1.8 Verify: all entities render as sprites, debug toggle shows hitboxes, FPS stable

## Phase 2: Particles & Audio (PR 2)

- [x] 2.1 Create `rendering/ParticleManager.kt` — pool-backed, 5 effect templates (explosion, engine trail, rescue sparkle, debris, damage sparks), 300 max, spawn/update/render
- [x] 2.2 Create `managers/AudioManager.kt` — singleton object, load 10 SFX + 2 BGM procedurally or as placeholders, volume persistence via Gdx.preferences, pause/resume
- [x] 2.3 Add event types if needed (ShipDestroyed, LaserFired) to GameEvent sealed class
- [x] 2.4 Add ~15 particle + audio constants to Constants.kt (counts, durations, volumes, file names)
- [x] 2.5 Hook ParticleManager + AudioManager into GameScreen — dispatch events after system updates, before render
- [x] 2.6 Update MenuScreen/GameOverScreen — start/stop BGM on show/hide, play game_over/new_record SFX
- [x] 2.7 Write `ParticleManagerTest` — spawn up to pool limit, verify recycling on completion, event → template mapping
- [x] 2.8 Write `AudioManagerTest` — play sound, set/persist volume, pause/resume lifecycle
- [x] 2.9 Verified: build + test passes (245 tests, 0 failures). Particles spawn on all 7+ event types. Audio plays gracefully when assets exist.

## Phase 3: Animations, Transitions & Polish (PR 3)

- [x] 3.1 Implement AnimationManager in GameRenderer — thrust 2-frame flicker at ~8 FPS, explosion 6-frame once
- [x] 3.2 Implement astronaut animations in GameRenderer — rescue sine-wave Y-bob + rise, death fall + fade
- [x] 3.3 Implement asteroid rotation rendering in GameRenderer — apply entity rotation via SpriteBatch
- [x] 3.4 Create `rendering/ScreenTransition.kt` — FadeTransition with FADE_OUT/FADE_IN/DONE phases (SWITCH is instantaneous), fullscreen black rect overlay
- [x] 3.5 Create `rendering/ParallaxBackground.kt` — 2-layer procedural Pixmap (stars + nebula), different scroll speeds
- [x] 3.6 Wrap screen switches in MenuScreen/GameOverScreen — use FadeTransition for Menu→Game, Game→GameOver, GameOver→Menu, GameOver→Game
- [x] 3.7 Add ~10 transition + parallax + animation constants to Constants.kt
- [x] 3.8 Write `ScreenTransitionTest` — 12 tests: phase progression timing, alpha values, isComplete signal, reset, switch count
- [x] 3.9 Verify: build + test passes (257 tests, 0 failures). Desktop module compiles. All rendering changes compile-tested.
