# Version System Specification

## Purpose

Single source of truth for game version, used across desktop and Android builds.

## Requirements

### REQ-INFRA-014: GAME_VERSION Constant — P0

Constants.kt MUST define `const val GAME_VERSION = "0.4.0"`. Used by SettingsScreen display and Android versionName.

#### Scenario: Version constant accessible

- GIVEN Constants object
- WHEN GAME_VERSION accessed
- THEN returns "0.4.0"

---

### REQ-INFRA-015: CHANGELOG.md — P1

CHANGELOG.md MUST exist in project root. MUST document versions v0.1.0 through v0.4.0 with date and Added sections per Keep a Changelog format.

#### Scenario: Changelog contains all versions

- GIVEN CHANGELOG.md
- WHEN read
- THEN sections for [0.4.0], [0.3.0], [0.2.0], [0.1.0] present

---

## Coverage Summary

| Requirement | Priority | Scenarios |
|-------------|----------|-----------|
| REQ-INFRA-014 GAME_VERSION | P0 | 1 |
| REQ-INFRA-015 CHANGELOG | P1 | 1 |
| **Total** | | **2** |
