package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.nebuladrift.entities.DamageState
import com.nebuladrift.util.Constants
import kotlin.math.roundToInt

/**
 * Procedural sprite atlas for all game visuals.
 *
 * Generates every sprite at runtime via [Pixmap] — no external art
 * dependencies. Sprites are packed into a single [Texture] to keep
 * the GPU in a single bind state for the entire entity render pass.
 *
 * Sprite key convention (used with [SpriteAtlas.findRegion]):
 * ```
 * ship_pristine, ship_damaged, ship_critical
 * asteroid_{large|medium|small}_{3|2|1}
 * enemy_fighter_1, enemy_frigate_{1|2}, enemy_destroyer_{1|2|3}, enemy_clone
 * astro_floating, astro_rescued, astro_dead
 * debris, debris_glow
 * laser, laser_glow
 * thrust_0, thrust_1
 * explosion_{small|medium|large}_0..5
 * particle
 * ```
 */
object SpriteGenerator {

    /**
     * Generate all sprites and pack them into a single-texture atlas.
     * Call once at game initialisation ([NebulaDriftGame.create()]).
     */
    fun generateAtlas(): SpriteAtlas {
        val sprites = buildSpritePixmaps()
        return pack(sprites)
    }

    // ── Public per-sprite generators (for testing without GL context) ──

    internal fun generateShipPixmap(state: DamageState): Pixmap =
        createShipSprite(state)

    internal fun generateAsteroidPixmap(size: Int, hp: Int, maxHp: Int): Pixmap =
        createAsteroidSprite(size, hp, maxHp)

    internal fun generateEnemyFighterPixmap(): Pixmap =
        createEnemyFighterSprite()

    internal fun generateEnemyFrigatePixmap(hp: Int): Pixmap =
        createEnemyFrigateSprite(hp)

    internal fun generateEnemyDestroyerPixmap(hp: Int): Pixmap =
        createEnemyDestroyerSprite(hp)

    internal fun generateEnemyClonePixmap(): Pixmap =
        createEnemyCloneSprite()

    internal fun generateAstronautPixmap(): Pixmap =
        createAstronautSprite()

    internal fun generateDebrisPixmap(): Pixmap =
        createDebrisSprite()

    internal fun generateDebrisGlowPixmap(): Pixmap =
        createDebrisGlowSprite()

    internal fun generateLaserPixmap(): Pixmap =
        createLaserSprite()

    internal fun generateLaserGlowPixmap(): Pixmap =
        createLaserGlowSprite()

    internal fun generateThrustPixmap(frame: Int): Pixmap =
        createThrustSprite(frame)

    internal fun generateExplosionPixmap(size: Int, frame: Int): Pixmap =
        createExplosionSprite(size, frame)

    internal fun generateParticlePixmap(): Pixmap =
        createParticleSprite()

    // ── Atlas building ───────────────────────────────────────────

    /**
     * Build all sprite pixmaps keyed by atlas name.
     * Call [dispose] on each pixmap after packing.
     */
    private fun buildSpritePixmaps(): Map<String, Pixmap> {
        val map = mutableMapOf<String, Pixmap>()

        // Ship (3 damage states)
        for (state in DamageState.entries) {
            if (state == DamageState.DESTROYED) continue
            map["ship_${state.name.lowercase()}"] = createShipSprite(state)
        }

        // Thrust (2-frame flicker)
        for (i in 0 until Constants.SPRITE_THRUST_FRAMES) {
            map["thrust_$i"] = createThrustSprite(i)
        }

        // Asteroids (3 sizes × HP states)
        val asteroidDefs = listOf(
            Triple("large", Constants.SPRITE_ASTEROID_LARGE, 3),
            Triple("medium", Constants.SPRITE_ASTEROID_MEDIUM, 2),
            Triple("small", Constants.SPRITE_ASTEROID_SMALL, 1)
        )
        for ((sizeName, sizePx, maxHp) in asteroidDefs) {
            for (hp in 1..maxHp) {
                map["asteroid_${sizeName}_$hp"] = createAsteroidSprite(sizePx, hp, maxHp)
            }
        }

        // Enemies (4 types × damage states)
        map["enemy_fighter_1"] = createEnemyFighterSprite()
        map["enemy_frigate_1"] = createEnemyFrigateSprite(2)
        map["enemy_frigate_2"] = createEnemyFrigateSprite(1)
        map["enemy_destroyer_1"] = createEnemyDestroyerSprite(3)
        map["enemy_destroyer_2"] = createEnemyDestroyerSprite(2)
        map["enemy_destroyer_3"] = createEnemyDestroyerSprite(1)
        map["enemy_clone"] = createEnemyCloneSprite()

        // Astronaut (3 states)
        map["astro_floating"] = createAstronautSprite()
        map["astro_rescued"] = createAstronautRescuedSprite()
        map["astro_dead"] = createAstronautDeadSprite()

        // Debris
        map["debris"] = createDebrisSprite()
        map["debris_glow"] = createDebrisGlowSprite()

        // Laser
        map["laser"] = createLaserSprite()
        map["laser_glow"] = createLaserGlowSprite()

        // Particle (white circle for particle effects)
        map["particle"] = createParticleSprite()

        // Explosions (3 sizes × 6 frames)
        val explosionDefs = listOf(
            "small" to Constants.SPRITE_EXPLOSION_SMALL,
            "medium" to Constants.SPRITE_EXPLOSION_MEDIUM,
            "large" to Constants.SPRITE_EXPLOSION_LARGE
        )
        for ((sizeName, sizePx) in explosionDefs) {
            for (frame in 0 until Constants.SPRITE_EXPLOSION_FRAMES) {
                map["explosion_${sizeName}_$frame"] = createExplosionSprite(sizePx, frame)
            }
        }

        return map
    }

