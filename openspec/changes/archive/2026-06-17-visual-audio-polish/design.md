# Design: Visual & Audio Polish (v0.3.0)

## Technical Approach

Extract all rendering from `GameScreen.render()` (~494 lines) into a dedicated `GameRenderer` that uses `SpriteBatch` + procedurally generated `TextureAtlas` via `Pixmap`. Add three new subsystems — `ParticleManager`, `AudioManager`, `ScreenTransition` — all driven by the existing `GameEvent` sealed class (7 event types). Synchronous asset loading at game init. Three chained PRs with hard boundaries.

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|----------|----------|----------|--------|-----------|
| Sprite generation | Runtime Pixmap | Pre-baked PNGs in atlas | **A** | Zero external art deps. Matches existing `createBackgroundTexture()` pattern in GameScreen. CC0-only scope. |
| AudioManager shape | Singleton object | Constructor-injected per screen | **A** | Single audio context across MenuScreen/GameScreen/GameOverScreen. Matches `I18nManager` pattern. Volume prefs are global. |
| Particle ownership | GameScreen field | Separate ParticleScreen | **A** | Particles are frame-scoped to gameplay. ParticleManager reads GameEvent list — same pattern as ScoreSystem. |
| Atlas strategy | Single TextureAtlas | Individual Textures per sprite | **Single TextureAtlas** | One bind per frame. libGDX TextureAtlas supports Pixmap-packed regions. Simpler disposal. |
| Screen transition | Wrapper around setScreen | Inline fade in each screen | **Wrapper (FadeTransition)** | Single implementation. Works for Menu→Game, Game→GameOver, GameOver→Menu. Opaque to screens. |
| Debug fallback | ShapeRenderer toggle in GameRenderer | Remove ShapeRenderer entirely | **Toggle** | Keep debug hitbox overlay. Zero ShapeRenderer in normal mode per spec REQ-VISUAL-001. |
| Parallax background | 2-layer procedural Texture | 3-layer with shader | **2-layer procedural** | Out-of-scope: shaders (v0.4.0). Two Pixmap textures (far stars, near nebula) scrolling at different speeds. |

## Data Flow

