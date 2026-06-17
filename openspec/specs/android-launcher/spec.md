# Android Launcher Specification

## Purpose

Android launcher configuration for correct mobile deployment: landscape orientation, wake lock, version alignment.

## Requirements

### REQ-INFRA-011: AndroidManifest Configuration — P0

AndroidManifest.xml MUST set screenOrientation="sensorLandscape", keepScreenOn="true". Activity MUST extend AndroidApplication.

#### Scenario: Manifest orientation

- GIVEN AndroidManifest.xml
- WHEN inspected
- THEN screenOrientation = "sensorLandscape"

#### Scenario: Screen stays on

- GIVEN game running on Android
- WHEN no touch input for 5 minutes
- THEN screen does NOT turn off

---

### REQ-INFRA-012: Android Build Versions — P0

build.gradle.kts MUST set: versionName = GAME_VERSION from Constants, versionCode = 4, minSdk = 21, targetSdk = 34.

#### Scenario: Version alignment

- GIVEN Constants.GAME_VERSION = "0.4.0"
- WHEN Android build configured
- THEN versionName = "0.4.0", versionCode = 4

---

### REQ-INFRA-013: Android Lifecycle — P0

AndroidLauncher MUST handle pause/resume via AudioManager. Touch input MUST route through GameInputProcessor. No code changes to AndroidLauncher.kt beyond manifest/build config.

#### Scenario: Audio pauses on app background

- GIVEN game playing on Android
- WHEN home button pressed
- THEN AudioManager.pause() called, BGM stops

---

## Coverage Summary

| Requirement | Priority | Scenarios |
|-------------|----------|-----------|
| REQ-INFRA-011 Manifest | P0 | 2 |
| REQ-INFRA-012 Build Versions | P0 | 1 |
| REQ-INFRA-013 Lifecycle | P0 | 1 |
| **Total** | | **4** |
