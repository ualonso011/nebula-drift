# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.0] - 2026-06-17

### Added
- Settings screen with volume controls (music/SFX) and language toggle
- Leaderboard screen with top 10 local scores
- Android launcher support (landscape orientation, immersive mode)
- Version system with in-game display

### Changed
- Improved UI component reusability (UiComponents utility)
- Enhanced I18nManager with runtime locale switching

## [0.3.0] - 2026-06-17

### Added
- Procedural sprite system (43+ sprites via Pixmap)
- Particle effects system (5 templates: explosions, engine trail, sparkles)
- Audio system (music + SFX with separate volume controls)
- Animation system (thrust flicker, explosions, astronaut states)
- Screen transitions (fade-to-black between screens)
- Parallax background (2-layer procedural scrolling)

## [0.2.0] - 2026-06-17

### Added
- 4 enemy types (LightFighter, MediumFrigate, HeavyDestroyer, DarkClone)
- Astronaut rescue/kill mechanics with state animations
- Space debris health recovery pickups
- Progressive difficulty system (15s safe zone, linear curves)
- MirrorSystem for DarkClone player action mirroring

### Changed
- Extended collision system with 5 new collision pairs
- Enhanced scoring with per-type enemy points and astronaut stats

## [0.1.0] - 2026-06-17

### Added
- Initial release
- Ship with gravity, thrust (tap left), shooting (tap right)
- 3 lives with visual damage states
- Asteroids (3 sizes with HP degradation)
- Score system with time-based and destruction points
- Main menu, game screen, game over screen
- HUD (lives, score, timer)
- Internationalization (Euskera default, Spanish, English)
- Desktop launcher (1280x720, 60 FPS)
- 49 unit tests
