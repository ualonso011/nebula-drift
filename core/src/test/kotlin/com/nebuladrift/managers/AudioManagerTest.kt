package com.nebuladrift.managers

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.nebuladrift.entities.Ship
import com.nebuladrift.systems.GameEvent
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [AudioManager] singleton.
 *
 * Covers:
 * - [init] runs without crashing when no audio assets exist
 * - Volume get/set with clamping
 * - Volume persistence round-trip via Gdx preferences
 * - Pause/resume lifecycle (no-op safe)
 * - [onGameEvent] handles event types without crash
 */
class AudioManagerTest {

    companion object {
        @BeforeAll @JvmStatic
        fun initGdx() {
            val config = HeadlessApplicationConfiguration()
            HeadlessApplication(object : ApplicationAdapter() {}, config)
        }
    }

    @BeforeEach
    fun setUp() {
        // Reset AudioManager internal state before each test
        val initializedField = AudioManager::class.java.getDeclaredField("initialized")
        initializedField.isAccessible = true
        initializedField.set(AudioManager, false)

        // Clear saved preferences for clean state
        val prefs = Gdx.app.getPreferences("nebula-drift")
        prefs.clear()
        prefs.flush()
    }

    @Test
    fun `init runs without crash when no assets exist`() {
        // Should not throw even though sfx/music directories are empty
        AudioManager.init()
        assertTrue(true, "Init should complete without exception")
    }

    @Test
    fun `music volume defaults to correct value`() {
        AudioManager.init()
        assertEquals(
            Constants.MUSIC_VOLUME_DEFAULT, AudioManager.musicVolume, 0.01f,
            "Default music volume should match constant"
        )
    }

    @Test
    fun `sfx volume defaults to correct value`() {
        AudioManager.init()
        assertEquals(
            Constants.SFX_VOLUME_DEFAULT, AudioManager.sfxVolume, 0.01f,
            "Default SFX volume should match constant"
        )
    }

    @Test
    fun `setMusicVolume clamps to valid range`() {
        AudioManager.init()
        AudioManager.setMusicVolume(1.5f)
        assertTrue(AudioManager.musicVolume <= 1f, "Volume should be clamped to max 1.0")
        assertTrue(AudioManager.musicVolume >= 0f, "Volume should be >= 0")

        AudioManager.setMusicVolume(-0.5f)
        assertTrue(AudioManager.musicVolume >= 0f, "Volume should be clamped to min 0.0")
    }

    @Test
    fun `setSfxVolume clamps to valid range`() {
        AudioManager.init()
        AudioManager.setSfxVolume(2.0f)
        assertTrue(AudioManager.sfxVolume <= 1f, "SFX volume should be clamped to max 1.0")

        AudioManager.setSfxVolume(-1.0f)
        assertTrue(AudioManager.sfxVolume >= 0f, "SFX volume should be clamped to min 0.0")
    }

    @Test
    fun `setMusicVolume persists to preferences`() {
        AudioManager.init()
        AudioManager.setMusicVolume(0.75f)

        // Reload from prefs by re-initialising
        val initializedField = AudioManager::class.java.getDeclaredField("initialized")
        initializedField.isAccessible = true
        initializedField.set(AudioManager, false)

        AudioManager.init()

        assertEquals(
            0.75f, AudioManager.musicVolume, 0.01f,
            "Music volume should persist across re-inits"
        )
    }

    @Test
    fun `setSfxVolume persists to preferences`() {
        AudioManager.init()
        AudioManager.setSfxVolume(0.3f)

        // Reload from prefs by re-initialising
        val initializedField = AudioManager::class.java.getDeclaredField("initialized")
        initializedField.isAccessible = true
        initializedField.set(AudioManager, false)

        AudioManager.init()

        assertEquals(
            0.3f, AudioManager.sfxVolume, 0.01f,
            "SFX volume should persist across re-inits"
        )
    }

    @Test
    fun `pause and resume are safe when no music loaded`() {
        AudioManager.init()
        AudioManager.pause()
        AudioManager.resume()
        assertTrue(true, "Pause/resume should be safe with no audio assets")
    }

    @Test
    fun `dispose is safe when no assets loaded`() {
        AudioManager.init()
        AudioManager.dispose()
        assertTrue(true, "Dispose should be safe with no audio assets")
    }

    @Test
    fun `playSound is safe when sound not loaded`() {
        AudioManager.init()
        AudioManager.playSound("nonexistent_sound")
        assertTrue(true, "playSound should be safe for missing sounds")
    }

    @Test
    fun `playMusic is safe when track not found`() {
        AudioManager.init()
        AudioManager.playMusic("nonexistent_track.mp3")
        assertTrue(true, "playMusic should be safe for missing tracks")
    }

    @Test
    fun `onGameEvent handles empty list without crash`() {
        AudioManager.init()
        AudioManager.onGameEvent(emptyList())
        assertTrue(true, "onGameEvent with empty list should be safe")
    }

    @Test
    fun `init is idempotent`() {
        AudioManager.init()
        AudioManager.setMusicVolume(0.9f)
        AudioManager.init() // second call should be no-op
        assertEquals(
            0.9f, AudioManager.musicVolume, 0.01f,
            "Second init should not reset state"
        )
    }

    @Test
    fun `playSound is safe for all SFX constants without assets`() {
        AudioManager.init()
        val sfxConstants = listOf(
            Constants.SFX_LASER,
            Constants.SFX_EXPLOSION_SMALL,
            Constants.SFX_EXPLOSION_LARGE,
            Constants.SFX_RESCUE,
            Constants.SFX_DAMAGE,
            Constants.SFX_GAME_OVER,
            Constants.SFX_NEW_RECORD
        )
        for (sfx in sfxConstants) {
            AudioManager.playSound(sfx)
        }
        assertTrue(true, "All SFX play calls should be safe without assets")
    }

    @Test
    fun `startMusic and stopMusic are safe without assets`() {
        AudioManager.init()
        AudioManager.startMusic()
        AudioManager.stopMusic()
        assertTrue(true, "Music lifecycle should be safe with no assets")
    }

    @Test
    fun `onGameEvent handles all common event types`() {
        AudioManager.init()
        val ship = Ship()
        val events = listOf(
            GameEvent.LaserFired(com.nebuladrift.entities.Laser(position = com.badlogic.gdx.math.Vector2.Zero)),
            GameEvent.AsteroidDestroyed(
                com.nebuladrift.entities.Asteroid(
                    position = com.badlogic.gdx.math.Vector2.Zero,
                    velocity = com.badlogic.gdx.math.Vector2.Zero,
                    size = com.nebuladrift.entities.AsteroidSize.SMALL
                ),
                100
            ),
            GameEvent.ShipDestroyed(ship),
            GameEvent.ShipHit(ship, 2),
            GameEvent.AstronautRescued(
                com.nebuladrift.entities.Astronaut(position = com.badlogic.gdx.math.Vector2.Zero)
            )
        )
        AudioManager.onGameEvent(events)
        assertTrue(true, "onGameEvent should handle all event types without crash")
    }
}