```
GameScreen.render(delta)
    │
    ├── Systems update (unchanged order)
    │   Physics → Spawn → EnemySpawn → AstronautSpawn → DebrisSpawn → Collision → Score
    │
    ├── Event dispatch (NEW)
    │   context.events → ParticleManager.onGameEvent()
    │                  → AudioManager.onGameEvent()
    │   context.events.clear()
    │
    └── Render pipeline (NEW)
        GameRenderer.render(context, stateTime)
            ├── spriteBatch.begin()
            │   ├── renderBackground(parallaxOffset)
            │   ├── renderDebris(context)
            │   ├── renderAstronauts(context)
            │   ├── renderAsteroids(context)
            │   ├── renderEnemies(context)
            │   ├── renderShip(context, stateTime)  ← thrust anim
            │   ├── renderLasers(context)
            │   └── renderParticles()
            └── spriteBatch.end()
        HudRenderer.render(...)  ← unchanged
        ScreenTransition.render()  ← overlay if active
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `rendering/GameRenderer.kt` | Create | Owns SpriteBatch + TextureAtlas. 8 private render methods per entity type. Debug toggle for ShapeRenderer hitboxes. |
| `rendering/SpriteGenerator.kt` | Create | Object with functions returning Pixmap per sprite key. Generates all sprites at init. Packs into TextureAtlas. |
| `rendering/ParticleManager.kt` | Create | Pool-backed ParticleEffects. 5 templates (explosion small/med/large, engine trail, rescue sparkle). `onGameEvent()` hook. |
| `managers/AudioManager.kt` | Create | Singleton object. Loads SFX (10) + BGM (2). `onGameEvent()` hook. Volume persistence via Gdx.preferences. |
| `rendering/ScreenTransition.kt` | Create | FadeTransition class with FADE_OUT/SWITCH/FADE_IN/DONE phases. Renders fullscreen rect with alpha. |
| `rendering/ParallaxBackground.kt` | Create | 2-layer scrolling background. Far stars (slow), near nebula (fast). Procedural Pixmap textures. |
| `screens/GameScreen.kt` | Modify | Remove all ShapeRenderer rendering. Delegate to GameRenderer. Add ParticleManager, stateTime, event dispatch. |
| `screens/MenuScreen.kt` | Modify | Wrap `game.setScreen<GameScreen>()` in FadeTransition. Start BGM on show. |
| `screens/GameOverScreen.kt` | Modify | Wrap transitions. Play game_over/new_record SFX. |
| `NebulaDriftGame.kt` | Modify | Init AudioManager + SpriteGenerator in `create()`. Pass atlas to screens. |
| `util/Constants.kt` | Modify | Add ~40 constants: particle counts, audio volumes, animation frame durations, transition duration, parallax speeds. |
| `test/.../SpriteGeneratorTest.kt` | Create | Verify Pixmap dimensions, atlas key coverage, damage state sprite count. |
| `test/.../ParticleManagerTest.kt` | Create | Test spawn/recycle, pool limits, onGameEvent dispatch, update removes completed. |
| `test/.../AudioManagerTest.kt` | Create | Test playSound/playMusic, volume persistence, onGameEvent mapping. |
| `test/.../ScreenTransitionTest.kt` | Create | Test phase progression (FADE_OUT→SWITCH→FADE_IN→DONE), alpha values, signal timing. |

## Interfaces / Contracts

### GameRenderer

```kotlin
class GameRenderer(
    private val spriteBatch: SpriteBatch,
    private val textureAtlas: TextureAtlas,
    private val debug: Boolean = false
) {
    private val shapeRenderer = ShapeRenderer()  // debug only
    private val animationManager = AnimationManager(textureAtlas)

    fun render(context: GameContext, stateTime: Float) {
        spriteBatch.projectionMatrix = camera.combined
        spriteBatch.begin()
        renderBackground(context)
        renderDebris(context)
        renderAstronauts(context)
        renderAsteroids(context)
        renderEnemies(context)
        renderShip(context, stateTime)
        renderLasers(context)
        spriteBatch.end()
        // Particles render in same batch (after entities)
        spriteBatch.begin()
        particleManager.render(spriteBatch)
        spriteBatch.end()
        if (debug) renderDebugHitboxes(context)
    }
}
```

### Sprite Key Convention

```
ship_pristine, ship_damaged, ship_critical
asteroid_{large|medium|small}_{3|2|1}  (size + HP)
enemy_fighter, enemy_frigate_{1|2}, enemy_destroyer_{1|2|3}, enemy_clone
astro_floating, astro_rescued, astro_dead
debris, debris_glow
laser, laser_glow
explosion_{small|medium|large}_0..5  (animation frames)
thrust_0, thrust_1  (2-frame flicker)
```

### ParticleManager

```kotlin
class ParticleManager {
    fun init(atlas: TextureAtlas)
    fun onGameEvent(event: GameEvent)  // auto-spawn by event type
    fun spawnEngineTrail(position: Vector2, isThrusting: Boolean)
    fun update(delta: Float)
    fun render(spriteBatch: SpriteBatch)
    fun dispose()
}
```

### AudioManager

```kotlin
object AudioManager {
    fun init()
    fun onGameEvent(event: GameEvent)
    fun playMusic(track: String)
    fun playSound(name: String)
    fun setMusicVolume(volume: Float)
    fun setSfxVolume(volume: Float)
    fun pause()
    fun resume()
    fun dispose()
}
```

### ScreenTransition

```kotlin
class FadeTransition(private val duration: Float = 0.5f) {
    enum class Phase { FADE_OUT, SWITCH, FADE_IN, DONE }
    fun update(delta: Float): Boolean  // returns true on SWITCH phase
    fun render(spriteBatch: SpriteBatch, camera: OrthographicCamera)
    val isComplete: Boolean
}
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | SpriteGenerator pixmap sizes | Generate each sprite, assert width/height > 0 and correct dimensions |
| Unit | ParticleManager pool logic | Spawn N effects, verify pool depletion at max (300), verify completed effects recycled |
| Unit | AudioManager volume/prefs | Set volume, verify persistence via mock Preferences |
| Unit | ScreenTransition phases | Drive update() through all 4 phases, verify alpha values and switch signal |
| Unit | AnimationManager frame indexing | Verify keyFrame returns correct region at given stateTime |
| Integration | GameEvent → particles + audio | Emit each of 7 event types, verify correct particle/sound triggered |
| Manual | Visual quality, audio mixing, FPS | Play full session, verify no ShapeRenderer in normal mode, FPS > 55 with 300 particles |

## Migration / Rollout

No migration. Three chained PRs, each independently revertable:

1. **PR #1**: SpriteGenerator + GameRenderer + TextureAtlas. GameScreen delegates rendering. ShapeRenderer kept behind debug toggle.
2. **PR #2**: ParticleManager + AudioManager + screen shake. Event-driven hookup in GameScreen.render().
3. **PR #3**: FadeTransition + ParallaxBackground + animations (thrust flicker, explosion frames).

## Open Questions

- [ ] **Audio asset sourcing**: bfxr for SFX is confirmed. For BGM, use OpenGameArt CC0 or generate silence as placeholder? *Default: CC0 placeholder, replace later.*
- [ ] **Screen shake intensity**: Proposal mentions ~30 lines. Use camera translation jitter (2-3px, 0.2s decay) on ShipHit only, or also on explosions? *Default: ShipHit + large explosions.*
- [ ] **Particle max on Android**: 300 desktop, but configurable? *Default: hardcode 300, add config in v0.4.0 if needed.*
