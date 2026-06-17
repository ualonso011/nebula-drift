# Settings Screen Specification

## Purpose

Settings screen provides volume controls, language toggle, and version display. All UI uses custom rendering (ShapeRenderer + SpriteBatch + BitmapFont).

## Requirements

### REQ-INFRA-001: Settings Screen Layout — P0

The system MUST display a SettingsScreen with: music volume slider, SFX volume slider, language toggle (3 options), version label, and back button. All elements MUST be vertically centered and use localized labels.

#### Scenario: Settings screen renders all elements

- GIVEN SettingsScreen shown
- WHEN screen renders
- THEN music slider, SFX slider, language toggle, version text ("v0.4.0"), and back button are visible

#### Scenario: Localized labels

- GIVEN locale = "eu"
- WHEN SettingsScreen renders
- THEN labels display in Euskera ("Musika", "Soinuak", "Hizkuntza", "Atzera")

---

### REQ-INFRA-002: Volume Sliders — P0

Each slider MUST render a horizontal bar with draggable thumb. Dragging MUST update value (0.0–1.0). Value changes MUST call AudioManager.setMusicVolume/setSfxVolume immediately. Values MUST persist in Preferences.

#### Scenario: Drag slider to change volume

- GIVEN music slider at 0.5
- WHEN user drags thumb to 0.8 position
- THEN AudioManager.setMusicVolume(0.8) called immediately
- AND preference "music_volume" saved as 0.8

#### Scenario: Slider bounds clamped

- GIVEN slider at 0.0
- WHEN user drags left of bar start
- THEN value remains 0.0

---

### REQ-INFRA-003: Language Toggle — P0

Language toggle MUST cycle through eu → es → en → eu on tap. On change, the system MUST call I18nManager.setLocale(newLocale) then transition to MenuScreen (recreated with new locale).

#### Scenario: Cycle language

- GIVEN current locale = "eu"
- WHEN language toggle tapped
- THEN locale changes to "es", I18nManager.setLocale("es") called
- AND transition to MenuScreen begins

#### Scenario: Full cycle

- GIVEN locale = "en"
- WHEN language toggle tapped
- THEN locale changes to "eu"

---

### REQ-INFRA-004: Settings Access — P0

MenuScreen MUST display a "Settings" button (localized). Tapping MUST transition to SettingsScreen via fade transition.

#### Scenario: Navigate to settings

- GIVEN MenuScreen active
- WHEN "Settings" button tapped
- THEN fade transition to SettingsScreen

---

### REQ-INFRA-005: UiComponents Utility — P1

UiComponents MUST provide reusable static methods: drawButton(renderer, batch, font, bounds, label) and drawSlider(renderer, batch, font, bounds, value). Used by SettingsScreen, LeaderboardScreen, and existing screens.

#### Scenario: drawButton renders rect + border + text

- GIVEN bounds rectangle and label "Play"
- WHEN drawButton called
- THEN filled rect, 2px border, centered text rendered

#### Scenario: drawSlider renders bar + thumb

- GIVEN bounds and value 0.6
- WHEN drawSlider called
- THEN background bar, filled portion (60%), and thumb circle rendered

---

## Coverage Summary

| Requirement | Priority | Scenarios |
|-------------|----------|-----------|
| REQ-INFRA-001 Layout | P0 | 2 |
| REQ-INFRA-002 Volume Sliders | P0 | 2 |
| REQ-INFRA-003 Language Toggle | P0 | 2 |
| REQ-INFRA-004 Settings Access | P0 | 1 |
| REQ-INFRA-005 UiComponents | P1 | 2 |
| **Total** | | **9** |
