# Proposal: Visual & Audio Polish (v0.3.0)

## Intent

Replace all `ShapeRenderer` placeholders with sprite-based rendering, add particles, audio, animations, and screen transitions. The game works mechanically but looks and sounds like a prototype — this change makes it feel like a real game.

## Scope

### In Scope
- GameRenderer with SpriteBatch + procedural TextureAtlas (Pixmap)
- ParticleManager: 5 effect templates, 300 max particles, pool-backed
- AudioManager: 10 SFX + 2 BGM tracks, separate volume, event-driven
- 2-frame ship thrust flicker, 4-6 frame explosion, astronaut wave/death
- Fade-to-black screen transitions
- Screen shake on impacts (~30 lines)

### Out of Scope
- Glow/bloom shaders (v0.4.0)
- Custom GLSL shaders (v0.4.0)
- Dynamic music layers (v0.4.0)
- Settings UI for volume (code-only defaults for v0.3.0)
- Asynchronous asset loading (sync is fine for this asset count)
- Real art assets (procedural Pixmap + CC0 placeholders only)

## Capabilities

### New Capabilities
- `game-rendering`: SpriteBatch + TextureAtlas entity rendering. GameRenderer owns all draw calls. Procedural sprites via Pixmap.
- `particle-effects`: ParticleManager with pooled programmatic effects. Hooks into GameEvent for auto-spawn. 5 effect templates, 300 max.
- `audio`: AudioManager singleton. Event-driven SFX playback via GameEvent. BGM with seamless loop. Independent music/SFX volume.
- `animations`: Frame-based entity animations. Thrust flicker, explosion spritesheets, astronaut wave/death sequences.
- `screen-transitions`: Fade-to-black wrapper between KtxScreen switches.

### Modified Capabilities
None — all are new. Existing specs (game, enemy, astronaut, debris, space-debris, difficulty-system) unchanged at spec level.

## Approach

3 chained PRs: (1) Sprite infra + GameRenderer + TextureAtlas, (2) Particles + Audio + screen shake, (3) Animations + screen transitions. Procedural sprites via Pixmap. SFX via bfxr. Event-driven hookup via existing GameEvent sealed class (7 event types). Synchronous asset load at game init.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `GameScreen.kt` | Modified | Extract rendering → GameRenderer |
| `NebulaDriftGame.kt` | Modified | Inject AudioManager across screens |
| `rendering/GameRenderer.kt` | New | Owns SpriteBatch, entity draw methods |
| `rendering/ParticleManager.kt` | New | Pooled particle effects |
| `managers/AudioManager.kt` | New | Singleton, SFX/BGM, volume persistence |
| `Constants.kt` | Modified | Particle/audio/animation constants |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Scope creep on visual polish | Med | 3 chained PRs with hard boundaries |
| Android audio lifecycle | Med | AudioManager.pause/resume in show/hide |
| Particle FPS on low-end Android | Low | Configurable max particles (100/200/300) |
| ShapeRenderer dead code left behind | Low | Keep debug toggle, remove in final PR |

## Rollback Plan

Revert each chained PR independently. PR #1: delete GameRenderer, restore inline ShapeRenderer. PR #2: remove AudioManager/ParticleManager calls from GameScreen. PR #3: remove ScreenTransition wrapper. Game compiles and plays after reverting any single PR.

## Dependencies

- libGDX core only (no new extensions)
- bfxr tool for procedural SFX generation
- CC0 music tracks from OpenGameArt.org

## Success Criteria

- [ ] All entities render as sprites (zero ShapeRenderer calls in normal mode)
- [ ] Particles spawn and recycle on all 7 GameEvent types + ship thrust
- [ ] SFX play on all 7 event types; BGM loops on menu and game screens
- [ ] Screen transitions are smooth fade-to-black (~50ms)
- [ ] Ship thrust flicker visible, explosion plays on destroy events
- [ ] FPS stays above 55 on desktop with 300 active particles
- [ ] Music and SFX volumes are independently controllable
