package com.nebuladrift.util

/**
 * Game-wide constants for tuning and world configuration.
 *
 * World units are abstract (1 unit ≈ 1 meter in a 16:9 aspect world).
 * All physics, sizes, and timings are defined here for single-source tuning.
 */
object Constants {
    // World dimensions (abstract units, 16:9 aspect ratio)
    const val WORLD_WIDTH = 16f
    const val WORLD_HEIGHT = 9f

    // ── Gravity ────────────────────────────────────────────────
    /** Constant downward acceleration applied each frame. */
    const val GRAVITY_ACCELERATION = -9.8f // units/s²

    // ── Ship ───────────────────────────────────────────────────
    /** Upward impulse applied on tap / A-key. */
    const val SHIP_THRUST = 15f // units/s²

    /** Radius of the ship's collision circle. */
    const val SHIP_RADIUS = 0.3f

    /** Starting lives. */
    const val SHIP_LIVES = 3

    /** Invulnerability window after a hit (seconds). */
    const val INVULNERABILITY_TIME = 2f

    /** Velocity damping factor per frame (0 = full stop, 1 = no damping). */
    const val VELOCITY_DAMPING = 0.98f

    // ── Lasers ─────────────────────────────────────────────────
    /** Speed of a laser projectile. */
    const val LASER_SPEED = 20f // units/s

    /** Max lifetime of a laser before auto-removal. */
    const val LASER_LIFETIME = 2f // seconds

    /** Collision radius of a laser projectile. */
    const val LASER_RADIUS = 0.05f

    /** Minimum interval between laser shots. */
    const val LASER_COOLDOWN = 0.25f // seconds

    // ── Asteroids ──────────────────────────────────────────────
    /** Initial spawn interval (seconds between waves). */
    const val ASTEROID_SPAWN_INTERVAL = 2f

    /** Minimum asteroid speed. */
    const val ASTEROID_MIN_SPEED = 2f // units/s

    /** Maximum asteroid speed. */
    const val ASTEROID_MAX_SPEED = 5f // units/s

    /** Large asteroid collision radius. */
    const val ASTEROID_LARGE_RADIUS = 0.8f

    /** Medium asteroid collision radius. */
    const val ASTEROID_MEDIUM_RADIUS = 0.5f

    /** Small asteroid collision radius. */
    const val ASTEROID_SMALL_RADIUS = 0.3f

    /** Hit points for each size tier. */
    const val ASTEROID_LARGE_HP = 3
    const val ASTEROID_MEDIUM_HP = 2
    const val ASTEROID_SMALL_HP = 1

    /** Max simultaneous asteroids before spawning pauses. */
    const val ASTEROID_MAX_COUNT = 20

    // ── Scoring ────────────────────────────────────────────────
    /** Points awarded for destroying a small asteroid. */
    const val SCORE_SMALL = 100

    /** Points awarded for destroying a medium asteroid. */
    const val SCORE_MEDIUM = 200

    /** Points awarded for destroying a large asteroid. */
    const val SCORE_LARGE = 300

    /** Time bonus: points per second survived. */
    const val SCORE_TIME_BONUS = 1 // point/s

    // ── Enemies ────────────────────────────────────────────────
    /** Default enemy spawn interval (seconds). */
    const val ENEMY_SPAWN_INTERVAL = 3f

    /** Light Fighter (small, fast, 1 HP). */
    const val ENEMY_LIGHT_SPEED = 3f
    const val ENEMY_LIGHT_RADIUS = 0.25f

    /** Medium Frigate (medium speed, 2 HP). */
    const val ENEMY_MEDIUM_SPEED = 2.5f
    const val ENEMY_MEDIUM_RADIUS = 0.4f

    /** Heavy Destroyer (slow, large, 3 HP). */
    const val ENEMY_HEAVY_SPEED = 1.5f
    const val ENEMY_HEAVY_RADIUS = 0.6f

    /** Dark Clone (mirrors player, 2-3 HP). */
    const val ENEMY_CLONE_SPEED = 2f

    // ── Enemy Points ───────────────────────────────────────────
    const val SCORE_LIGHT_FIGHTER = 150
    const val SCORE_MEDIUM_FRIGATE = 250
    const val SCORE_HEAVY_DESTROYER = 400
    const val SCORE_DARK_CLONE = 500

    // ── Astronaut ──────────────────────────────────────────────
    /** Interval between astronaut spawns (seconds). */
    const val ASTRONAUT_SPAWN_INTERVAL = 12f

    /** Astronaut drift speed. */
    const val ASTRONAUT_SPEED = 0.8f

    /** Astronaut collision radius. */
    const val ASTRONAUT_RADIUS = 0.2f

    /** Points awarded for rescuing an astronaut. */
    const val SCORE_ASTRONAUT_RESCUE = 500

    /** Points deducted for killing an astronaut. */
    const val SCORE_ASTRONAUT_KILL_PENALTY = 200

    // ── Space Debris ───────────────────────────────────────────
    /** Interval between debris spawns (seconds). */
    const val DEBRIS_SPAWN_INTERVAL = 25f

    /** Debris drift speed. */
    const val DEBRIS_SPEED = 0.5f

    /** Debris collision radius. */
    const val DEBRIS_RADIUS = 0.15f

    /** Speed of the glow phase oscillation (radians/s). */
    const val DEBRIS_GLOW_SPEED = 3f

    // ── Difficulty Curve ───────────────────────────────────────
    /** No enemies spawn during the first N seconds. */
    const val DIFFICULTY_SAFE_ZONE = 15f

