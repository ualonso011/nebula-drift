# Screen Transitions Specification

## Purpose

Fade-to-black transitions between all screen switches. Provides visual polish and masks loading. Total transition time: ~1 second (0.5s fade-out + 0.5s fade-in).

## Requirements

### REQ-TRANS-001: Fade Transition Wrapper — P0

`ScreenTransition` MUST wrap screen switches with fade-to-black effect. Fade-out: current screen darkens to black over 0.5s. Fade-in: new screen brightens from black over 0.5s. Implementation SHOULD use a full-screen black rectangle with animated alpha (no FrameBuffer capture needed).

#### Scenario: Fade out to black

- GIVEN screen A active
- WHEN transition triggered
- THEN black overlay alpha goes from 0.0 → 1.0 over 0.5s

#### Scenario: Fade in from black

- GIVEN screen B just activated, black overlay at alpha 1.0
- WHEN fade-in begins
- THEN black overlay alpha goes from 1.0 → 0.0 over 0.5s

#### Scenario: Full transition timing

- GIVEN transition triggered
- WHEN 1.0 seconds elapse
- THEN transition complete, new screen fully visible, no black overlay

---

### REQ-TRANS-002: Applied Transitions — P0

Fade transition MUST apply to ALL screen switches:

| From | To |
|------|----|
| MenuScreen | GameScreen |
| GameScreen | GameOverScreen |
| GameOverScreen | MenuScreen |
| GameOverScreen | GameScreen (retry) |
| MenuScreen | SettingsScreen |
| SettingsScreen | MenuScreen |
| MenuScreen | LeaderboardScreen |
| LeaderboardScreen | MenuScreen |
| GameOverScreen | LeaderboardScreen |
| LeaderboardScreen | GameOverScreen |

#### Scenario: Menu to Game transition

- GIVEN MenuScreen active, player taps Play
- WHEN transition starts
- THEN fade-to-black → GameScreen loads → fade-from-black

#### Scenario: Game Over retry transition

- GIVEN GameOverScreen active, player taps Retry
- WHEN transition starts
- THEN fade-to-black → GameScreen loads (fresh state) → fade-from-black

#### Scenario: Game Over to Menu transition

- GIVEN GameOverScreen active, player taps Main Menu
- WHEN transition starts
- THEN fade-to-black → MenuScreen loads → fade-from-black

#### Scenario: Menu to Settings transition

- GIVEN MenuScreen active, player taps Settings
- WHEN transition starts
- THEN fade-to-black → SettingsScreen loads → fade-from-black

#### Scenario: Settings to Menu transition

- GIVEN SettingsScreen active, player taps Back
- WHEN transition starts
- THEN fade-to-black → MenuScreen loads → fade-from-black

#### Scenario: Menu to Leaderboard transition

- GIVEN MenuScreen active, player taps Leaderboard
- WHEN transition starts
- THEN fade-to-black → LeaderboardScreen loads → fade-from-black

#### Scenario: GameOver to Leaderboard transition

- GIVEN GameOverScreen active, player taps Leaderboard
- WHEN transition starts
- THEN fade-to-black → LeaderboardScreen loads → fade-from-black

---

### REQ-TRANS-003: Input Blocking During Transition — P1

User input MUST be ignored during active transition. Prevents double-taps or accidental actions during screen switch.

#### Scenario: Input blocked during fade

- GIVEN transition in progress (fade-out or fade-in)
- WHEN user taps screen
- THEN tap is ignored (no action triggered)

#### Scenario: Input restored after transition

- GIVEN transition complete
- WHEN user taps screen
- THEN tap is processed normally by active screen

---

## Coverage Summary

| Requirement | Priority | Scenarios |
|-------------|----------|-----------|
| REQ-TRANS-001 Fade Wrapper | P0 | 3 |
| REQ-TRANS-002 Applied To | P0 | 7 |
| REQ-TRANS-003 Input Block | P1 | 2 |
| **Total** | | **12** |
