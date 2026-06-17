package com.nebuladrift.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.DamageState
import com.nebuladrift.entities.enemies.EnemyType
import com.nebuladrift.systems.GameContext
import com.nebuladrift.util.Constants
import kotlin.math.sin

/**
 * Entity renderer that owns all SpriteBatch + TextureAtlas draw calls.
 *
 * Zero ShapeRenderer calls in normal mode (REQ-VISUAL-001). A debug
 * overlay toggled with F1 draws collision hitboxes via ShapeRenderer.
 * HUD rendering stays in [HudRenderer].
 *
 * ## Sprite selection
 *
 * Each entity type maps damage state to atlas region key per the
 * convention in [SpriteGenerator].
 */
class GameRenderer(
    private val batch: SpriteBatch,
    private val atlas: SpriteAtlas,
    private val camera: OrthographicCamera,
    private val debugShapeRenderer: ShapeRenderer = ShapeRenderer()
) {
    /** Whether debug hitbox overlay is enabled (toggled by F1). */
    var debugEnabled: Boolean = false
        private set

    /** Last-known background texture (from GameScreen). */
    var backgroundTexture: Texture? = null

    /** Animation manager for frame-based animations. */
    val animationManager = AnimationManager(atlas).also { it.init() }

    /** Parallax background (optional, replaces simple backgroundTexture). */
    var parallaxBackground: ParallaxBackground? = null

    /**
     * Render all entities for the current frame.
     *
     * @param context  Frame snapshot from GameScreen (after all system updates)
     * @param stateTime  Accumulated elapsed time for animation
     */
    fun render(context: GameContext, stateTime: Float) {
        batch.projectionMatrix = camera.combined
        batch.begin()

        renderBackground()
        renderDebris(context)
        renderAstronauts(context)
        renderAsteroids(context)
        renderEnemies(context)
        renderShip(context, stateTime)
        renderLasers(context)

        batch.end()

        // Debug hitbox overlay (ShapeRenderer)
        if (debugEnabled) {
            renderDebugHitboxes(context)
        }

        // Toggle debug with F1
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            debugEnabled = !debugEnabled
        }
    }

    // ── Background ───────────────────────────────────────────────

    private fun renderBackground() {
        // Parallax background takes precedence
        parallaxBackground?.let {
            it.render(batch)
            return
        }
        val bg = backgroundTexture ?: return
        batch.draw(
            bg,
            0f, 0f,
            Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT
        )
    }

    // ── Ship ─────────────────────────────────────────────────────

    private fun renderShip(context: GameContext, stateTime: Float) {
        val ship = context.ship
        if (ship.isDestroyed) return

        // Invulnerability blink: skip every other 100ms
        if (ship.isInvulnerable && ((ship.invulnerabilityTimer * 10).toInt() % 2 == 0)) {
            // Blink off
        } else {
            val spriteName = when (ship.damageState) {
                DamageState.PRISTINE -> "ship_pristine"
                DamageState.DAMAGED  -> "ship_damaged"
                DamageState.CRITICAL -> "ship_critical"
                DamageState.DESTROYED -> "ship_pristine" // fallback, should not draw
            }
            val region = atlas.findRegion(spriteName)
            val halfSize = ship.radius * 0.8f
            batch.draw(
                region,
                ship.position.x - halfSize,
                ship.position.y - halfSize,
                halfSize * 2f,
                halfSize * 2f
            )
        }

        // Thrust flame (animated via AnimationManager)
        if (ship.isThrusting) {
            val thrustRegion = animationManager.getKeyFrame("thrust", stateTime)
            if (thrustRegion != null) {
                val halfSize = ship.radius * 0.8f
                batch.draw(
                    thrustRegion,
                    ship.position.x - halfSize * 0.3f,
                    ship.position.y - halfSize * 1.4f,
                    halfSize * 0.6f,
                    halfSize * 0.4f
                )
            }
        }
    }

    // ── Asteroids ────────────────────────────────────────────────

    private fun renderAsteroids(context: GameContext) {
        for (asteroid in context.asteroids) {
            val sizeKey = when (asteroid.size) {
                AsteroidSize.LARGE  -> "large"
                AsteroidSize.MEDIUM -> "medium"
                AsteroidSize.SMALL  -> "small"
            }
            val spriteName = "asteroid_${sizeKey}_${asteroid.health}"
            val region = atlas.findRegion(spriteName)
            val d = asteroid.radius * 2f
            batch.draw(
                region,
                asteroid.position.x - asteroid.radius,
                asteroid.position.y - asteroid.radius,
                asteroid.radius, // originX (center)
                asteroid.radius, // originY (center)
                d, d,
                1f, 1f,
                asteroid.rotation
            )
        }
    }

    // ── Enemies ──────────────────────────────────────────────────

    private fun renderEnemies(context: GameContext) {
        for (enemy in context.enemies) {
            val spriteName = when (enemy.getType()) {
                EnemyType.LIGHT_FIGHTER -> "enemy_fighter_1"
                EnemyType.MEDIUM_FRIGATE -> {
                    when {
                        enemy.health >= 2 -> "enemy_frigate_1"
                        else              -> "enemy_frigate_2"
                    }
                }
                EnemyType.HEAVY_DESTROYER -> {
                    when {
                        enemy.health >= 3 -> "enemy_destroyer_1"
                        enemy.health == 2 -> "enemy_destroyer_2"
                        else              -> "enemy_destroyer_3"
                    }
                }
                EnemyType.DARK_CLONE -> "enemy_clone"
            }
            val region = atlas.findRegion(spriteName)

            // Scale draw size by enemy type
            val drawSize = when (enemy.getType()) {
                EnemyType.LIGHT_FIGHTER -> enemy.radius * 2f
                EnemyType.MEDIUM_FRIGATE -> enemy.radius * 1.6f
                EnemyType.HEAVY_DESTROYER -> enemy.radius * 2f
                EnemyType.DARK_CLONE -> enemy.radius * 1.6f
            }
            batch.draw(
                region,
                enemy.position.x - drawSize / 2f,
                enemy.position.y - drawSize / 2f,
                drawSize, drawSize
            )
        }
    }

    // ── Astronauts ──────────────────────────────────────────────

    private fun renderAstronauts(context: GameContext) {
        for (astronaut in context.astronauts) {
            val baseX = astronaut.position.x
            val baseY = astronaut.position.y
            val spriteName: String
            val drawY: Float
            val drawSize: Float
            val alpha: Float

            when (astronaut.state) {
                Astronaut.State.FLOATING -> {
                    spriteName = "astro_floating"
                    drawY = baseY
                    drawSize = astronaut.radius * 2f
                    alpha = 1f
                }
                Astronaut.State.RESCUED -> {
                    spriteName = "astro_rescued"
                    // Sine wave bob + rise
                    val offsetY = sin(astronaut.stateTimer * kotlin.math.PI.toFloat() * 4f) * 0.3f
                    drawY = baseY + offsetY + astronaut.stateTimer * 1.5f
                    drawSize = astronaut.radius * 1.6f
                    alpha = 1f
                }
                Astronaut.State.DEAD -> {
                    spriteName = "astro_dead"
                    // Fall down + fade out
                    drawY = baseY - astronaut.stateTimer * 2f
                    drawSize = astronaut.radius * 1.2f
                    alpha = (1f - astronaut.stateTimer / Constants.ASTRONAUT_DEATH_FADE_DURATION)
                        .coerceIn(0f, 1f)
                }
            }

            val region = atlas.findRegion(spriteName)
            batch.color = Color(1f, 1f, 1f, alpha)
            batch.draw(
                region,
                baseX - drawSize / 2f,
                drawY - drawSize / 2f,
                drawSize, drawSize
            )
            batch.color = Color.WHITE
        }
    }

    // ── Debris ──────────────────────────────────────────────────

    private fun renderDebris(context: GameContext) {
        for (d in context.debris) {
            // Glow (tinted via batch color)
            val glowRegion = atlas.findRegion("debris_glow")
            val glowSize = d.radius * 5f
            val alpha = 0.5f + 0.5f * sin(d.glowPhase)
            batch.color = Color(1f, 0.85f, 0f, alpha * 0.4f)
            batch.draw(
                glowRegion,
                d.position.x - glowSize / 2f,
                d.position.y - glowSize / 2f,
                glowSize, glowSize
            )

            // Core
            batch.color = Color(1f, 0.85f, 0f, 1f)
            val coreRegion = atlas.findRegion("debris")
            val coreSize = d.radius * 2f
            batch.draw(
                coreRegion,
                d.position.x - coreSize / 2f,
                d.position.y - coreSize / 2f,
                coreSize, coreSize
            )
        }
        batch.color = Color.WHITE  // reset tint
    }

    // ── Lasers ──────────────────────────────────────────────────

    private fun renderLasers(context: GameContext) {
        for (laser in context.lasers) {
            // Glow
            val glowRegion = atlas.findRegion("laser_glow")
            val glowW = laser.radius * 12f
            val glowH = laser.radius * 6f
            batch.draw(
                glowRegion,
                laser.position.x - glowW / 2f,
                laser.position.y - glowH / 2f,
                glowW, glowH
            )

            // Core
            val coreRegion = atlas.findRegion("laser")
            val coreW = laser.radius * 8f
            val coreH = laser.radius * 2f
            batch.draw(
                coreRegion,
                laser.position.x - coreW / 2f,
                laser.position.y - coreH / 2f,
                coreW, coreH
            )
        }
    }

    // ── Debug hitboxes ──────────────────────────────────────────

    private fun renderDebugHitboxes(context: GameContext) {
        debugShapeRenderer.projectionMatrix = camera.combined
        debugShapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        // Ship
        if (!context.ship.isDestroyed) {
            debugShapeRenderer.color = Color.CYAN
            debugShapeRenderer.circle(
                context.ship.position.x,
                context.ship.position.y,
                context.ship.radius
            )
        }

        // Asteroids
        debugShapeRenderer.color = Color.GRAY
        for (a in context.asteroids) {
            debugShapeRenderer.circle(a.position.x, a.position.y, a.radius)
        }

        // Enemies
        debugShapeRenderer.color = Color.RED
        for (e in context.enemies) {
            debugShapeRenderer.circle(e.position.x, e.position.y, e.radius)
        }

        // Astronauts
        debugShapeRenderer.color = Color.GREEN
        for (a in context.astronauts) {
            debugShapeRenderer.circle(a.position.x, a.position.y, a.radius)
        }

        // Debris
        debugShapeRenderer.color = Color.YELLOW
        for (d in context.debris) {
            debugShapeRenderer.circle(d.position.x, d.position.y, d.radius)
        }

        // Lasers
        debugShapeRenderer.color = Color.ORANGE
        for (l in context.lasers) {
            debugShapeRenderer.circle(l.position.x, l.position.y, l.radius)
        }

        debugShapeRenderer.end()
    }

    fun dispose() {
        debugShapeRenderer.dispose()
    }
}