    /** Duration over which difficulty ramps from start to end values. */
    const val DIFFICULTY_RAMP_DURATION = 180f

    /** Scroll speed endpoints (units/s). */
    const val DIFFICULTY_START_SCROLL_SPEED = 2f
    const val DIFFICULTY_END_SCROLL_SPEED = 5f

    /** Asteroid spawn interval endpoints (seconds between waves). */
    const val DIFFICULTY_START_ASTEROID_RATE = 2f
    const val DIFFICULTY_END_ASTEROID_RATE = 0.5f

    /** Enemy spawn interval endpoints (seconds between spawns). */
    const val DIFFICULTY_START_ENEMY_RATE = 4f
    const val DIFFICULTY_END_ENEMY_RATE = 2f

    // ── Mirror / Dark Clone ────────────────────────────────────
    /** Delay between player action and Dark Clone mirroring (seconds). */
    const val CLONE_MIRROR_DELAY = 0.5f

    /** Number of frames in the mirror queue (~0.5s at 60 FPS). */
    const val CLONE_MIRROR_QUEUE_SIZE = 30

    // ── Sprite pixel dimensions (procedural spritesheet) ─────────
    const val SPRITE_SHIP = 64
    const val SPRITE_ASTEROID_LARGE = 128
    const val SPRITE_ASTEROID_MEDIUM = 96
    const val SPRITE_ASTEROID_SMALL = 64
    const val SPRITE_ENEMY_FIGHTER = 48
    const val SPRITE_ENEMY_FRIGATE = 64
    const val SPRITE_ENEMY_DESTROYER = 96
    const val SPRITE_ENEMY_CLONE = 64
    const val SPRITE_ASTRONAUT = 48
    const val SPRITE_DEBRIS = 48
    const val SPRITE_DEBRIS_GLOW = 64
    const val SPRITE_LASER_WIDTH = 32
    const val SPRITE_LASER_HEIGHT = 8
    const val SPRITE_LASER_GLOW_WIDTH = 48
    const val SPRITE_LASER_GLOW_HEIGHT = 16
    const val SPRITE_THRUST_WIDTH = 32
    const val SPRITE_THRUST_HEIGHT = 16
    const val SPRITE_EXPLOSION_SMALL = 48
    const val SPRITE_EXPLOSION_MEDIUM = 64
    const val SPRITE_EXPLOSION_LARGE = 96
    const val SPRITE_EXPLOSION_FRAMES = 6
    const val SPRITE_THRUST_FRAMES = 2
    const val SPRITE_ATLAS_MAX_WIDTH = 1024

    // ── Animation / Timing ────────────────────────────────────
    /** Frame duration for thrust flame flicker (~8 FPS). */
    const val THRUST_FRAME_DURATION = 0.125f

    /** Total duration of explosion animation. */
    const val EXPLOSION_DURATION = 0.5f

    // ── Particles ──────────────────────────────────────────────
    /** Maximum simultaneous particles on screen. */
    const val PARTICLE_MAX_COUNT = 300

    /** Pixel size of the white particle circle texture. */
    const val SPRITE_PARTICLE = 16

    // ── Audio ──────────────────────────────────────────────────
    /** Default music volume (0..1). */
    const val MUSIC_VOLUME_DEFAULT = 0.5f

    /** Default SFX volume (0..1). */
    const val SFX_VOLUME_DEFAULT = 0.7f

    /** Filename for background music (assets/music/bgm.mp3). */
    const val MUSIC_FILENAME = "music/bgm.mp3"

    /** SFX filenames in assets/sfx/. */
    const val SFX_LASER = "sfx/laser.wav"
    const val SFX_EXPLOSION_SMALL = "sfx/explosion_small.wav"
    const val SFX_EXPLOSION_LARGE = "sfx/explosion_large.wav"
    const val SFX_RESCUE = "sfx/rescue.wav"
    const val SFX_DAMAGE = "sfx/damage.wav"
    const val SFX_GAME_OVER = "sfx/game_over.wav"
    const val SFX_NEW_RECORD = "sfx/new_record.wav"

    // ── Screen Transition ───────────────────────────────────────
    /** Duration of each fade half (fade-out or fade-in) in seconds. */
    const val TRANSITION_DURATION = 0.5f

    // ── Parallax Background ──────────────────────────────────────
    /** Pixel width of each parallax layer texture. */
    const val PARALLAX_BG_WIDTH = 512

    /** Pixel height of each parallax layer texture. */
    const val PARALLAX_BG_HEIGHT = 288

    /** Far stars layer scroll speed multiplier. */
    const val PARALLAX_FAR_SPEED = 0.1f

    /** Near nebula layer scroll speed multiplier. */
    const val PARALLAX_NEAR_SPEED = 0.3f

    /** Number of star dots on the far layer. */
    const val PARALLAX_STAR_COUNT = 120

    // ── Astronaut Animation ─────────────────────────────────────
    /** Duration of the death fade-out animation (seconds). */
    const val ASTRONAUT_DEATH_FADE_DURATION = 0.5f

    // ── Version ────────────────────────────────────────────────
    /** Current game version (semantic versioning). */
    const val GAME_VERSION = "0.4.0"

    // ── Debug / Toggle ─────────────────────────────────────────
    /** Key code for toggling debug hitbox overlay (F1). */
    const val DEBUG_TOGGLE_KEY = com.badlogic.gdx.Input.Keys.F1
}
