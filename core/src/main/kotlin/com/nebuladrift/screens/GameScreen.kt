package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxGame
import ktx.app.KtxScreen
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.Ship
import com.nebuladrift.input.GameInputProcessor
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.rendering.CameraSetup
import com.nebuladrift.rendering.HudRenderer
import com.nebuladrift.systems.CollisionSystem
import com.nebuladrift.systems.GameContext
import com.nebuladrift.systems.GameEvent
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
    private val i18n: I18nManager
) : KtxScreen {

    // ── Rendering ─────────────────────────────────────────────
    private val cameraSetup = CameraSetup()
    private val shapeRenderer = ShapeRenderer()
    private val batch = SpriteBatch()
    private val hudRenderer = HudRenderer()

    /** Runtime-generated placeholder background texture. */
    private val backgroundTexture: Texture by lazy { createBackgroundTexture() }

    // ── Entity state ──────────────────────────────────────────
    private var ship: Ship = freshShip()
    private val asteroids = mutableListOf<Asteroid>()
    private val lasers = mutableListOf<Laser>()
    private val events = mutableListOf<GameEvent>()
    private var score = 0

    // ── Systems ───────────────────────────────────────────────
    private val physicsSystem = PhysicsSystem()
    private val spawnSystem = SpawnSystem()
    private val collisionSystem = CollisionSystem()
    private val scoreSystem = ScoreSystem()

    // ── Timers ────────────────────────────────────────────────
    private var laserCooldownTimer = 0f

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
            onThrustStart = { ship.isThrusting = true },
            onThrustStop = { ship.isThrusting = false },
            onFire = { fireLaser() }
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
        if (gameOverTriggered) return
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

        // ── Frame context and systems update ──────────────────
        val context = GameContext(ship, asteroids, lasers, events, score)
        physicsSystem.update(delta, context)
        spawnSystem.update(delta, context)
        collisionSystem.update(delta, context)
        scoreSystem.update(delta, context)
        score = context.score   // sync back mutable score

        // ── Render game world ─────────────────────────────────
        // Background
        batch.projectionMatrix = cameraSetup.camera.combined
        batch.begin()
        batch.draw(
            backgroundTexture,
            0f, 0f,
            Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT
        )
        batch.end()

        // Entities via ShapeRenderer (placeholder shapes)
        shapeRenderer.projectionMatrix = cameraSetup.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        renderLasers()
        renderAsteroids()
        renderShip()

        shapeRenderer.end()

        // ── HUD overlay (screen-space) ────────────────────────
        hudRenderer.render(ship, score, scoreSystem.formattedTime, i18n)

        // ── Clear frame-scoped events ─────────────────────────
        events.clear()
    }

    // ── Ship rendering ────────────────────────────────────────

    private fun renderShip() {
        if (ship.isDestroyed) return

        // Invulnerability blink: skip every other 100ms
        if (ship.isInvulnerable && ((ship.invulnerabilityTimer * 10).toInt() % 2 == 0)) {
            // Blink off — skip drawing
        } else {
            val halfSize = ship.radius * 0.8f

            // Ship body colour varies by damage state
            shapeRenderer.color = when (ship.damageState) {
                com.nebuladrift.entities.DamageState.PRISTINE -> Color(0.2f, 0.5f, 1f, 1f)
                com.nebuladrift.entities.DamageState.DAMAGED -> Color(0.4f, 0.4f, 0.8f, 1f)
                com.nebuladrift.entities.DamageState.CRITICAL -> Color(0.8f, 0.3f, 0.3f, 1f)
                com.nebuladrift.entities.DamageState.DESTROYED -> Color.DARK_GRAY
            }
            shapeRenderer.rect(
                ship.position.x - halfSize,
                ship.position.y - halfSize,
                halfSize * 2f,
                halfSize * 2f
            )

            // Thrust flame (small orange rectangle below ship)
            if (ship.isThrusting) {
                shapeRenderer.color = Color.ORANGE
                shapeRenderer.rect(
                    ship.position.x - halfSize * 0.3f,
                    ship.position.y - halfSize * 1.4f,
                    halfSize * 0.6f,
                    halfSize * 0.4f
                )
            }
        }
    }

    // ── Asteroid rendering ────────────────────────────────────

    private fun renderAsteroids() {
        for (asteroid in asteroids) {
            // Colour shade varies by size
            shapeRenderer.color = when (asteroid.size) {
                AsteroidSize.LARGE -> Color(0.4f, 0.4f, 0.4f, 1f)
                AsteroidSize.MEDIUM -> Color(0.55f, 0.55f, 0.55f, 1f)
                AsteroidSize.SMALL -> Color(0.7f, 0.7f, 0.7f, 1f)
            }
            shapeRenderer.circle(asteroid.position.x, asteroid.position.y, asteroid.radius)
        }
    }

    // ── Laser rendering ───────────────────────────────────────

    private fun renderLasers() {
        shapeRenderer.color = Color.YELLOW
        for (laser in lasers) {
            val halfLen = laser.radius * 4f
            shapeRenderer.rectLine(
                laser.position.x - halfLen,
                laser.position.y,
                laser.position.x + halfLen,
                laser.position.y,
                laser.radius * 2f
            )
        }
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
    }

    // ── Game-over transition ──────────────────────────────────

    private fun triggerGameOver() {
        if (gameOverTriggered) return
        gameOverTriggered = true

        // Persist final stats for GameOverScreen
        GameSession.finalScore = score
        GameSession.finalTimeFormatted = scoreSystem.formattedTime
        GameSession.asteroidsDestroyed = scoreSystem.asteroidsDestroyed

        game.setScreen<GameOverScreen>()
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun freshShip() = Ship()

    private fun resetGame() {
        ship = freshShip()
        asteroids.clear()
        lasers.clear()
        events.clear()
        laserCooldownTimer = 0f
        score = 0
        gameOverTriggered = false
        scoreSystem.reset()
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
        shapeRenderer.dispose()
        batch.dispose()
        hudRenderer.dispose()
        backgroundTexture.dispose()
    }
}
