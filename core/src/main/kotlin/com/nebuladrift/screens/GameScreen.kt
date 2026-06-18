package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.Ship
import com.nebuladrift.entities.SpaceDebris
import com.nebuladrift.entities.enemies.Enemy
import com.nebuladrift.input.GameInputProcessor
import com.nebuladrift.managers.AudioManager
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.rendering.CameraSetup
import com.nebuladrift.rendering.GameRenderer
import com.nebuladrift.rendering.HudRenderer
import com.nebuladrift.rendering.ParallaxBackground
import com.nebuladrift.rendering.ParticleManager
import com.nebuladrift.rendering.SpriteAtlas
import com.nebuladrift.systems.AstronautSpawnSystem
import com.nebuladrift.systems.CollisionSystem
import com.nebuladrift.systems.DebrisSpawnSystem
import com.nebuladrift.systems.DifficultyManager
import com.nebuladrift.systems.EnemySpawnSystem
import com.nebuladrift.systems.GameContext
import com.nebuladrift.systems.GameEvent
import com.nebuladrift.systems.MirrorSystem
import com.nebuladrift.systems.PhysicsSystem
import com.nebuladrift.systems.ScoreSystem
import com.nebuladrift.systems.SpawnSystem
import com.nebuladrift.util.Constants
import com.nebuladrift.NebulaDriftGame

/**
 * Main gameplay screen.
 *
 * Owns the game entities, systems (physics, collision, spawn, score),
 * input processing, and rendering. Integrates a [HudRenderer] overlay
 * and a [ScoreSystem] for tracking.
 *
 * Transitions to [GameOverScreen] when the ship is destroyed.
 */
