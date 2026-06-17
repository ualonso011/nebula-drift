# Leaderboard Screen Specification

## Purpose

Leaderboard screen displays top 10 local scores with predefined name selection. Data persisted as JSON in Preferences.

## Requirements

### REQ-INFRA-006: Leaderboard Data Model — P0

LeaderboardEntry MUST contain: name (String), score (Int), time (Float), date (String). Entries MUST be sorted by score descending, then time ascending for ties. Maximum 10 entries stored.

#### Scenario: Sort by score descending

- GIVEN entries: [{score: 500}, {score: 1000}, {score: 300}]
- WHEN sorted
- THEN order: [1000, 500, 300]

#### Scenario: Tie-break by time ascending

- GIVEN entries: [{score: 500, time: 120.5}, {score: 500, time: 90.2}]
- WHEN sorted
- THEN order: [time=90.2, time=120.5]

---

### REQ-INFRA-007: Leaderboard Persistence — P0

Leaderboard MUST serialize List<LeaderboardEntry> to JSON and store in Preferences key "leaderboard". Load MUST deserialize JSON back to List<LeaderboardEntry>. Invalid/missing data MUST return empty list.

#### Scenario: Save and load round-trip

- GIVEN 3 entries saved
- WHEN leaderboard loaded
- THEN same 3 entries returned with correct fields

#### Scenario: Missing data returns empty

- GIVEN no "leaderboard" preference key
- WHEN load called
- THEN empty list returned

---

### REQ-INFRA-008: Leaderboard Screen Layout — P0

LeaderboardScreen MUST display: localized title, up to 10 entries (position, name, score, time in M:SS), top 3 highlighted (gold #1, silver #2, bronze #3), and back button.

#### Scenario: Display top 3 with colors

- GIVEN 5 entries in leaderboard
- WHEN rendered
- THEN position 1 = gold highlight, 2 = silver, 3 = bronze, 4-5 = default

#### Scenario: Empty leaderboard

- GIVEN no saved entries
- WHEN LeaderboardScreen shown
- THEN title displayed, no entries shown, back button visible

---

### REQ-INFRA-009: Name Entry Flow — P0

When a new high score qualifies for top 10, GameOverScreen MUST show 6 predefined names (Pilot, Ace, Nova, Stryker, Vega, Orion). Player taps one name. Score MUST be saved with selected name. If score doesn't qualify, skip name entry.

#### Scenario: New record triggers name selection

- GIVEN player score qualifies for top 10
- WHEN GameOverScreen shown
- THEN 6 name buttons displayed

#### Scenario: Name selected saves entry

- GIVEN name selection shown
- WHEN "Nova" tapped
- THEN LeaderboardEntry created with name="Nova", score=current score
- AND entry inserted into leaderboard (sorted, max 10)

#### Scenario: Non-qualifying score skips

- GIVEN player score does NOT qualify for top 10
- WHEN GameOverScreen shown
- THEN no name selection displayed

---

### REQ-INFRA-010: Leaderboard Access — P0

MenuScreen MUST display "Leaderboard" button (localized). GameOverScreen MUST display "Leaderboard" button when new record achieved. Both MUST transition via fade.

#### Scenario: Menu to leaderboard

- GIVEN MenuScreen active
- WHEN "Leaderboard" tapped
- THEN fade transition to LeaderboardScreen

#### Scenario: Game over to leaderboard

- GIVEN GameOverScreen with new record
- WHEN "Leaderboard" tapped
- THEN fade transition to LeaderboardScreen

---

## Coverage Summary

| Requirement | Priority | Scenarios |
|-------------|----------|-----------|
| REQ-INFRA-006 Data Model | P0 | 2 |
| REQ-INFRA-007 Persistence | P0 | 2 |
| REQ-INFRA-008 Screen Layout | P0 | 2 |
| REQ-INFRA-009 Name Entry | P0 | 3 |
| REQ-INFRA-010 Access | P0 | 2 |
| **Total** | | **11** |
