# Audio Specification

## Purpose

AudioManager provides background music (2 tracks) and sound effects (11 SFX). Event-driven playback via GameEvent. Independent music/SFX volume controls persisted in Preferences.

## Requirements

### REQ-AUDIO-001: AudioManager Lifecycle — P0

`AudioManager` MUST load all audio assets on init. MUST expose `playSfx(name)`, `playBgm(name)`, `stopBgm()`, `setMusicVolume(0..1)`, `setSfxVolume(0..1)`. MUST handle Android lifecycle (pause/resume stops and restarts BGM).

#### Scenario: Audio assets loaded on init

- GIVEN AudioManager initialized
- WHEN any SFX or BGM name requested
- THEN audio plays without null/missing errors

#### Scenario: Android pause/resume

- GIVEN BGM playing on GameScreen
- WHEN app paused then resumed
- THEN BGM resumes from start (or paused position)

---

### REQ-AUDIO-002: Background Music — P0

Two BGM tracks MUST exist:

| Track | Screen | Behavior |
|-------|--------|----------|
| Menu theme | MenuScreen, GameOverScreen | Loop, calm/spacey |
| Game theme | GameScreen | Loop, epic/intense |

BGM MUST auto-play on screen entry and stop on screen exit. Default volume: 0.5.

#### Scenario: Menu theme plays on menu

- GIVEN MenuScreen shown
- WHEN screen enters show()
- THEN menu theme BGM starts looping

#### Scenario: Game theme plays during gameplay

- GIVEN GameScreen shown
- WHEN screen enters show()
- THEN game theme BGM starts looping (menu theme stopped)

#### Scenario: BGM stops on screen exit

- GIVEN GameScreen active with game theme
- WHEN screen transitions to GameOverScreen
- THEN game theme stops, menu theme starts

---

### REQ-AUDIO-003: Sound Effects — P0

Eleven SFX MUST be defined:

| SFX Name | Trigger | Notes |
|----------|---------|-------|
| laser_shoot | Laser fired | Short, high-pitched |
| explosion_small | Asteroid destroyed | |
| explosion_medium | Enemy destroyed (fighter/frigate) | |
| explosion_large | Enemy destroyed (destroyer/clone) | |
| explosion_ship | Ship destroyed | Game over |
| thrust | Ship thrusting | Loops while thrusting |
| rescue | Astronaut rescued | Positive chime |
| debris_collect | Debris collected | Positive ding |
| damage | Ship takes hit | Impact sound |
| game_over | Game over screen | Sad descending tone |
| new_record | High score beaten | Triumphant fanfare |

Default SFX volume: 0.7.

#### Scenario: Explosion SFX by enemy type

- GIVEN LightFighter destroyed
- WHEN EnemyDestroyed event fires
- THEN explosion_small plays

- GIVEN HeavyDestroyer destroyed
- WHEN EnemyDestroyed event fires
- THEN explosion_large plays

#### Scenario: Thrust loops while thrusting

- GIVEN ship thrusting
- WHEN thrust input held
- THEN thrust SFX loops continuously

- GIVEN thrust SFX playing
- WHEN thrust input released
- THEN thrust SFX stops

#### Scenario: New record fanfare

- GIVEN player score > high score
- WHEN GameOverScreen shown
- THEN new_record SFX plays

---

### REQ-AUDIO-004: Volume Controls — P0

Music volume and SFX volume MUST be independently controllable (0.0 to 1.0). Values MUST persist in `Gdx.app.getPreferences("nebula-drift")`. Volume = 0.0 means muted.

#### Scenario: Volume persisted across sessions

- GIVEN music volume set to 0.3
- WHEN app restarted
- THEN music volume loaded as 0.3 from preferences

#### Scenario: Mute via zero volume

- GIVEN SFX volume set to 0.0
- WHEN any SFX triggered
- THEN no audible sound (SFX still "plays" but at zero gain)

---

### REQ-AUDIO-005: Event-Driven Playback — P0

AudioManager MUST subscribe to GameEvent stream and auto-play corresponding SFX:

| GameEvent | SFX |
|-----------|-----|
| LaserFired | laser_shoot |
| AsteroidDestroyed | explosion_small |
| EnemyDestroyed | explosion by type (small/medium/large) |
| ShipDestroyed | explosion_ship, then game_over |
| AstronautRescued | rescue |
| DebrisCollected | debris_collect |
| ShipDamaged | damage |
| ShipThrusting | thrust (loop/stop) |

#### Scenario: ShipDestroyed plays two SFX

- GIVEN ship destroyed
- WHEN ShipDestroyed event fires
- THEN explosion_ship plays, followed by game_over

---

## Coverage Summary

| Requirement | Priority | Scenarios |
|-------------|----------|-----------|
| REQ-AUDIO-001 Lifecycle | P0 | 2 |
| REQ-AUDIO-002 BGM | P0 | 3 |
| REQ-AUDIO-003 SFX | P0 | 3 |
| REQ-AUDIO-004 Volume | P0 | 2 |
| REQ-AUDIO-005 Events | P0 | 1 |
| **Total** | | **11** |