class GameScreen(
    private val game: NebulaDriftGame,
    private val i18n: I18nManager,
    private val atlas: SpriteAtlas,
    /**
     * Optional callback for Android. When set, fired instead of
     * transitioning to [GameOverScreen] — the host Activity is
     * expected to handle the result itself.
     */
    private val onGameOver: (() -> Unit)? = null,
) : KtxScreen {

    // ── Rendering ─────────────────────────────────────────────
    private val cameraSetup = CameraSetup()
    private val batch = SpriteBatch()
    private val hudRenderer = HudRenderer()

    /** Entity renderer using the procedural sprite atlas. */
    private val gameRenderer = GameRenderer(batch, atlas, cameraSetup.camera)

    /** Particle system for explosions, trails, sparkles, and damage. */
    private val particleManager = ParticleManager(atlas).also { it.init() }

    /** Parallax scrolling background (replaces simple background texture). */
    private val parallaxBackground = ParallaxBackground().also { it.init() }

    /** Runtime-generated placeholder background texture (fallback). */
    private val backgroundTexture: Texture by lazy { createBackgroundTexture() }

    // ── Entity state ──────────────────────────────────────────
    private var ship: Ship = freshShip()
    private val asteroids = mutableListOf<Asteroid>()
    private val lasers = mutableListOf<Laser>()
    private val enemies = mutableListOf<Enemy>()
    private val astronauts = mutableListOf<Astronaut>()
    private val debris = mutableListOf<SpaceDebris>()
    private val events = mutableListOf<GameEvent>()
    private var score = 0
    private var elapsedTime = 0f

    // ── Systems ───────────────────────────────────────────────
    private val physicsSystem = PhysicsSystem()
    private val spawnSystem = SpawnSystem()
    private val collisionSystem = CollisionSystem()
    private val scoreSystem = ScoreSystem()
    private val difficultyManager = DifficultyManager()
    private val mirrorSystem = MirrorSystem()
    private val enemySpawnSystem = EnemySpawnSystem()
    private val astronautSpawnSystem = AstronautSpawnSystem()
    private val debrisSpawnSystem = DebrisSpawnSystem()

    // ── Timers ────────────────────────────────────────────────
    private var laserCooldownTimer = 0f
    /** Set to true when the player fires a laser (consumed by MirrorSystem). */
    private var justFired = false

    // ── Input ─────────────────────────────────────────────────
    private val inputMultiplexer = InputMultiplexer()
    private lateinit var gameInputProcessor: GameInputProcessor

    // ── Flow ──────────────────────────────────────────────────
    /** Whether a game-over transition has already been triggered. */
    private var gameOverTriggered = false

    // ── Lifecycle ─────────────────────────────────────────────

    override fun show() {
        resetGame()

        // Notify HUD of current screen size
        hudRenderer.resize(Gdx.graphics.width, Gdx.graphics.height)

        gameInputProcessor = GameInputProcessor(
            onFlap = {
                // Flappy Bird style: instant upward impulse
                ship.velocity.y = Constants.SHIP_FLAP_VELOCITY
            },
            onFire = {
                fireLaser()
                justFired = true
            }
        )

        inputMultiplexer.addProcessor(gameInputProcessor)
        Gdx.input.inputProcessor = inputMultiplexer
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        cameraSetup.resize(width, height)
        hudRenderer.resize(width, height)
    }

    override fun render(delta: Float) {
        // ── Game-over check ───────────────────────────────────
        if (gameOverTriggered) {
            // Keep rendering the transition overlay if active;
            // skip game logic but still let the game-level render pass.
            if (game.transition == null) return
            // Clear and let the game-level overlay handle the rest.
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            return
        }
        if (ship.isDestroyed) {
            triggerGameOver()
            return
        }

        // ── Clear ─────────────────────────────────────────────
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cameraSetup.viewport.apply()
        cameraSetup.update()

        // ── Cooldown ──────────────────────────────────────────
        laserCooldownTimer = (laserCooldownTimer - delta).coerceAtLeast(0f)

        // ── Update elapsed time ───────────────────────────────
        elapsedTime += delta

        // ── Update DifficultyManager ──────────────────────────
        difficultyManager.update(elapsedTime)

        // ── Record player action for MirrorSystem ─────────────
        mirrorSystem.recordPlayerAction(
            elapsedTime = elapsedTime,
            ship = ship,
            isShooting = justFired
        )
        justFired = false

        // ── Frame context ─────────────────────────────────────
        val context = GameContext(
            ship = ship,
            asteroids = asteroids,
            lasers = lasers,
            enemies = enemies,
            astronauts = astronauts,
            debris = debris,
            events = events,
            score = score,
            elapsedTime = elapsedTime,
            difficultyManager = difficultyManager,
            mirrorSystem = mirrorSystem
        )

        // ── Systems update (order matters) ────────────────────
        physicsSystem.update(delta, context)
        spawnSystem.update(delta, context)
        enemySpawnSystem.update(delta, context)
        astronautSpawnSystem.update(delta, context)
        debrisSpawnSystem.update(delta, context)
        collisionSystem.update(delta, context)
        scoreSystem.update(delta, context)
        score = context.score   // sync back mutable score

        // ── Event dispatch (Particles + Audio) ─────────────────
        particleManager.onGameEvent(events)
        AudioManager.onGameEvent(events)

        // ── Update particles ──────────────────────────────────
        particleManager.update(delta)

        // ── Update parallax background ────────────────────────
        parallaxBackground.update(delta, difficultyManager.scrollSpeedMultiplier)

        // ── Render game world via GameRenderer ────────────────
        gameRenderer.parallaxBackground = parallaxBackground
        gameRenderer.backgroundTexture = backgroundTexture
        gameRenderer.render(context, elapsedTime)

        // ── Render particles over game world ──────────────────
        batch.begin()
        particleManager.render(batch)
        batch.end()

        // ── HUD overlay (screen-space) ────────────────────────
        hudRenderer.render(ship, score, scoreSystem.formattedTime, scoreSystem.astronautsRescued, i18n)

        // ── Clear frame-scoped events ─────────────────────────
        events.clear()
    }

    // ── Actions ───────────────────────────────────────────────

    private fun fireLaser() {
        if (laserCooldownTimer > 0f) return
        laserCooldownTimer = Constants.LASER_COOLDOWN

        val laser = Laser(
            position = Vector2(
                ship.position.x + ship.radius + 0.2f,
                ship.position.y
            ),
            velocity = Vector2(Constants.LASER_SPEED, 0f)
        )
        lasers.add(laser)
        events.add(GameEvent.LaserFired(laser))
    }

    // ── Game-over transition ──────────────────────────────────

    private fun triggerGameOver() {
        if (gameOverTriggered) return
        gameOverTriggered = true

        // Persist final stats for GameOverScreen
        GameSession.finalScore = score
        GameSession.finalTime = scoreSystem.elapsedTime
        GameSession.finalTimeFormatted = scoreSystem.formattedTime
        GameSession.asteroidsDestroyed = scoreSystem.asteroidsDestroyed
        GameSession.enemiesDestroyed = scoreSystem.enemiesDestroyed
        GameSession.astronautsRescued = scoreSystem.astronautsRescued
        GameSession.astronautsKilled = scoreSystem.astronautsKilled

        if (onGameOver != null) {
            onGameOver()
        } else {
            game.startTransition { game.setScreen<GameOverScreen>() }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun freshShip() = Ship()

    private fun resetGame() {
        ship = freshShip()
        asteroids.clear()
        lasers.clear()
        enemies.clear()
        astronauts.clear()
        debris.clear()
        events.clear()
        laserCooldownTimer = 0f
        score = 0
        elapsedTime = 0f
        justFired = false
        gameOverTriggered = false
        scoreSystem.reset()
        mirrorSystem.reset()
        particleManager.clear()
    }

    // ── Placeholder textures ──────────────────────────────────

    /**
     * Generate a simple dark-space background texture at runtime.
     * A subtle radial gradient gives depth without requiring an
     * external image file.
     */
    private fun createBackgroundTexture(): Texture {
        val width = 256
        val height = 144 // 16:9
        val pixmap = Pixmap(width, height, Pixmap.Format.RGB888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - width / 2f
                val dy = y - height / 2f
                val dist = kotlin.math.sqrt(dx * dx + dy * dy).toFloat() /
                    kotlin.math.sqrt((width / 2f) * (width / 2f) + (height / 2f) * (height / 2f)).toFloat()
                // Dark blue-black gradient: edges darker, centre slightly lighter
                val brightness = (0.02f + (1f - dist.coerceIn(0f, 1f)) * 0.06f).coerceIn(0f, 1f)
                val r = (brightness * 10f).toInt().coerceIn(0, 255)
                val g = (brightness * 15f).toInt().coerceIn(0, 255)
                val b = (brightness * 40f).toInt().coerceIn(10, 255)
                pixmap.setColor(r / 255f, g / 255f, b / 255f, 1f)
                pixmap.drawPixel(x, y)
            }
        }

        // Sprinkle a few "stars"
        pixmap.setColor(1f, 1f, 1f, 0.8f)
        for (i in 0 until 40) {
            val sx = (i * 23 + 7) % width
            val sy = (i * 17 + 13) % height
            pixmap.drawPixel(sx, sy)
            val brightness = 0.6f + (i % 3) * 0.2f
            pixmap.setColor(brightness, brightness, brightness, 0.6f)
        }

        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    // ── Cleanup ───────────────────────────────────────────────

    override fun dispose() {
        gameRenderer.dispose()
        parallaxBackground.dispose()
        batch.dispose()
        hudRenderer.dispose()
        backgroundTexture.dispose()
    }
}
