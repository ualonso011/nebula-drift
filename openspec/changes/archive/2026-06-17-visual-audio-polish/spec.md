# Spec: Visual & Audio Polish (v0.3.0)

## Game Rendering

### REQ-VISUAL-001: GameRenderer Extraction — P0
GameRenderer MUST own all entity rendering via SpriteBatch + procedural TextureAtlas. Zero ShapeRenderer calls in normal mode.
- GIVEN active entities → WHEN render() → THEN all drawn via SpriteBatch
- GIVEN debug toggle → WHEN render() → THEN ShapeRenderer MAY show hitboxes

### REQ-VISUAL-002: Procedural TextureAtlas — P0
All sprites generated at init via Pixmap. Atlas: ship (3 states), asteroids (3×3=9), enemies (4 types × damage states), astronaut (3 states), debris (1+glow), laser (1+glow).
- GIVEN initialized → WHEN any sprite key queried → THEN valid TextureRegion returned
- GIVEN ship at 3/2/1 lives → WHEN drawn → THEN correct damage sprite selected

### REQ-VISUAL-003: Entity Sprite Mapping — P0
Each entity maps to atlas regions by damage state. Sprite switches on HP change.
- GIVEN asteroid HP changes → WHEN hit → THEN sprite key updates (size × HP)
- GIVEN enemy HP changes → WHEN hit → THEN sprite key updates per type/state

### REQ-VISUAL-004: Parallax Background — P1
2–3 parallax layers at different scroll speeds. Procedural or static textures.
- GIVEN 3 layers scrolling → WHEN 1s elapses → THEN far=slow, near=fast

## Particle Effects

### REQ-PARTICLE-001: ParticleManager Lifecycle — P0
Pool-backed, max 300 active. Exposes spawn/update/render. Recycles expired particles.
- GIVEN 300 active → WHEN new requested → THEN recycled or skipped (no crash)
- GIVEN 50 active → WHEN update+render → THEN all 50 processed

### REQ-PARTICLE-002: Effect Templates — P0
6 effects: explosion (orange/red, 20-40), engine trail (blue/white, continuous), rescue sparkle (green/yellow, 15-25), debris sparkle (golden, 10-15), damage sparks (red/orange, 10-20), ship destruction (massive, 60-100).
- GIVEN asteroid destroyed → WHEN event fires → THEN explosion at position
- GIVEN ship thrusting → WHEN 0.5s elapses → THEN engine trail continuous
- GIVEN ship destroyed → WHEN event fires → THEN 60-100 particles

### REQ-PARTICLE-003: Event Hooks — P0
Auto-spawn on: AsteroidDestroyed, EnemyDestroyed, ShipDestroyed, AstronautRescued, DebrisCollected, ShipDamaged, ShipThrusting(continuous).
- GIVEN subscribed to GameEvent → WHEN each of 7 types fires → THEN effect spawns
- GIVEN thrusting → WHEN input released → THEN trail stops spawning

## Audio

### REQ-AUDIO-001: AudioManager Lifecycle — P0
Loads all assets on init. Exposes playSfx/playBgm/stopBgm/setVolume. Handles Android pause/resume.
- GIVEN initialized → WHEN any name requested → THEN plays without error
- GIVEN BGM playing → WHEN app paused/resumed → THEN BGM resumes

### REQ-AUDIO-002: Background Music — P0
Menu theme (MenuScreen/GameOverScreen, loop) + Game theme (GameScreen, loop). Default volume 0.5.
- GIVEN MenuScreen → WHEN show() → THEN menu theme loops
- GIVEN GameScreen → WHEN show() → THEN game theme loops (menu stopped)
- GIVEN transition → WHEN screen changes → THEN old BGM stops, new starts

### REQ-AUDIO-003: Sound Effects — P0
11 SFX: laser_shoot, explosion_small/medium/large/ship, thrust(loop), rescue, debris_collect, damage, game_over, new_record. Default volume 0.7.
- GIVEN LightFighter destroyed → WHEN event → THEN explosion_small
- GIVEN HeavyDestroyer destroyed → WHEN event → THEN explosion_large
- GIVEN thrust held → WHEN input → THEN thrust loops; released → stops
- GIVEN score > highscore → WHEN GameOverScreen → THEN new_record plays

### REQ-AUDIO-004: Volume Controls — P0
Music/SFX volume independent (0.0–1.0). Persisted in Gdx.app.getPreferences("nebula-drift"). 0.0 = muted.
- GIVEN volume=0.3 → WHEN app restarted → THEN loaded as 0.3
- GIVEN SFX volume=0.0 → WHEN triggered → THEN silent (plays at zero gain)

### REQ-AUDIO-005: Event-Driven Playback — P0
Auto-play SFX on GameEvent: LaserFired→laser_shoot, AsteroidDestroyed→explosion_small, EnemyDestroyed→by type, ShipDestroyed→explosion_ship+game_over, AstronautRescued→rescue, DebrisCollected→debris_collect, ShipDamaged→damage, ShipThrusting→thrust loop.
- GIVEN ShipDestroyed → WHEN fires → THEN explosion_ship then game_over

## Animations

### REQ-ANIM-001: Ship Thrust Flicker — P0
2-frame engine glow alternation at ~8 FPS while thrusting. No glow when idle.
- GIVEN thrusting → WHEN 0.25s elapses → THEN glow alternated at least once
- GIVEN not thrusting → WHEN render() → THEN no glow rendered

### REQ-ANIM-002: Explosion Animation — P0
4–6 frame spritesheet, play once, ~0.4–0.6s. Used for all destruction events.
- GIVEN destroyed at (x,y) → WHEN starts → THEN frames play ~0.5s then removed

### REQ-ANIM-003: Astronaut Animations — P0
RESCUED: sine Y-offset wave (0.5–1s). DEAD: move down + fade (0.5–1s). Both play once then remove.
- GIVEN rescued → WHEN state entered → THEN sine bob 0.5–1s then removed
- GIVEN killed → WHEN state entered → THEN fall+fade 0.5–1s then removed

### REQ-ANIM-004: Enemy Damage Flash — P1
Brief spark/flash overlay (~0.1s) on hit. Applies to enemies with >1 HP.
- GIVEN MediumFrigate hit → WHEN HP→1 → THEN flash ~0.1s then normal sprite

### REQ-ANIM-005: Asteroid Rotation — P0
Render with rotation field applied via SpriteBatch. Continuous per rotation speed.
- GIVEN rotation speed=30°/s → WHEN 1s elapses → THEN rotated 30°

## Screen Transitions

### REQ-TRANS-001: Fade Transition Wrapper — P0
Fade-to-black: 0.5s fade-out (alpha 0→1), 0.5s fade-in (alpha 1→0). Black rectangle overlay approach.
- GIVEN transition triggered → WHEN 0.5s → THEN black overlay at alpha 1.0
- GIVEN fade-in → WHEN 0.5s → THEN overlay at alpha 0.0, transition complete

### REQ-TRANS-002: Applied Transitions — P0
All screen switches: Menu→Game, Game→GameOver, GameOver→Menu, GameOver→Game(retry).
- GIVEN Menu + Play tapped → WHEN transition → THEN fade→Game→fade-in
- GIVEN GameOver + Retry → WHEN transition → THEN fade→Game(fresh)→fade-in
- GIVEN GameOver + Menu → WHEN transition → THEN fade→Menu→fade-in

### REQ-TRANS-003: Input Blocking During Transition — P1
Input ignored during active transition. Restored after completion.
- GIVEN transition in progress → WHEN tap → THEN ignored
- GIVEN transition complete → WHEN tap → THEN processed normally
