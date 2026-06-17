# Proposal: Infrastructure v0.4.0

## Intent

Add settings (volume, language), local leaderboard, Android launcher fix, and version system. Enables user configuration, score persistence, and correct mobile deployment.

## Scope

### In Scope
- Settings screen: music/SFX sliders, language toggle, version display
- Leaderboard screen: top 10, JSON storage, predefined names, gold/silver/bronze highlights
- Android launcher: portrait→sensorLandscape, versionName 0.4.0, wake lock, immersive mode
- Version system: GAME_VERSION in Constants.kt, CHANGELOG.md (v0.1.0–v0.4.0)
- Language switching: I18nManager.setLocale() + screen recreation via transitions
- UiComponents utility: drawButton, drawSlider helpers (custom rendering)

### Out of Scope
- On-screen keyboard (predefined names only)
- Online leaderboard, achievements, missions
- Glow/bloom shaders (v0.5.0+)
- Real art or audio assets (procedural sprites + silent placeholders)

## Capabilities

### New Capabilities
- `settings-screen`: Volume sliders, language toggle, version display, back button
- `leaderboard-screen`: Top 10 JSON persistence, predefined names, top 3 highlights
- `android-launcher`: Orientation fix, versionName, wake lock, immersive mode flags
- `version-system`: GAME_VERSION constant in Constants.kt, CHANGELOG.md structure

### Modified Capabilities
- `audio`: Add real-time volume slider → AudioManager integration requirement
- `screen-transitions`: Add Settings↔Menu, Leaderboard↔Menu, GameOver→Leaderboard, language-switch recreation routes
- `game`: Add Settings/Leaderboard buttons to MenuScreen, View Leaderboard to GameOverScreen; I18nManager.setLocale() spec

## Approach

Custom rendering (ShapeRenderer + SpriteBatch + BitmapFont) for all new screens — consistent with existing codebase. JSON via libGDX Json serializer for leaderboard persistence. Predefined name pool avoids text input complexity. Language switch via setLocale() + screen recreation through existing transition system. Lightweight UiComponents extracted from existing button rendering patterns.

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Slider touch handling bugs | Med | Full world-coordinate bounds testing |
| Language switch visual glitch | Low | Existing transition covers screen recreation |
| Leaderboard JSON schema drift | Low | Include version field in JSON for migration |
| No Android SDK in environment | High | Document requirement; compile-only verification |

## Rollback Plan

Revert android/build.gradle.kts versionName. Delete SettingsScreen + LeaderboardScreen. Remove new navigation buttons from MenuScreen/GameOverScreen. Keep Constants.GAME_VERSION and CHANGELOG.md (non-breaking additions).

## Dependencies

- libGDX Json serializer (already in classpath via core)
- Android SDK (user-provided, not available in environment)

## Success Criteria

- [ ] Volume sliders persist across sessions and update AudioManager in real-time
- [ ] Leaderboard top 10 survives JSON serialize→deserialize round-trip
- [ ] Language switch changes all UI text via setLocale + screen recreation
- [ ] Android manifest: sensorLandscape, versionName 0.4.0
- [ ] GAME_VERSION = "0.4.0" accessible from Constants.kt
- [ ] CHANGELOG.md documents v0.1.0 through v0.4.0
- [ ] All 257 existing tests pass unchanged
