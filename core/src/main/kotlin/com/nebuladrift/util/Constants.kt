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
}
