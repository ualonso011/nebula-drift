package com.nebuladrift.managers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.nebuladrift.systems.GameEvent
import com.nebuladrift.util.Constants

/**
 * Singleton audio manager for music and SFX.
 *
 * Manages a single looping background music track and a map of named
 * sound effects. Volumes are persisted via [Gdx.app.getPreferences].
 *
 * ## Design
 * - **Singleton object** — single audio context shared across
 *   MenuScreen, GameScreen, and GameOverScreen.
 * - **Graceful degradation** — if audio assets are absent (no
 *   `assets/sfx/` or `assets/music/` directory), all operations
 *   become no-ops without crashing.
 * - **Separate volumes** — music and SFX volumes are independent
 *   and persisted as `music_volume` / `sfx_volume` preferences.
 *
 * Usage:
 * ```kotlin
 * // One-time init in NebulaDriftGame.create()
 * AudioManager.init()
 *
 * // In screens
 * AudioManager.playMusic("bgm")
 * AudioManager.playSound("explosion_small")
 * ```
 */
object AudioManager {

    private var music: Music? = null
    private val sounds = mutableMapOf<String, Sound>()

    /** Current music volume (0..1). */
    var musicVolume: Float = Constants.MUSIC_VOLUME_DEFAULT
        private set

    /** Current SFX volume (0..1). */
    var sfxVolume: Float = Constants.SFX_VOLUME_DEFAULT
        private set

    /** Whether [init] has been called. */
    private var initialized = false

    // ── Initialisation ──────────────────────────────────────────

    /**
     * Initialise the audio manager.
     *
     * Loads persisted volume preferences. Attempts to load BGM and
     * SFX assets; if files are absent, the manager enters silent
     * mode where all play calls are no-ops.
     *
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    fun init() {
        if (initialized) return
        initialized = true

        // Load persisted volume preferences
        val prefs = Gdx.app.getPreferences("nebula-drift")
        musicVolume = prefs.getFloat("music_volume", Constants.MUSIC_VOLUME_DEFAULT)
            .coerceIn(0f, 1f)
        sfxVolume = prefs.getFloat("sfx_volume", Constants.SFX_VOLUME_DEFAULT)
            .coerceIn(0f, 1f)

        // Load SFX assets (gracefully skip if missing)
        loadSound(Constants.SFX_LASER)
        loadSound(Constants.SFX_EXPLOSION_SMALL)
        loadSound(Constants.SFX_EXPLOSION_LARGE)
        loadSound(Constants.SFX_RESCUE)
        loadSound(Constants.SFX_DAMAGE)
        loadSound(Constants.SFX_GAME_OVER)
        loadSound(Constants.SFX_NEW_RECORD)
        loadSound(Constants.SFX_LASER_CLASH)
        loadSound(Constants.SFX_REPAIR)
    }

    /**
     * Attempt to load a sound effect from the assets directory.
     * If the file does not exist, silently skip.
     */
    private fun loadSound(filename: String) {
        try {
            if (Gdx.files.internal(filename).exists()) {
                sounds[filename] = Gdx.audio.newSound(Gdx.files.internal(filename))
            }
        } catch (_: Exception) {
            // File not found or unreadable — silent skip
        }
    }

    /**
     * Attempt to load and start looping the background music.
     * If the file does not exist, silently skip.
     */
    private fun loadAndPlayMusic(filename: String) {
        try {
            if (Gdx.files.internal(filename).exists()) {
                val newMusic = Gdx.audio.newMusic(Gdx.files.internal(filename))
                newMusic.isLooping = true
                newMusic.volume = musicVolume
                newMusic.play()
                music?.stop()
                music?.dispose()
                music = newMusic
            }
        } catch (_: Exception) {
            // File not found or unreadable — silent skip
        }
    }

    // ── Music ───────────────────────────────────────────────────

    /**
     * Play (and loop) the named music track.
     * Stops any currently playing music first.
     */
    fun playMusic(track: String) {
        loadAndPlayMusic(track)
    }

    /**
     * Start or resume background music playback.
     */
    fun startMusic() {
        music?.play()
    }

    /**
     * Pause background music playback.
     */
    fun pauseMusic() {
        music?.pause()
    }

    /**
     * Stop and release background music.
     */
    fun stopMusic() {
        music?.stop()
    }

    // ── SFX ─────────────────────────────────────────────────────

    /**
     * Play a named sound effect at the current [sfxVolume].
     * No-op if the sound was not loaded (asset missing).
     */
    fun playSound(name: String) {
        sounds[name]?.play(sfxVolume)
    }

    // ── Volume ──────────────────────────────────────────────────

    /**
     * Set music volume, clamped to [0..1].
     * Persists to preferences immediately.
     */
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        music?.volume = musicVolume
        savePreferences()
    }

    /**
     * Set SFX volume, clamped to [0..1].
     * Persists to preferences immediately.
     */
    fun setSfxVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0f, 1f)
        savePreferences()
    }

    // ── Lifecycle ───────────────────────────────────────────────

    /**
     * Pause all audio (called when game loses focus).
     */
    fun pause() {
        music?.pause()
    }

    /**
     * Resume all audio (called when game regains focus).
     */
    fun resume() {
        music?.play()
    }

    /**
     * Release all audio resources.
     */
    fun dispose() {
        music?.stop()
        music?.dispose()
        music = null
        for (sound in sounds.values) {
            sound.dispose()
        }
        sounds.clear()
    }

    // ── Event dispatch ──────────────────────────────────────────

    /**
     * Dispatch a list of frame-scoped [GameEvent]s to matching SFX.
     */
    fun onGameEvent(events: List<GameEvent>) {
        for (event in events) {
            when (event) {
                is GameEvent.LaserFired -> playSound(Constants.SFX_LASER)
                is GameEvent.AsteroidDestroyed -> playSound(Constants.SFX_EXPLOSION_SMALL)
                is GameEvent.ShipDestroyed -> playSound(Constants.SFX_EXPLOSION_LARGE)
                is GameEvent.ShipHit -> playSound(Constants.SFX_DAMAGE)
                is GameEvent.AstronautRescued -> playSound(Constants.SFX_RESCUE)
                is GameEvent.EnemyDestroyed -> playSound(Constants.SFX_EXPLOSION_SMALL)
                is GameEvent.LaserLaserHit -> playSound(Constants.SFX_LASER_CLASH)
                is GameEvent.DebrisCollected -> playSound(Constants.SFX_REPAIR)
                else -> { /* No audio feedback for other events */ }
            }
        }
    }

    // ── Persistence ─────────────────────────────────────────────

    private fun savePreferences() {
        val prefs = Gdx.app.getPreferences("nebula-drift")
        prefs.putFloat("music_volume", musicVolume)
        prefs.putFloat("sfx_volume", sfxVolume)
        prefs.flush()
    }
}