    // ── Ship sprite ──────────────────────────────────────────────

    private fun createShipSprite(state: DamageState): Pixmap {
        val s = Constants.SPRITE_SHIP
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val h = s * 0.3f  // half-size of triangle

        // Ship body — triangle pointing right
        pix.setColor(when (state) {
            DamageState.PRISTINE -> Color(0.2f, 0.5f, 1f, 1f)
            DamageState.DAMAGED  -> Color(0.4f, 0.3f, 0.8f, 1f)
            DamageState.CRITICAL -> Color(0.8f, 0.25f, 0.25f, 1f)
            DamageState.DESTROYED -> Color(0.3f, 0.3f, 0.3f, 1f)
        })
        fillTriangle(pix,
            (cx + h * 1.2f).roundToInt(), cy.roundToInt(),          // nose
            (cx - h * 0.8f).roundToInt(), (cy - h).roundToInt(),    // top-left
            (cx - h * 0.8f).roundToInt(), (cy + h).roundToInt()     // bottom-left
        )

        // Damaged: red slash marks
        if (state == DamageState.DAMAGED || state == DamageState.CRITICAL) {
            pix.setColor(1f, 0.2f, 0.2f, 0.5f)
            val off = (s * 0.12f).roundToInt()
            pix.drawLine(cx.roundToInt() - off, (cy - off).roundToInt(),
                         cx.roundToInt() + off, (cy + off).roundToInt())
            pix.drawLine(cx.roundToInt() + off, (cy - off).roundToInt(),
                         cx.roundToInt() - off, (cy + off).roundToInt())
        }

        // Critical: more damage marks, slight alpha
        if (state == DamageState.CRITICAL) {
            pix.setColor(1f, 0f, 0f, 0.35f)
            pix.drawLine((cx - h * 0.5f).roundToInt(), (cy - h * 0.3f).roundToInt(),
                         (cx + h * 0.6f).roundToInt(), (cy + h * 0.5f).roundToInt())
            pix.drawLine((cx - h * 0.5f).roundToInt(), (cy + h * 0.3f).roundToInt(),
                         (cx + h * 0.6f).roundToInt(), (cy - h * 0.5f).roundToInt())
        }

        return pix
    }

    // ── Thrust sprite ────────────────────────────────────────────

    private fun createThrustSprite(frame: Int): Pixmap {
        val w = Constants.SPRITE_THRUST_WIDTH
        val h = Constants.SPRITE_THRUST_HEIGHT
        val pix = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = w / 2f
        val cy = h / 2f
        val intensity = if (frame == 0) 0.7f else 1.0f

        // Outer flame (orange)
        pix.setColor(1f, 0.5f, 0f, intensity * 0.8f)
        fillTriangle(pix,
            cx.roundToInt(), (cy + h * 0.35f).roundToInt(),
            (cx - w * 0.4f).roundToInt(), cy.roundToInt(),
            cx.roundToInt(), (cy - h * 0.35f).roundToInt()
        )

        // Inner flame (white-yellow)
        pix.setColor(1f, 0.9f, 0.2f, intensity)
        fillTriangle(pix,
            cx.roundToInt(), (cy + h * 0.2f).roundToInt(),
            (cx - w * 0.2f).roundToInt(), cy.roundToInt(),
            cx.roundToInt(), (cy - h * 0.2f).roundToInt()
        )

        return pix
    }

    // ── Asteroid sprite ──────────────────────────────────────────

    private fun createAsteroidSprite(sizePx: Int, hp: Int, maxHp: Int): Pixmap {
        val pix = Pixmap(sizePx, sizePx, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val r = sizePx * 0.42f  // circle radius

        // Base color varies by damage
        val damageRatio = 1f - hp.toFloat() / maxHp.toFloat()
        val grayBase = 0.35f + damageRatio * 0.25f
        pix.setColor(1f * grayBase, 0.4f * grayBase + 0.2f, 0.35f * grayBase + 0.1f, 1f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), r.roundToInt())

        // Surface detail — random crack lines based on damage
        pix.setColor(0f, 0f, 0f, 0.3f + damageRatio * 0.4f)
        val crackCount = (2 + hp).coerceAtLeast(1)
        for (i in 0 until crackCount) {
            val angle = i * 1.8f + 0.3f
            val x1 = (cx + kotlin.math.cos(angle.toDouble()).toFloat() * r * 0.3f).roundToInt()
            val y1 = (cy + kotlin.math.sin(angle.toDouble()).toFloat() * r * 0.3f).roundToInt()
            val x2 = (cx + kotlin.math.cos((angle + 0.8f).toDouble()).toFloat() * r * 0.8f).roundToInt()
            val y2 = (cy + kotlin.math.sin((angle + 0.8f).toDouble()).toFloat() * r * 0.8f).roundToInt()
            pix.drawLine(x1, y1, x2, y2)
        }

        // Heavily damaged: extra visible cracks
        if (damageRatio > 0.5f) {
            pix.setColor(0f, 0f, 0f, 0.5f)
            for (i in 0 until 3) {
                val angle = i * 2.1f + 1.2f
                val x1 = (cx + kotlin.math.cos(angle.toDouble()).toFloat() * r * 0.4f).roundToInt()
                val y1 = (cy + kotlin.math.sin(angle.toDouble()).toFloat() * r * 0.4f).roundToInt()
                val x2 = (cx + kotlin.math.cos((angle + 0.5f).toDouble()).toFloat() * r * 0.7f).roundToInt()
                val y2 = (cy + kotlin.math.sin((angle + 0.5f).toDouble()).toFloat() * r * 0.7f).roundToInt()
                pix.drawLine(x1, y1, x2, y2)
            }
        }

        return pix
    }

    // ── Enemy: LightFighter ──────────────────────────────────────

    private fun createEnemyFighterSprite(): Pixmap {
        val s = Constants.SPRITE_ENEMY_FIGHTER
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val h = s * 0.35f

        // Red triangle pointing left
        pix.setColor(0.9f, 0.15f, 0.15f, 1f)
        fillTriangle(pix,
            (cx - h * 1.2f).roundToInt(), cy.roundToInt(),         // nose (left)
            (cx + h * 0.8f).roundToInt(), (cy - h).roundToInt(),   // top-right
            (cx + h * 0.8f).roundToInt(), (cy + h).roundToInt()    // bottom-right
        )

        return pix
    }

    // ── Enemy: MediumFrigate ─────────────────────────────────────

    private fun createEnemyFrigateSprite(hp: Int): Pixmap {
        val s = Constants.SPRITE_ENEMY_FRIGATE
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val halfW = s * 0.35f
        val halfH = s * 0.22f

        // Orange rectangle body
        val color = if (hp >= 2) Color(1f, 0.6f, 0.1f, 1f)
                    else Color(0.8f, 0.4f, 0.05f, 1f)  // darker when damaged
        pix.setColor(color)
        pix.fillRectangle(
            (cx - halfW).roundToInt(),
            (cy - halfH).roundToInt(),
            (halfW * 2f).roundToInt(),
            (halfH * 2f).roundToInt()
        )

        // Damage marks
        if (hp < 2) {
            pix.setColor(1f, 0.2f, 0f, 0.4f)
            pix.drawLine((cx - halfW * 0.5f).roundToInt(), (cy - halfH * 0.5f).roundToInt(),
                         (cx + halfW * 0.5f).roundToInt(), (cy + halfH * 0.5f).roundToInt())
        }

        return pix
    }

    // ── Enemy: HeavyDestroyer ────────────────────────────────────

    private fun createEnemyDestroyerSprite(hp: Int): Pixmap {
        val s = Constants.SPRITE_ENEMY_DESTROYER
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.42f

        // Purple hexagon (approximate with circle + flat edges)
        val color = when {
            hp >= 3 -> Color(0.6f, 0.2f, 0.8f, 1f)
            hp == 2 -> Color(0.5f, 0.15f, 0.7f, 1f)
            else    -> Color(0.35f, 0.1f, 0.5f, 1f)
        }
        pix.setColor(color)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), r.roundToInt())

        // Hexagon outline (six sides)
        pix.setColor(0.3f, 0.1f, 0.4f, 0.6f)
        val vertices = (0 until 6).map { i ->
            val angle = Math.toRadians((i * 60f + 30f).toDouble())
            val vx = (cx + kotlin.math.cos(angle).toFloat() * r).roundToInt()
            val vy = (cy + kotlin.math.sin(angle).toFloat() * r).roundToInt()
            vx to vy
        }
        for (i in vertices.indices) {
            val (x1, y1) = vertices[i]
            val (x2, y2) = vertices[(i + 1) % vertices.size]
            pix.drawLine(x1, y1, x2, y2)
        }

        // Damage marks
        if (hp <= 2) {
            pix.setColor(1f, 0f, 0f, 0.3f)
            pix.drawLine(
                (cx - r * 0.4f).roundToInt(), (cy - r * 0.4f).roundToInt(),
                (cx + r * 0.4f).roundToInt(), (cy + r * 0.4f).roundToInt()
            )
        }
        if (hp <= 1) {
            pix.setColor(1f, 0f, 0f, 0.5f)
            pix.drawLine(
                (cx + r * 0.3f).roundToInt(), (cy - r * 0.5f).roundToInt(),
                (cx - r * 0.3f).roundToInt(), (cy + r * 0.5f).roundToInt()
            )
        }

        return pix
    }

    // ── Enemy: DarkClone ─────────────────────────────────────────

    private fun createEnemyCloneSprite(): Pixmap {
        val s = Constants.SPRITE_ENEMY_CLONE
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val h = s * 0.3f

        // Dark red ship triangle (pointing left — enemy direction)
        pix.setColor(0.6f, 0.1f, 0.1f, 1f)
        fillTriangle(pix,
            (cx - h * 1.2f).roundToInt(), cy.roundToInt(),          // nose (left)
            (cx + h * 0.8f).roundToInt(), (cy - h).roundToInt(),    // top-right
            (cx + h * 0.8f).roundToInt(), (cy + h).roundToInt()     // bottom-right
        )

        // Dark glow outline
        pix.setColor(0.3f, 0.05f, 0.05f, 0.6f)
        pix.drawLine((cx - h * 1.2f).roundToInt(), cy.roundToInt(),
                     (cx + h * 0.8f).roundToInt(), (cy - h).roundToInt())
        pix.drawLine((cx + h * 0.8f).roundToInt(), (cy - h).roundToInt(),
                     (cx + h * 0.8f).roundToInt(), (cy + h).roundToInt())
        pix.drawLine((cx + h * 0.8f).roundToInt(), (cy + h).roundToInt(),
                     (cx - h * 1.2f).roundToInt(), cy.roundToInt())

        return pix
    }

    // ── Astronaut sprites ────────────────────────────────────────

    private fun createAstronautSprite(): Pixmap {
        val s = Constants.SPRITE_ASTRONAUT
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val bodyR = s * 0.28f
        val helmetR = s * 0.35f

        // Helmet ring (white)
        pix.setColor(1f, 1f, 1f, 0.6f)
        pix.drawCircle(cx.roundToInt(), cy.roundToInt(), helmetR.roundToInt())

        // Body (green)
        pix.setColor(0.2f, 0.8f, 0.2f, 1f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), bodyR.roundToInt())

        return pix
    }

    private fun createAstronautRescuedSprite(): Pixmap {
        val s = Constants.SPRITE_ASTRONAUT
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val bodyR = s * 0.28f
        val helmetR = s * 0.35f

        // Helmet ring (brighter)
        pix.setColor(1f, 1f, 0.8f, 0.7f)
        pix.drawCircle(cx.roundToInt(), cy.roundToInt(), helmetR.roundToInt())

        // Body (brighter green)
        pix.setColor(0.3f, 1f, 0.3f, 0.9f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), bodyR.roundToInt())

        // Wave indicator (small yellow arc)
        pix.setColor(1f, 1f, 0.2f, 0.7f)
        val waveY = (cy + helmetR * 0.6f).roundToInt()
        for (dx in -2..2) {
            pix.drawPixel((cx + dx).roundToInt(), waveY)
        }

        return pix
    }

    private fun createAstronautDeadSprite(): Pixmap {
        val s = Constants.SPRITE_ASTRONAUT
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val bodyR = s * 0.28f
        val helmetR = s * 0.35f

        // Faded helmet
        pix.setColor(0.5f, 0.5f, 0.5f, 0.5f)
        pix.drawCircle(cx.roundToInt(), cy.roundToInt(), helmetR.roundToInt())

        // Faded red body (dead)
        pix.setColor(0.5f, 0.2f, 0.2f, 0.6f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), bodyR.roundToInt())

        // Cross (dead indicator)
        pix.setColor(0.3f, 0.1f, 0.1f, 0.5f)
        pix.drawLine((cx - bodyR * 0.4f).roundToInt(), cy.roundToInt(),
                     (cx + bodyR * 0.4f).roundToInt(), cy.roundToInt())
        pix.drawLine(cx.roundToInt(), (cy - bodyR * 0.4f).roundToInt(),
                     cx.roundToInt(), (cy + bodyR * 0.4f).roundToInt())

        return pix
    }

    // ── Debris sprites ───────────────────────────────────────────

    private fun createDebrisSprite(): Pixmap {
        val s = Constants.SPRITE_DEBRIS
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.35f

        // Golden circle
        pix.setColor(1f, 0.85f, 0f, 1f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), r.roundToInt())

        // Inner highlight
        pix.setColor(1f, 0.95f, 0.5f, 0.6f)
        pix.fillCircle((cx - r * 0.2f).roundToInt(), (cy + r * 0.2f).roundToInt(),
                       (r * 0.35f).roundToInt())

        return pix
    }

    private fun createDebrisGlowSprite(): Pixmap {
        val s = Constants.SPRITE_DEBRIS_GLOW
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val outerR = s * 0.45f

        // Radial glow (multiple concentric circles with decreasing alpha)
        for (i in 5 downTo 1) {
            val r = outerR * i / 5f
            val alpha = 0.3f * (1f - i.toFloat() / 6f)
            pix.setColor(1f, 0.85f, 0f, alpha)
            pix.drawCircle(cx.roundToInt(), cy.roundToInt(), r.roundToInt())
        }

        // Center dot
        pix.setColor(1f, 0.9f, 0.2f, 0.8f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (outerR * 0.15f).roundToInt())

        return pix
    }

    // ── Laser sprites ────────────────────────────────────────────

    private fun createLaserSprite(): Pixmap {
        val w = Constants.SPRITE_LASER_WIDTH
        val h = Constants.SPRITE_LASER_HEIGHT
        val pix = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = w / 2f
        val cy = h / 2f

        // Yellow rectangle
        pix.setColor(1f, 1f, 0.2f, 1f)
        pix.fillRectangle(0, (cy - 1f).roundToInt(), w, 3)

        // Bright center
        pix.setColor(1f, 1f, 0.8f, 1f)
        pix.fillRectangle((cx - 2f).roundToInt(), (cy - 1f).roundToInt(), 4, 3)

        return pix
    }

    private fun createLaserGlowSprite(): Pixmap {
        val w = Constants.SPRITE_LASER_GLOW_WIDTH
        val h = Constants.SPRITE_LASER_GLOW_HEIGHT
        val pix = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = w / 2f
        val cy = h / 2f

        // Diffuse glow
        pix.setColor(1f, 1f, 0.4f, 0.3f)
        pix.fillRectangle((cx - w * 0.35f).roundToInt(), 1, (w * 0.7f).roundToInt(), h - 2)

        // Inner glow
        pix.setColor(1f, 1f, 0.6f, 0.5f)
        pix.fillRectangle((cx - w * 0.2f).roundToInt(), (cy - 1f).roundToInt(), (w * 0.4f).roundToInt(), 3)

        return pix
    }

    // ── Explosion sprites ────────────────────────────────────────

    private fun createExplosionSprite(sizePx: Int, frame: Int): Pixmap {
        val pix = Pixmap(sizePx, sizePx, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val totalFrames = Constants.SPRITE_EXPLOSION_FRAMES  // 6
        val progress = frame.toFloat() / (totalFrames - 1).toFloat()

        // Explosion grows then shrinks
        val scale: Float
        val alpha: Float
        when {
            frame < 2 -> {  // Frames 0-1: growing
                scale = 0.3f + progress * 0.5f
                alpha = 0.6f + progress * 0.4f
            }
            frame < 4 -> {  // Frames 2-3: peak
                scale = 0.8f + (progress - 0.4f) * 0.3f
                alpha = 1f
            }
            else -> {  // Frames 4-5: fading
                scale = 1.1f - (progress - 0.6f) * 0.8f
                alpha = 1f - (progress - 0.6f) * 1.5f
            }
        }

        val r = sizePx * 0.4f * scale.coerceIn(0.1f, 1f)

        // Outer ring (orange)
        pix.setColor(1f, 0.5f, 0f, (alpha * 0.5f).coerceIn(0f, 1f))
        pix.drawCircle(cx.roundToInt(), cy.roundToInt(), r.roundToInt())

        // Middle (orange-red)
        pix.setColor(1f, 0.3f, 0f, alpha.coerceIn(0f, 1f))
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (r * 0.7f).roundToInt())

        // Core (white-yellow)
        pix.setColor(1f, 0.9f, 0.4f, (alpha * 0.8f).coerceIn(0f, 1f))
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (r * 0.35f).roundToInt())

        return pix
    }

    // ── Particle sprite ──────────────────────────────────────────

    private fun createParticleSprite(): Pixmap {
        val s = Constants.SPRITE_PARTICLE
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        // White filled circle — tinted at render time via batch colour
        pix.setColor(1f, 1f, 1f, 1f)
        pix.fillCircle(s / 2, s / 2, s / 2 - 1)

        return pix
    }

    // ── Atlas packing ────────────────────────────────────────────

    /**
     * Pack all pixmaps into a single [Pixmap] using a greedy shelf
     * packer, create a [Texture], and return a [SpriteAtlas].
     */
    private fun pack(sprites: Map<String, Pixmap>): SpriteAtlas {
        if (sprites.isEmpty()) throw IllegalStateException("No sprites to pack")

        val sorted = sprites.entries.sortedByDescending { it.value.height }
        val maxWidth = Constants.SPRITE_ATLAS_MAX_WIDTH

        // Shelf packing
        data class Shelf(val y: Int, val height: Int, var cursorX: Int)
        data class Slot(val x: Int, val y: Int, val w: Int, val h: Int)

        val shelves = mutableListOf<Shelf>()
        val slots = mutableMapOf<String, Slot>()
        var totalUsedHeight = 0

        for ((name, pix) in sorted) {
            val pw = pix.width
            val ph = pix.height
            var placed = false

            // Try existing shelves
            for (shelf in shelves) {
                if (shelf.height >= ph && shelf.cursorX + pw <= maxWidth) {
                    slots[name] = Slot(shelf.cursorX, shelf.y, pw, ph)
                    shelf.cursorX += pw
                    placed = true
                    break
                }
            }

            if (!placed) {
                // Create new shelf
                slots[name] = Slot(0, totalUsedHeight, pw, ph)
                shelves.add(Shelf(totalUsedHeight, ph, pw))
                totalUsedHeight += ph
            }
        }

        // Round height up to power of 2
        val texHeight = roundUpToPowerOf2(totalUsedHeight)

        // Create the packed pixmap
        val packed = Pixmap(maxWidth, texHeight, Pixmap.Format.RGBA8888)
        packed.setColor(0f, 0f, 0f, 0f)
        packed.fill()

        for ((name, slot) in slots) {
            val src = sprites[name]!!
            packed.drawPixmap(src, slot.x, slot.y)
            src.dispose()
        }

        val texture = Texture(packed)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        packed.dispose()

        // Build region map
        val regions = mutableMapOf<String, TextureRegion>()
        for ((name, slot) in slots) {
            regions[name] = TextureRegion(texture, slot.x, slot.y, slot.w, slot.h)
        }

        return SpriteAtlas(texture, regions)
    }

    // ── Helpers ──────────────────────────────────────────────────

    /**
     * Fill a triangle on a pixmap.
     * libGDX [Pixmap.fillTriangle] expects integer coordinates.
     */
    private fun fillTriangle(pix: Pixmap, x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int) {
        pix.fillTriangle(x1, y1, x2, y2, x3, y3)
    }

    /** Round an integer up to the nearest power of two. */
    private fun roundUpToPowerOf2(value: Int): Int {
        var v = value
        if (v <= 0) return 1
        v--
        v = v or (v shr 1)
        v = v or (v shr 2)
        v = v or (v shr 4)
        v = v or (v shr 8)
        v = v or (v shr 16)
        return v + 1
    }
}
