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
        createDebrisGearSprite()

    internal fun generateDebrisGearPixmap(): Pixmap =
        createDebrisGearSprite()

    internal fun generateDebrisAntennaPixmap(): Pixmap =
        createDebrisAntennaSprite()

    internal fun generateDebrisPanelPixmap(): Pixmap =
        createDebrisPanelSprite()

    internal fun generateDebrisGlowPixmap(): Pixmap =
        createDebrisGlowSprite()

    internal fun generateLaserPixmap(): Pixmap =
        createLaserSprite()

    internal fun generateLaserGlowPixmap(): Pixmap =
        createLaserGlowSprite()

    internal fun generateLaserPixmap(color: Color): Pixmap =
        createLaserSprite(color)

    internal fun generateLaserGlowPixmap(color: Color): Pixmap =
        createLaserGlowSprite(color)

    internal fun generateThrustPixmap(frame: Int): Pixmap =
        createThrustSprite(frame)

    internal fun generateExplosionPixmap(size: Int, frame: Int): Pixmap =
        createExplosionSprite(size, frame)

    internal fun generateParticlePixmap(): Pixmap =
        createParticleSprite()

    internal fun generateHeartPixmap(): Pixmap =
        createHeartSprite()

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

        // Asteroids (3 sizes × HP states × 3 mineral types)
        val asteroidDefs = listOf(
            Triple("large", Constants.SPRITE_ASTEROID_LARGE, 3),
            Triple("medium", Constants.SPRITE_ASTEROID_MEDIUM, 2),
            Triple("small", Constants.SPRITE_ASTEROID_SMALL, 1)
        )
        for ((sizeName, sizePx, maxHp) in asteroidDefs) {
            for (hp in 1..maxHp) {
                for (type in listOf("rock", "ice", "metal")) {
                    map["asteroid_${sizeName}_${type}_$hp"] = createAsteroidSprite(sizePx, hp, maxHp, type)
                }
            }
        }

        // Enemies (4 types × damage states)
        map["enemy_fighter_1"] = createEnemyFighterSprite(false)
        map["enemy_fighter_2"] = createEnemyFighterSprite(true)
        map["enemy_frigate_1"] = createEnemyFrigateSprite(2)
        map["enemy_frigate_2"] = createEnemyFrigateSprite(1)
        map["enemy_destroyer_1"] = createEnemyDestroyerSprite(3)
        map["enemy_destroyer_2"] = createEnemyDestroyerSprite(2)
        map["enemy_destroyer_3"] = createEnemyDestroyerSprite(1)
        map["enemy_clone"] = createEnemyCloneSprite()
        map["enemy_kamikaze"] = createEnemyKamikazeSprite()

        // Astronaut (3 states)
        map["astro_floating"] = createAstronautSprite()
        map["astro_rescued"] = createAstronautRescuedSprite()
        map["astro_dead"] = createAstronautDeadSprite()

        // Debris (3 shape variants)
        map["debris_gear"] = createDebrisGearSprite()
        map["debris_antenna"] = createDebrisAntennaSprite()
        map["debris_panel"] = createDebrisPanelSprite()
        map["debris_glow"] = createDebrisGlowSprite()

        // Laser (default yellow — backward compat)
        map["laser"] = createLaserSprite()
        map["laser_glow"] = createLaserGlowSprite()

        // Laser color variants (5 types × core + glow)
        val laserColors = listOf(
            "laser_player"   to Color(0.2f, 1.0f, 0.2f, 1f),
            "laser_fighter"  to Color(0.2f, 0.8f, 1.0f, 1f),
            "laser_frigate"  to Color(1.0f, 0.6f, 0.1f, 1f),
            "laser_destroyer" to Color(1.0f, 0.2f, 0.2f, 1f),
            "laser_clone"    to Color(0.8f, 0.2f, 0.8f, 1f)
        )
        for ((key, color) in laserColors) {
            map[key] = createLaserSprite(color)
            map["${key}_glow"] = createLaserGlowSprite(color)
        }

        // Particle (white circle for particle effects)
        map["particle"] = createParticleSprite()

        // Heart (HUD lives indicator)
        map["heart"] = createHeartSprite()

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
        val h = s * 0.35f

        // Engine glow (behind ship)
        pix.setColor(0.3f, 0.6f, 1f, 0.3f)
        pix.fillCircle((cx - h * 0.9f).roundToInt(), cy.roundToInt(), (h * 0.4f).roundToInt())
        pix.setColor(0.5f, 0.8f, 1f, 0.5f)
        pix.fillCircle((cx - h * 0.9f).roundToInt(), cy.roundToInt(), (h * 0.25f).roundToInt())

        // Main body (futuristic spaceship shape)
        val bodyColor = when (state) {
            DamageState.PRISTINE -> Color(0.3f, 0.6f, 0.95f, 1f)
            DamageState.DAMAGED -> Color(0.35f, 0.5f, 0.75f, 1f)
            DamageState.CRITICAL -> Color(0.4f, 0.4f, 0.55f, 1f)
            DamageState.DESTROYED -> Color(0.5f, 0.2f, 0.2f, 1f)
        }
        
        // Fuselage (elongated body)
        pix.setColor(bodyColor)
        pix.fillRectangle((cx - h * 0.6f).roundToInt(), (cy - h * 0.25f).roundToInt(), 
                         (h * 1.4f).roundToInt(), (h * 0.5f).roundToInt())
        
        // Nose cone (pointed front)
        fillTriangle(pix,
            (cx + h * 1.1f).roundToInt(), cy.roundToInt(),         // nose tip
            (cx + h * 0.5f).roundToInt(), (cy - h * 0.25f).roundToInt(),   // top
            (cx + h * 0.5f).roundToInt(), (cy + h * 0.25f).roundToInt()    // bottom
        )
        
        // Wings (swept back)
        pix.setColor(bodyColor.r * 0.8f, bodyColor.g * 0.8f, bodyColor.b * 0.8f, 1f)
        // Top wing
        fillTriangle(pix,
            (cx + h * 0.2f).roundToInt(), (cy - h * 0.25f).roundToInt(),
            (cx - h * 0.5f).roundToInt(), (cy - h * 0.25f).roundToInt(),
            (cx - h * 0.7f).roundToInt(), (cy - h * 0.9f).roundToInt()
        )
        // Bottom wing
        fillTriangle(pix,
            (cx + h * 0.2f).roundToInt(), (cy + h * 0.25f).roundToInt(),
            (cx - h * 0.5f).roundToInt(), (cy + h * 0.25f).roundToInt(),
            (cx - h * 0.7f).roundToInt(), (cy + h * 0.9f).roundToInt()
        )
        
        // Cockpit (glowing window)
        pix.setColor(0.7f, 0.9f, 1f, 0.9f)
        pix.fillCircle((cx + h * 0.4f).roundToInt(), cy.roundToInt(), (h * 0.18f).roundToInt())
        pix.setColor(1f, 1f, 1f, 0.6f)
        pix.fillCircle((cx + h * 0.45f).roundToInt(), (cy - h * 0.05f).roundToInt(), (h * 0.08f).roundToInt())
        
        // Engine nozzles
        pix.setColor(0.2f, 0.2f, 0.3f, 1f)
        pix.fillCircle((cx - h * 0.6f).roundToInt(), (cy - h * 0.15f).roundToInt(), (h * 0.12f).roundToInt())
        pix.fillCircle((cx - h * 0.6f).roundToInt(), (cy + h * 0.15f).roundToInt(), (h * 0.12f).roundToInt())

        // Panel lines (detail)
        pix.setColor(0.1f, 0.2f, 0.3f, 0.4f)
        pix.drawLine((cx - h * 0.3f).roundToInt(), (cy - h * 0.25f).roundToInt(),
                     (cx - h * 0.3f).roundToInt(), (cy + h * 0.25f).roundToInt())
        pix.drawLine((cx + h * 0.1f).roundToInt(), (cy - h * 0.25f).roundToInt(),
                     (cx + h * 0.1f).roundToInt(), (cy + h * 0.25f).roundToInt())

        // Damage marks
        if (state == DamageState.DAMAGED || state == DamageState.CRITICAL) {
            pix.setColor(0.1f, 0.1f, 0.1f, 0.7f)
            pix.drawLine((cx - h * 0.4f).roundToInt(), (cy - h * 0.3f).roundToInt(),
                         (cx + h * 0.1f).roundToInt(), (cy + h * 0.2f).roundToInt())
            pix.drawLine((cx - h * 0.2f).roundToInt(), (cy + h * 0.4f).roundToInt(),
                         (cx + h * 0.3f).roundToInt(), (cy - h * 0.1f).roundToInt())
        }
        if (state == DamageState.CRITICAL) {
            // Sparks/fire
            pix.setColor(1f, 0.5f, 0f, 0.8f)
            pix.fillCircle((cx - h * 0.3f).roundToInt(), (cy + h * 0.3f).roundToInt(), (h * 0.1f).roundToInt())
            pix.setColor(1f, 0.8f, 0.2f, 0.6f)
            pix.fillCircle((cx + h * 0.2f).roundToInt(), (cy - h * 0.35f).roundToInt(), (h * 0.08f).roundToInt())
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

    private fun createAsteroidSprite(sizePx: Int, hp: Int, maxHp: Int, type: String = "rock"): Pixmap {
        val pix = Pixmap(sizePx, sizePx, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val baseRadius = sizePx * 0.4f

        // Base colour palette by type
        val (baseR, baseG, baseB) = when (type) {
            "ice"   -> Triple(0.50f, 0.60f, 0.75f) // blue-white
            "metal" -> Triple(0.55f, 0.55f, 0.60f) // silver-grey
            else    -> Triple(0.55f, 0.45f, 0.35f) // rock: brown-grey
        }
        val (darkR, darkG, darkB) = when (type) {
            "ice"   -> Triple(0.15f, 0.25f, 0.40f)
            "metal" -> Triple(0.25f, 0.25f, 0.30f)
            else    -> Triple(0.20f, 0.15f, 0.12f)
        }
        val (lightR, lightG, lightB) = when (type) {
            "ice"   -> Triple(0.70f, 0.80f, 0.95f)
            "metal" -> Triple(0.70f, 0.70f, 0.75f)
            else    -> Triple(0.60f, 0.50f, 0.45f)
        }
        val (crackR, crackG, crackB) = when (type) {
            "ice"   -> Triple(0.80f, 0.85f, 0.95f)
            "metal" -> Triple(0.10f, 0.10f, 0.12f)
            else    -> Triple(0.10f, 0.08f, 0.05f)
        }

        // Damage ratio affects brightness
        val damageRatio = 1f - hp.toFloat() / maxHp.toFloat()
        val damageBright = 1f + damageRatio * 0.25f
        
        // Create irregular shape by drawing multiple overlapping circles
        // Main body
        pix.setColor(
            (baseR * damageBright).coerceIn(0f, 1f),
            (baseG * damageBright).coerceIn(0f, 1f),
            (baseB * damageBright).coerceIn(0f, 1f),
            1f
        )
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), baseRadius.roundToInt())
        
        // Add irregular bumps around the edge
        val bumpAngles = listOf(0f, 0.8f, 1.6f, 2.4f, 3.2f, 4.0f, 4.8f, 5.6f)
        for (angle in bumpAngles) {
            val bumpX = (cx + kotlin.math.cos(angle.toDouble()).toFloat() * baseRadius * 0.7f).roundToInt()
            val bumpY = (cy + kotlin.math.sin(angle.toDouble()).toFloat() * baseRadius * 0.7f).roundToInt()
            val bumpSize = (baseRadius * 0.35f).roundToInt()
            pix.fillCircle(bumpX, bumpY, bumpSize)
        }
        
        // Add darker areas for depth (craters/shadows)
        pix.setColor(darkR, darkG, darkB, 0.6f)
        pix.fillCircle((cx - baseRadius * 0.3f).roundToInt(), (cy - baseRadius * 0.2f).roundToInt(),
                        (baseRadius * 0.25f).roundToInt())
        pix.fillCircle((cx + baseRadius * 0.2f).roundToInt(), (cy + baseRadius * 0.3f).roundToInt(),
                        (baseRadius * 0.2f).roundToInt())

        // Add lighter highlights
        pix.setColor(lightR, lightG, lightB, 0.4f)
        pix.fillCircle((cx + baseRadius * 0.1f).roundToInt(), (cy - baseRadius * 0.3f).roundToInt(), 
                        (baseRadius * 0.15f).roundToInt())
        
        // Surface detail — random crack lines based on damage
        pix.setColor(crackR, crackG, crackB, 0.5f + damageRatio * 0.3f)
        val crackCount = (2 + hp).coerceAtLeast(1)
        for (i in 0 until crackCount) {
            val angle = i * 1.8f + 0.3f
            val x1 = (cx + kotlin.math.cos(angle.toDouble()).toFloat() * baseRadius * 0.3f).roundToInt()
            val y1 = (cy + kotlin.math.sin(angle.toDouble()).toFloat() * baseRadius * 0.3f).roundToInt()
            val x2 = (cx + kotlin.math.cos((angle + 0.8f).toDouble()).toFloat() * baseRadius * 0.8f).roundToInt()
            val y2 = (cy + kotlin.math.sin((angle + 0.8f).toDouble()).toFloat() * baseRadius * 0.8f).roundToInt()
            pix.drawLine(x1, y1, x2, y2)
        }

        // Heavily damaged: extra visible cracks and glowing cracks
        if (damageRatio > 0.5f) {
            pix.setColor(0.05f, 0.05f, 0.05f, 0.7f)
            for (i in 0 until 4) {
                val angle = i * 1.5f + 0.8f
                val x1 = (cx + kotlin.math.cos(angle.toDouble()).toFloat() * baseRadius * 0.2f).roundToInt()
                val y1 = (cy + kotlin.math.sin(angle.toDouble()).toFloat() * baseRadius * 0.2f).roundToInt()
                val x2 = (cx + kotlin.math.cos((angle + 0.6f).toDouble()).toFloat() * baseRadius * 0.9f).roundToInt()
                val y2 = (cy + kotlin.math.sin((angle + 0.6f).toDouble()).toFloat() * baseRadius * 0.9f).roundToInt()
                pix.drawLine(x1, y1, x2, y2)
            }
            
            // Glowing cracks for heavily damaged
            pix.setColor(1f, 0.4f, 0.1f, 0.3f)
            pix.drawLine((cx - baseRadius * 0.5f).roundToInt(), cy.roundToInt(),
                         (cx + baseRadius * 0.5f).roundToInt(), cy.roundToInt())
        }

        return pix
    }

    // ── Enemy: LightFighter ──────────────────────────────────────

    private fun createEnemyFighterSprite(damaged: Boolean = false): Pixmap {
        val s = Constants.SPRITE_ENEMY_FIGHTER
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val h = s * 0.35f

        // Engine exhaust glow (layered for realistic thruster look)
        pix.setColor(1f, 0.3f, 0.1f, 0.25f)
        pix.fillCircle((cx + h * 0.85f).roundToInt(), cy.roundToInt(), (h * 0.5f).roundToInt())
        pix.setColor(1f, 0.5f, 0.2f, 0.35f)
        pix.fillCircle((cx + h * 0.85f).roundToInt(), cy.roundToInt(), (h * 0.35f).roundToInt())
        pix.setColor(1f, 0.7f, 0.3f, 0.5f)
        pix.fillCircle((cx + h * 0.85f).roundToInt(), cy.roundToInt(), (h * 0.2f).roundToInt())

        // Main fuselage (aggressive angular shape)
        pix.setColor(0.8f, 0.15f, 0.15f, 1f)
        pix.fillRectangle((cx - h * 0.5f).roundToInt(), (cy - h * 0.2f).roundToInt(),
                         (h * 1.2f).roundToInt(), (h * 0.4f).roundToInt())

        // Pointed nose cone (more aggressive angle)
        fillTriangle(pix,
            (cx - h * 1.15f).roundToInt(), cy.roundToInt(),         // nose tip
            (cx - h * 0.5f).roundToInt(), (cy - h * 0.2f).roundToInt(),
            (cx - h * 0.5f).roundToInt(), (cy + h * 0.2f).roundToInt()
        )

        // Aggressive swept-back wings (sharp angles)
        pix.setColor(0.7f, 0.1f, 0.1f, 1f)
        // Top wing
        fillTriangle(pix,
            (cx - h * 0.2f).roundToInt(), (cy - h * 0.2f).roundToInt(),
            (cx + h * 0.4f).roundToInt(), (cy - h * 0.2f).roundToInt(),
            (cx + h * 0.6f).roundToInt(), (cy - h * 0.8f).roundToInt()
        )
        // Bottom wing
        fillTriangle(pix,
            (cx - h * 0.2f).roundToInt(), (cy + h * 0.2f).roundToInt(),
            (cx + h * 0.4f).roundToInt(), (cy + h * 0.2f).roundToInt(),
            (cx + h * 0.6f).roundToInt(), (cy + h * 0.8f).roundToInt()
        )

        // Wing struts (structural detail lines)
        pix.setColor(0.5f, 0.08f, 0.08f, 0.6f)
        pix.drawLine((cx - h * 0.1f).roundToInt(), (cy - h * 0.2f).roundToInt(),
                     (cx + h * 0.5f).roundToInt(), (cy - h * 0.6f).roundToInt())
        pix.drawLine((cx - h * 0.1f).roundToInt(), (cy + h * 0.2f).roundToInt(),
                     (cx + h * 0.5f).roundToInt(), (cy + h * 0.6f).roundToInt())

        // Wingtip markers (navigation lights)
        pix.setColor(0.3f, 0.02f, 0.02f, 1f)
        pix.fillCircle((cx + h * 0.6f).roundToInt(), (cy - h * 0.8f).roundToInt(), 2)
        pix.fillCircle((cx + h * 0.6f).roundToInt(), (cy + h * 0.8f).roundToInt(), 2)

        // Cockpit (menacing red/orange with glare)
        pix.setColor(1f, 0.4f, 0.1f, 0.9f)
        pix.fillCircle((cx - h * 0.3f).roundToInt(), cy.roundToInt(), (h * 0.15f).roundToInt())
        pix.setColor(1f, 0.7f, 0.4f, 0.5f)
        pix.fillCircle((cx - h * 0.25f).roundToInt(), (cy - h * 0.03f).roundToInt(), (h * 0.07f).roundToInt())

        // Engine nozzles (dark recess with inner glow)
        pix.setColor(0.2f, 0.05f, 0.05f, 1f)
        pix.fillCircle((cx + h * 0.6f).roundToInt(), (cy - h * 0.12f).roundToInt(), (h * 0.1f).roundToInt())
        pix.fillCircle((cx + h * 0.6f).roundToInt(), (cy + h * 0.12f).roundToInt(), (h * 0.1f).roundToInt())
        // Nozzle inner glow (hot exhaust inside)
        pix.setColor(1f, 0.5f, 0.1f, 0.7f)
        pix.fillCircle((cx + h * 0.6f).roundToInt(), (cy - h * 0.12f).roundToInt(), (h * 0.05f).roundToInt())
        pix.fillCircle((cx + h * 0.6f).roundToInt(), (cy + h * 0.12f).roundToInt(), (h * 0.05f).roundToInt())

        // Weapon pods on wing struts
        pix.setColor(0.5f, 0.1f, 0.1f, 1f)
        pix.fillCircle((cx + h * 0.25f).roundToInt(), (cy - h * 0.5f).roundToInt(), (h * 0.12f).roundToInt())
        pix.fillCircle((cx + h * 0.25f).roundToInt(), (cy + h * 0.5f).roundToInt(), (h * 0.12f).roundToInt())
        // Pod highlights
        pix.setColor(0.65f, 0.15f, 0.15f, 0.7f)
        pix.fillCircle((cx + h * 0.25f).roundToInt(), (cy - h * 0.52f).roundToInt(), (h * 0.06f).roundToInt())
        pix.fillCircle((cx + h * 0.25f).roundToInt(), (cy + h * 0.52f).roundToInt(), (h * 0.06f).roundToInt())

        // Panel lines along fuselage
        pix.setColor(0.3f, 0.05f, 0.05f, 0.5f)
        pix.drawLine((cx - h * 0.3f).roundToInt(), (cy - h * 0.2f).roundToInt(),
                     (cx - h * 0.3f).roundToInt(), (cy + h * 0.2f).roundToInt())
        pix.drawLine((cx + h * 0.1f).roundToInt(), (cy - h * 0.2f).roundToInt(),
                     (cx + h * 0.1f).roundToInt(), (cy + h * 0.2f).roundToInt())

        // Damage overlay (cracks, scorch marks)
        if (damaged) {
            val rng = java.util.Random(42)
            // Scorch marks
            pix.setColor(0.15f, 0.05f, 0.05f, 0.5f)
            pix.fillCircle((cx + rng.nextInt((h * 0.6f).toInt()) - (h * 0.3f).toInt()).roundToInt(),
                           (cy + rng.nextInt((h * 0.6f).toInt()) - (h * 0.3f).toInt()).roundToInt(),
                           (h * 0.15f).roundToInt())
            pix.fillCircle((cx + rng.nextInt((h * 0.8f).toInt()) - (h * 0.4f).toInt()).roundToInt(),
                           (cy + rng.nextInt((h * 0.4f).toInt()) - (h * 0.2f).toInt()).roundToInt(),
                           (h * 0.08f).roundToInt())
            // Crack lines
            pix.setColor(0.1f, 0.02f, 0.02f, 0.7f)
            pix.drawLine(
                (cx - h * 0.4f).roundToInt(), (cy - h * 0.3f).roundToInt(),
                (cx + h * 0.3f).roundToInt(), (cy + h * 0.1f).roundToInt())
            pix.drawLine(
                (cx + h * 0.1f).roundToInt(), (cy + h * 0.4f).roundToInt(),
                (cx + h * 0.5f).roundToInt(), (cy - h * 0.45f).roundToInt())
            // Wing scorch
            pix.setColor(0.25f, 0.05f, 0.05f, 0.4f)
            pix.fillCircle((cx + h * 0.5f).roundToInt(), (cy - h * 0.6f).roundToInt(),
                          (h * 0.2f).roundToInt())
        }

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
        val halfW = s * 0.4f
        val halfH = s * 0.25f

        // Engine exhaust glow (layered orange)
        pix.setColor(1f, 0.4f, 0.1f, 0.2f)
        pix.fillCircle((cx + halfW * 0.95f).roundToInt(), cy.roundToInt(), (halfH * 0.8f).roundToInt())
        pix.setColor(1f, 0.6f, 0.2f, 0.35f)
        pix.fillCircle((cx + halfW * 0.9f).roundToInt(), cy.roundToInt(), (halfH * 0.5f).roundToInt())
        pix.setColor(1f, 0.8f, 0.3f, 0.5f)
        pix.fillCircle((cx + halfW * 0.9f).roundToInt(), cy.roundToInt(), (halfH * 0.3f).roundToInt())

        // Main body (larger, more imposing)
        val bodyColor = if (hp >= 2) Color(1f, 0.6f, 0.1f, 1f)
                        else Color(0.7f, 0.4f, 0.05f, 1f)  // darker when damaged
        pix.setColor(bodyColor)

        // Central hull
        pix.fillRectangle((cx - halfW * 0.8f).roundToInt(), (cy - halfH * 0.6f).roundToInt(),
                         (halfW * 1.6f).roundToInt(), (halfH * 1.2f).roundToInt())

        // Front section (pointed nose cone)
        fillTriangle(pix,
            (cx - halfW * 1.15f).roundToInt(), cy.roundToInt(),
            (cx - halfW * 0.8f).roundToInt(), (cy - halfH * 0.6f).roundToInt(),
            (cx - halfW * 0.8f).roundToInt(), (cy + halfH * 0.6f).roundToInt()
        )

        // Rear engine block
        pix.fillRectangle((cx + halfW * 0.4f).roundToInt(), (cy - halfH * 0.8f).roundToInt(),
                         (halfW * 0.6f).roundToInt(), (halfH * 1.6f).roundToInt())

        // Engine nozzles (dark rectangles with inner glow)
        pix.setColor(0.2f, 0.1f, 0.02f, 1f)
        pix.fillRectangle((cx + halfW * 0.7f).roundToInt(), (cy - halfH * 0.5f).roundToInt(),
                          (halfW * 0.2f).roundToInt(), (halfH * 0.3f).roundToInt())
        pix.fillRectangle((cx + halfW * 0.7f).roundToInt(), (cy + halfH * 0.2f).roundToInt(),
                          (halfW * 0.2f).roundToInt(), (halfH * 0.3f).roundToInt())
        // Nozzle inner glow
        pix.setColor(1f, 0.6f, 0.1f, 0.6f)
        pix.fillRectangle((cx + halfW * 0.72f).roundToInt(), (cy - halfH * 0.45f).roundToInt(),
                          (halfW * 0.14f).roundToInt(), (halfH * 0.2f).roundToInt())
        pix.fillRectangle((cx + halfW * 0.72f).roundToInt(), (cy + halfH * 0.25f).roundToInt(),
                          (halfW * 0.14f).roundToInt(), (halfH * 0.2f).roundToInt())

        // Armor panels (darker sections with structural detail)
        pix.setColor(bodyColor.r * 0.7f, bodyColor.g * 0.7f, bodyColor.b * 0.7f, 1f)
        pix.fillRectangle((cx - halfW * 0.3f).roundToInt(), (cy - halfH * 0.4f).roundToInt(),
                         (halfW * 0.6f).roundToInt(), (halfH * 0.8f).roundToInt())

        // Panel lines on armor
        pix.setColor(bodyColor.r * 0.5f, bodyColor.g * 0.5f, bodyColor.b * 0.5f, 0.5f)
        pix.drawLine((cx - halfW * 0.1f).roundToInt(), (cy - halfH * 0.4f).roundToInt(),
                     (cx - halfW * 0.1f).roundToInt(), (cy + halfH * 0.4f).roundToInt())
        pix.drawLine((cx + halfW * 0.1f).roundToInt(), (cy - halfH * 0.4f).roundToInt(),
                     (cx + halfW * 0.1f).roundToInt(), (cy + halfH * 0.4f).roundToInt())

        // Weapon turrets with barrels
        val turretPositions = listOf(
            (cx - halfW * 0.5f) to (cy - halfH * 0.9f),
            (cx - halfW * 0.5f) to (cy + halfH * 0.9f),
            (cx + halfW * 0.2f) to (cy - halfH * 0.9f),
            (cx + halfW * 0.2f) to (cy + halfH * 0.9f)
        )
        pix.setColor(0.4f, 0.2f, 0.05f, 1f)
        for ((tx, ty) in turretPositions) {
            pix.fillCircle(tx.roundToInt(), ty.roundToInt(), (halfH * 0.2f).roundToInt())
            // Barrel pointing forward
            pix.fillRectangle((tx - halfW * 0.1f).roundToInt(), (ty - 1).roundToInt(),
                             (halfW * 0.15f).roundToInt(), 3)
        }

        // Bridge/cockpit with detail
        pix.setColor(1f, 0.8f, 0.3f, 0.9f)
        pix.fillCircle((cx - halfW * 0.6f).roundToInt(), cy.roundToInt(), (halfH * 0.25f).roundToInt())
        pix.setColor(1f, 0.9f, 0.5f, 0.5f)
        pix.fillCircle((cx - halfW * 0.55f).roundToInt(), (cy - halfH * 0.04f).roundToInt(), (halfH * 0.1f).roundToInt())

        // Damage marks (more severe when hp < 2)
        if (hp < 2) {
            // Dark scorch / hull breach
            pix.setColor(0.15f, 0.08f, 0.02f, 0.8f)
            pix.fillCircle((cx - halfW * 0.3f).roundToInt(), (cy + halfH * 0.3f).roundToInt(), (halfH * 0.3f).roundToInt())

            // Hull cracks
            pix.setColor(0.2f, 0.1f, 0.05f, 0.7f)
            pix.drawLine((cx - halfW * 0.4f).roundToInt(), (cy - halfH * 0.3f).roundToInt(),
                         (cx + halfW * 0.3f).roundToInt(), (cy + halfH * 0.4f).roundToInt())
            pix.drawLine((cx - halfW * 0.2f).roundToInt(), (cy + halfH * 0.5f).roundToInt(),
                         (cx + halfW * 0.4f).roundToInt(), (cy - halfH * 0.2f).roundToInt())
            pix.drawLine((cx + halfW * 0.1f).roundToInt(), (cy - halfH * 0.1f).roundToInt(),
                         (cx + halfW * 0.3f).roundToInt(), (cy + halfH * 0.2f).roundToInt())

            // Fire/sparks from damage
            pix.setColor(1f, 0.4f, 0f, 0.7f)
            pix.fillCircle((cx + halfW * 0.1f).roundToInt(), (cy - halfH * 0.4f).roundToInt(), (halfH * 0.15f).roundToInt())
            pix.setColor(1f, 0.8f, 0.2f, 0.5f)
            pix.fillCircle((cx + halfW * 0.12f).roundToInt(), (cy - halfH * 0.38f).roundToInt(), (halfH * 0.08f).roundToInt())
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

        // Engine exhaust glow (layered purple)
        pix.setColor(0.5f, 0.15f, 0.7f, 0.2f)
        pix.fillCircle((cx + r * 0.85f).roundToInt(), cy.roundToInt(), (r * 0.65f).roundToInt())
        pix.setColor(0.7f, 0.25f, 0.9f, 0.3f)
        pix.fillCircle((cx + r * 0.85f).roundToInt(), cy.roundToInt(), (r * 0.45f).roundToInt())
        pix.setColor(0.9f, 0.4f, 1f, 0.4f)
        pix.fillCircle((cx + r * 0.85f).roundToInt(), cy.roundToInt(), (r * 0.25f).roundToInt())

        // Main body (massive hexagonal shape)
        val color = when {
            hp >= 3 -> Color(0.6f, 0.2f, 0.8f, 1f)
            hp == 2 -> Color(0.5f, 0.15f, 0.7f, 1f)
            else    -> Color(0.35f, 0.1f, 0.5f, 1f)
        }
        pix.setColor(color)

        // Central hexagon (6 sides)
        val vertices = (0 until 6).map { i ->
            val angle = Math.toRadians((i * 60f + 30f).toDouble())
            val vx = (cx + kotlin.math.cos(angle).toFloat() * r).roundToInt()
            val vy = (cy + kotlin.math.sin(angle).toFloat() * r).roundToInt()
            vx to vy
        }

        // Fill hexagon by drawing triangles from center
        for (i in vertices.indices) {
            val (x1, y1) = vertices[i]
            val (x2, y2) = vertices[(i + 1) % vertices.size]
            fillTriangle(pix, cx.roundToInt(), cy.roundToInt(), x1, y1, x2, y2)
        }

        // Inner structural ring (darker)
        pix.setColor(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, 1f)
        val innerVertices = (0 until 6).map { i ->
            val angle = Math.toRadians((i * 60f + 30f).toDouble())
            val vx = (cx + kotlin.math.cos(angle).toFloat() * r * 0.7f).roundToInt()
            val vy = (cy + kotlin.math.sin(angle).toFloat() * r * 0.7f).roundToInt()
            vx to vy
        }
        for (i in innerVertices.indices) {
            val (x1, y1) = innerVertices[i]
            val (x2, y2) = innerVertices[(i + 1) % innerVertices.size]
            fillTriangle(pix, cx.roundToInt(), cy.roundToInt(), x1, y1, x2, y2)
        }

        // Armor plating (darker center circle)
        pix.setColor(color.r * 0.6f, color.g * 0.6f, color.b * 0.6f, 1f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (r * 0.5f).roundToInt())

        // Structural spoke lines from center to hex edges
        pix.setColor(color.r * 0.4f, color.g * 0.4f, color.b * 0.4f, 0.6f)
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60f + 30f).toDouble())
            val ex = (cx + kotlin.math.cos(angle).toFloat() * r * 0.95f).roundToInt()
            val ey = (cy + kotlin.math.sin(angle).toFloat() * r * 0.95f).roundToInt()
            pix.drawLine(cx.roundToInt(), cy.roundToInt(), ex, ey)
        }

        // Weapon batteries (6 turrets around the edge)
        pix.setColor(0.3f, 0.1f, 0.4f, 1f)
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60f).toDouble())
            val wx = (cx + kotlin.math.cos(angle).toFloat() * r * 0.75f).roundToInt()
            val wy = (cy + kotlin.math.sin(angle).toFloat() * r * 0.75f).roundToInt()
            pix.fillCircle(wx, wy, (r * 0.15f).roundToInt())
            // Turret barrel
            val bx = (cx + kotlin.math.cos(angle).toFloat() * r * 0.95f).roundToInt()
            val by = (cy + kotlin.math.sin(angle).toFloat() * r * 0.95f).roundToInt()
            pix.drawLine(wx, wy, bx, by)
        }

        // Central bridge/command center
        pix.setColor(0.8f, 0.4f, 1f, 0.9f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (r * 0.25f).roundToInt())

        // Glowing core
        pix.setColor(1f, 0.6f, 1f, 0.7f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (r * 0.15f).roundToInt())
        pix.setColor(1f, 0.8f, 1f, 0.4f)
        pix.fillCircle((cx + r * 0.05f).roundToInt(), (cy - r * 0.05f).roundToInt(), (r * 0.07f).roundToInt())

        // Damage marks (hp <= 2)
        if (hp <= 2) {
            pix.setColor(0.15f, 0.05f, 0.2f, 0.7f)
            pix.drawLine(
                (cx - r * 0.4f).roundToInt(), (cy - r * 0.4f).roundToInt(),
                (cx + r * 0.4f).roundToInt(), (cy + r * 0.4f).roundToInt()
            )
            pix.drawLine(
                (cx + r * 0.3f).roundToInt(), (cy - r * 0.5f).roundToInt(),
                (cx - r * 0.3f).roundToInt(), (cy + r * 0.5f).roundToInt()
            )
            // Hull breach scorch
            if (hp <= 1) {
                pix.setColor(0.1f, 0.03f, 0.15f, 0.6f)
                pix.fillCircle((cx + r * 0.2f).roundToInt(), (cy - r * 0.3f).roundToInt(), (r * 0.2f).roundToInt())
            }
        }
        if (hp <= 1) {
            // Heavy damage - fire and exposed internals
            pix.setColor(1f, 0.3f, 0f, 0.6f)
            pix.fillCircle((cx - r * 0.3f).roundToInt(), (cy + r * 0.2f).roundToInt(), (r * 0.15f).roundToInt())
            pix.setColor(1f, 0.6f, 0f, 0.5f)
            pix.fillCircle((cx - r * 0.3f).roundToInt(), (cy + r * 0.22f).roundToInt(), (r * 0.08f).roundToInt())
            pix.setColor(1f, 0.3f, 0f, 0.6f)
            pix.fillCircle((cx + r * 0.4f).roundToInt(), (cy - r * 0.3f).roundToInt(), (r * 0.12f).roundToInt())

            // Massive hull fracture line
            pix.setColor(0.2f, 0.1f, 0.3f, 0.8f)
            pix.drawLine(
                (cx - r * 0.6f).roundToInt(), cy.roundToInt(),
                (cx + r * 0.6f).roundToInt(), cy.roundToInt()
            )
            pix.drawLine(
                cx.roundToInt(), (cy - r * 0.6f).roundToInt(),
                cx.roundToInt(), (cy + r * 0.6f).roundToInt()
            )

            // Exposed core leak
            pix.setColor(1f, 0.4f, 0.6f, 0.5f)
            pix.fillCircle((cx - r * 0.1f).roundToInt(), (cy - r * 0.1f).roundToInt(), (r * 0.08f).roundToInt())
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
        val h = s * 0.35f

        // Dark energy aura (pulsing dark red/black, layered)
        pix.setColor(0.3f, 0.05f, 0.05f, 0.15f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (h * 1.4f).roundToInt())
        pix.setColor(0.4f, 0.08f, 0.08f, 0.25f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (h * 1.15f).roundToInt())
        pix.setColor(0.5f, 0.1f, 0.1f, 0.35f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (h * 0.9f).roundToInt())

        // Main body (corrupted version of player ship - dark red/black)
        pix.setColor(0.25f, 0.05f, 0.05f, 1f)

        // Fuselage
        pix.fillRectangle((cx - h * 0.6f).roundToInt(), (cy - h * 0.25f).roundToInt(),
                         (h * 1.4f).roundToInt(), (h * 0.5f).roundToInt())

        // Nose cone (pointed left - clone direction)
        fillTriangle(pix,
            (cx - h * 1.15f).roundToInt(), cy.roundToInt(),         // nose tip
            (cx - h * 0.5f).roundToInt(), (cy - h * 0.25f).roundToInt(),
            (cx - h * 0.5f).roundToInt(), (cy + h * 0.25f).roundToInt()
        )

        // Wings (swept back, darker)
        pix.setColor(0.15f, 0.03f, 0.03f, 1f)
        // Top wing
        fillTriangle(pix,
            (cx + h * 0.2f).roundToInt(), (cy - h * 0.25f).roundToInt(),
            (cx - h * 0.5f).roundToInt(), (cy - h * 0.25f).roundToInt(),
            (cx - h * 0.7f).roundToInt(), (cy - h * 0.9f).roundToInt()
        )
        // Bottom wing
        fillTriangle(pix,
            (cx + h * 0.2f).roundToInt(), (cy + h * 0.25f).roundToInt(),
            (cx - h * 0.5f).roundToInt(), (cy + h * 0.25f).roundToInt(),
            (cx - h * 0.7f).roundToInt(), (cy + h * 0.9f).roundToInt()
        )

        // Corrupted wing struts (dark energy traces)
        pix.setColor(0.4f, 0.05f, 0.05f, 0.5f)
        pix.drawLine((cx - h * 0.1f).roundToInt(), (cy - h * 0.25f).roundToInt(),
                     (cx - h * 0.5f).roundToInt(), (cy - h * 0.7f).roundToInt())
        pix.drawLine((cx - h * 0.1f).roundToInt(), (cy + h * 0.25f).roundToInt(),
                     (cx - h * 0.5f).roundToInt(), (cy + h * 0.7f).roundToInt())

        // Cockpit (glowing red, corrupted - menacing)
        pix.setColor(0.8f, 0.1f, 0.1f, 0.9f)
        pix.fillCircle((cx - h * 0.4f).roundToInt(), cy.roundToInt(), (h * 0.18f).roundToInt())
        pix.setColor(1f, 0.3f, 0.3f, 0.7f)
        pix.fillCircle((cx - h * 0.45f).roundToInt(), (cy - h * 0.05f).roundToInt(), (h * 0.08f).roundToInt())

        // Engine nozzles (dark with corrupted red glow)
        pix.setColor(0.15f, 0.02f, 0.02f, 1f)
        pix.fillCircle((cx + h * 0.6f).roundToInt(), (cy - h * 0.15f).roundToInt(), (h * 0.12f).roundToInt())
        pix.fillCircle((cx + h * 0.6f).roundToInt(), (cy + h * 0.15f).roundToInt(), (h * 0.12f).roundToInt())
        // Nozzle inner corrupted glow
        pix.setColor(1f, 0.2f, 0.2f, 0.6f)
        pix.fillCircle((cx + h * 0.6f).roundToInt(), (cy - h * 0.15f).roundToInt(), (h * 0.06f).roundToInt())
        pix.fillCircle((cx + h * 0.6f).roundToInt(), (cy + h * 0.15f).roundToInt(), (h * 0.06f).roundToInt())

        // Corrupted engine exhaust glow (deeper red, wider)
        pix.setColor(1f, 0.15f, 0.15f, 0.35f)
        pix.fillCircle((cx + h * 0.8f).roundToInt(), cy.roundToInt(), (h * 0.4f).roundToInt())
        pix.setColor(1f, 0.1f, 0.1f, 0.5f)
        pix.fillCircle((cx + h * 0.8f).roundToInt(), cy.roundToInt(), (h * 0.25f).roundToInt())

        // Dark energy cracks (glowing red lines, more extensive)
        pix.setColor(1f, 0.1f, 0.1f, 0.7f)
        pix.drawLine((cx - h * 0.3f).roundToInt(), (cy - h * 0.2f).roundToInt(),
                     (cx + h * 0.2f).roundToInt(), (cy + h * 0.15f).roundToInt())
        pix.drawLine((cx - h * 0.1f).roundToInt(), (cy + h * 0.25f).roundToInt(),
                     (cx + h * 0.3f).roundToInt(), (cy - h * 0.1f).roundToInt())

        // Additional energy corruption lines
        pix.setColor(0.8f, 0.05f, 0.1f, 0.5f)
        pix.drawLine((cx - h * 0.5f).roundToInt(), (cy - h * 0.1f).roundToInt(),
                     (cx + h * 0.1f).roundToInt(), (cy - h * 0.1f).roundToInt())
        pix.drawLine((cx + h * 0.3f).roundToInt(), (cy - h * 0.25f).roundToInt(),
                     (cx + h * 0.5f).roundToInt(), (cy + h * 0.2f).roundToInt())

        // Dark energy pulsing nodes
        pix.setColor(0.6f, 0.05f, 0.05f, 0.8f)
        pix.fillCircle((cx - h * 0.1f).roundToInt(), (cy - h * 0.3f).roundToInt(), 3)
        pix.fillCircle((cx + h * 0.2f).roundToInt(), (cy + h * 0.25f).roundToInt(), 3)
        pix.fillCircle((cx - h * 0.2f).roundToInt(), cy.roundToInt(), 2)

        return pix
    }

    // ── Kamikaze sprite ──────────────────────────────────────────

    private fun createEnemyKamikazeSprite(): Pixmap {
        val s = Constants.SPRITE_ENEMY_FIGHTER // reuse fighter size
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val h = s * 0.35f

        // Glowing orange/yellow aura (hot engine, kamikaze is fast)
        pix.setColor(1f, 0.6f, 0f, 0.15f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (h * 1.3f).roundToInt())
        pix.setColor(1f, 0.7f, 0.1f, 0.25f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (h * 1.0f).roundToInt())

        // Main body — sleek arrow/dart shape (pointed right = toward player)
        pix.setColor(0.9f, 0.5f, 0f, 1f) // bright orange
        pix.fillRectangle((cx - h * 0.3f).roundToInt(), (cy - h * 0.15f).roundToInt(),
                         (h * 0.8f).roundToInt(), (h * 0.3f).roundToInt())

        // Pointed nose (right side — toward player)
        fillTriangle(pix,
            (cx + h * 0.9f).roundToInt(), cy.roundToInt(),
            (cx + h * 0.3f).roundToInt(), (cy - h * 0.15f).roundToInt(),
            (cx + h * 0.3f).roundToInt(), (cy + h * 0.15f).roundToInt()
        )

        // Tail fins (left side)
        pix.setColor(0.7f, 0.35f, 0f, 1f)
        fillTriangle(pix,
            (cx - h * 0.3f).roundToInt(), (cy - h * 0.15f).roundToInt(),
            (cx - h * 0.3f).roundToInt(), (cy - h * 0.7f).roundToInt(),
            (cx - h * 0.7f).roundToInt(), (cy - h * 0.15f).roundToInt()
        )
        fillTriangle(pix,
            (cx - h * 0.3f).roundToInt(), (cy + h * 0.15f).roundToInt(),
            (cx - h * 0.3f).roundToInt(), (cy + h * 0.7f).roundToInt(),
            (cx - h * 0.7f).roundToInt(), (cy + h * 0.15f).roundToInt()
        )

        // Warhead glow (bright white-hot tip)
        pix.setColor(1f, 0.9f, 0.3f, 0.9f)
        pix.fillCircle((cx + h * 0.7f).roundToInt(), cy.roundToInt(), (h * 0.12f).roundToInt())
        pix.setColor(1f, 1f, 0.8f, 0.7f)
        pix.fillCircle((cx + h * 0.65f).roundToInt(), cy.roundToInt(), (h * 0.06f).roundToInt())

        // Exhaust trail (orange glow behind)
        pix.setColor(1f, 0.4f, 0f, 0.4f)
        pix.fillCircle((cx - h * 0.8f).roundToInt(), cy.roundToInt(), (h * 0.25f).roundToInt())
        pix.setColor(1f, 0.6f, 0.1f, 0.6f)
        pix.fillCircle((cx - h * 0.75f).roundToInt(), cy.roundToInt(), (h * 0.15f).roundToInt())

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
        val bodyWidth = s * 0.22f
        val bodyHeight = s * 0.35f
        val helmetRadius = s * 0.18f

        // Legs (white suit)
        pix.setColor(0.9f, 0.9f, 0.95f, 1f)
        pix.fillRectangle((cx - bodyWidth * 0.6f).roundToInt(), (cy + bodyHeight * 0.3f).roundToInt(),
                         (bodyWidth * 0.4f).roundToInt(), (bodyHeight * 0.5f).roundToInt())
        pix.fillRectangle((cx + bodyWidth * 0.2f).roundToInt(), (cy + bodyHeight * 0.3f).roundToInt(),
                         (bodyWidth * 0.4f).roundToInt(), (bodyHeight * 0.5f).roundToInt())

        // Body (white suit with blue details)
        pix.setColor(0.95f, 0.95f, 1f, 1f)
        pix.fillRectangle((cx - bodyWidth * 0.5f).roundToInt(), (cy - bodyHeight * 0.2f).roundToInt(),
                         bodyWidth.roundToInt(), (bodyHeight * 0.6f).roundToInt())
        
        // Suit details (blue stripes)
        pix.setColor(0.2f, 0.5f, 0.9f, 0.7f)
        pix.fillRectangle((cx - bodyWidth * 0.4f).roundToInt(), (cy - bodyHeight * 0.1f).roundToInt(),
                         (bodyWidth * 0.8f).roundToInt(), (bodyHeight * 0.08f).roundToInt())

        // Arms (white suit)
        pix.setColor(0.9f, 0.9f, 0.95f, 1f)
        pix.fillRectangle((cx - bodyWidth * 0.9f).roundToInt(), (cy - bodyHeight * 0.15f).roundToInt(),
                         (bodyWidth * 0.35f).roundToInt(), (bodyHeight * 0.4f).roundToInt())
        pix.fillRectangle((cx + bodyWidth * 0.55f).roundToInt(), (cy - bodyHeight * 0.15f).roundToInt(),
                         (bodyWidth * 0.35f).roundToInt(), (bodyHeight * 0.4f).roundToInt())

        // Helmet (white with visor)
        pix.setColor(1f, 1f, 1f, 1f)
        pix.fillCircle(cx.roundToInt(), (cy - bodyHeight * 0.35f).roundToInt(), helmetRadius.roundToInt())
        
        // Visor (dark blue/black)
        pix.setColor(0.1f, 0.2f, 0.4f, 0.9f)
        pix.fillCircle(cx.roundToInt(), (cy - bodyHeight * 0.35f).roundToInt(), (helmetRadius * 0.7f).roundToInt())
        
        // Visor reflection
        pix.setColor(0.5f, 0.7f, 1f, 0.4f)
        pix.fillCircle((cx + helmetRadius * 0.2f).roundToInt(), (cy - bodyHeight * 0.4f).roundToInt(), 
                        (helmetRadius * 0.25f).roundToInt())

        // Helmet ring (gold/orange)
        pix.setColor(1f, 0.7f, 0.2f, 0.8f)
        pix.drawCircle(cx.roundToInt(), (cy - bodyHeight * 0.35f).roundToInt(), helmetRadius.roundToInt())

        return pix
    }

    private fun createAstronautRescuedSprite(): Pixmap {
        val s = Constants.SPRITE_ASTRONAUT
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val bodyWidth = s * 0.22f
        val bodyHeight = s * 0.35f
        val helmetRadius = s * 0.18f

        // Legs (white suit)
        pix.setColor(0.95f, 0.95f, 1f, 1f)
        pix.fillRectangle((cx - bodyWidth * 0.6f).roundToInt(), (cy + bodyHeight * 0.3f).roundToInt(),
                         (bodyWidth * 0.4f).roundToInt(), (bodyHeight * 0.5f).roundToInt())
        pix.fillRectangle((cx + bodyWidth * 0.2f).roundToInt(), (cy + bodyHeight * 0.3f).roundToInt(),
                         (bodyWidth * 0.4f).roundToInt(), (bodyHeight * 0.5f).roundToInt())

        // Body (white suit with blue details)
        pix.setColor(1f, 1f, 1f, 1f)
        pix.fillRectangle((cx - bodyWidth * 0.5f).roundToInt(), (cy - bodyHeight * 0.2f).roundToInt(),
                         bodyWidth.roundToInt(), (bodyHeight * 0.6f).roundToInt())
        
        // Suit details (blue stripes)
        pix.setColor(0.3f, 0.6f, 1f, 0.8f)
        pix.fillRectangle((cx - bodyWidth * 0.4f).roundToInt(), (cy - bodyHeight * 0.1f).roundToInt(),
                         (bodyWidth * 0.8f).roundToInt(), (bodyHeight * 0.08f).roundToInt())

        // Arms raised in celebration
        pix.setColor(0.95f, 0.95f, 1f, 1f)
        pix.fillRectangle((cx - bodyWidth * 0.9f).roundToInt(), (cy - bodyHeight * 0.5f).roundToInt(),
                         (bodyWidth * 0.35f).roundToInt(), (bodyHeight * 0.4f).roundToInt())
        pix.fillRectangle((cx + bodyWidth * 0.55f).roundToInt(), (cy - bodyHeight * 0.5f).roundToInt(),
                         (bodyWidth * 0.35f).roundToInt(), (bodyHeight * 0.4f).roundToInt())

        // Helmet (white with visor)
        pix.setColor(1f, 1f, 1f, 1f)
        pix.fillCircle(cx.roundToInt(), (cy - bodyHeight * 0.35f).roundToInt(), helmetRadius.roundToInt())
        
        // Visor (bright blue - happy)
        pix.setColor(0.2f, 0.5f, 1f, 0.9f)
        pix.fillCircle(cx.roundToInt(), (cy - bodyHeight * 0.35f).roundToInt(), (helmetRadius * 0.7f).roundToInt())
        
        // Visor reflection
        pix.setColor(0.6f, 0.8f, 1f, 0.5f)
        pix.fillCircle((cx + helmetRadius * 0.2f).roundToInt(), (cy - bodyHeight * 0.4f).roundToInt(), 
                        (helmetRadius * 0.25f).roundToInt())

        // Helmet ring (gold/orange - brighter)
        pix.setColor(1f, 0.8f, 0.3f, 0.9f)
        pix.drawCircle(cx.roundToInt(), (cy - bodyHeight * 0.35f).roundToInt(), helmetRadius.roundToInt())

        // Celebration sparkles
        pix.setColor(1f, 1f, 0.3f, 0.7f)
        pix.fillCircle((cx - bodyWidth * 1.2f).roundToInt(), (cy - bodyHeight * 0.6f).roundToInt(), 2)
        pix.fillCircle((cx + bodyWidth * 1.2f).roundToInt(), (cy - bodyHeight * 0.6f).roundToInt(), 2)

        return pix
    }

    private fun createAstronautDeadSprite(): Pixmap {
        val s = Constants.SPRITE_ASTRONAUT
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val bodyWidth = s * 0.22f
        val bodyHeight = s * 0.35f
        val helmetRadius = s * 0.18f

        // Legs (faded suit)
        pix.setColor(0.5f, 0.5f, 0.55f, 0.7f)
        pix.fillRectangle((cx - bodyWidth * 0.6f).roundToInt(), (cy + bodyHeight * 0.3f).roundToInt(),
                         (bodyWidth * 0.4f).roundToInt(), (bodyHeight * 0.5f).roundToInt())
        pix.fillRectangle((cx + bodyWidth * 0.2f).roundToInt(), (cy + bodyHeight * 0.3f).roundToInt(),
                         (bodyWidth * 0.4f).roundToInt(), (bodyHeight * 0.5f).roundToInt())

        // Body (faded suit)
        pix.setColor(0.6f, 0.6f, 0.65f, 0.8f)
        pix.fillRectangle((cx - bodyWidth * 0.5f).roundToInt(), (cy - bodyHeight * 0.2f).roundToInt(),
                         bodyWidth.roundToInt(), (bodyHeight * 0.6f).roundToInt())

        // Arms hanging down
        pix.setColor(0.5f, 0.5f, 0.55f, 0.7f)
        pix.fillRectangle((cx - bodyWidth * 0.9f).roundToInt(), (cy - bodyHeight * 0.1f).roundToInt(),
                         (bodyWidth * 0.35f).roundToInt(), (bodyHeight * 0.5f).roundToInt())
        pix.fillRectangle((cx + bodyWidth * 0.55f).roundToInt(), (cy - bodyHeight * 0.1f).roundToInt(),
                         (bodyWidth * 0.35f).roundToInt(), (bodyHeight * 0.5f).roundToInt())

        // Helmet (cracked/faded)
        pix.setColor(0.7f, 0.7f, 0.75f, 0.8f)
        pix.fillCircle(cx.roundToInt(), (cy - bodyHeight * 0.35f).roundToInt(), helmetRadius.roundToInt())
        
        // Visor (dark/cracked)
        pix.setColor(0.15f, 0.15f, 0.2f, 0.9f)
        pix.fillCircle(cx.roundToInt(), (cy - bodyHeight * 0.35f).roundToInt(), (helmetRadius * 0.7f).roundToInt())
        
        // Cracks on visor
        pix.setColor(0.4f, 0.4f, 0.5f, 0.6f)
        pix.drawLine((cx - helmetRadius * 0.4f).roundToInt(), (cy - bodyHeight * 0.35f).roundToInt(),
                     (cx + helmetRadius * 0.4f).roundToInt(), (cy - bodyHeight * 0.35f).roundToInt())
        pix.drawLine(cx.roundToInt(), (cy - bodyHeight * 0.35f - helmetRadius * 0.4f).roundToInt(),
                     cx.roundToInt(), (cy - bodyHeight * 0.35f + helmetRadius * 0.4f).roundToInt())

        // Helmet ring (faded)
        pix.setColor(0.5f, 0.4f, 0.2f, 0.6f)
        pix.drawCircle(cx.roundToInt(), (cy - bodyHeight * 0.35f).roundToInt(), helmetRadius.roundToInt())

        return pix
    }

    // ── Debris sprites (3 variants) ───────────────────────────────

    /** Gear: metallic circle with teeth and center hole. */
    private fun createDebrisGearSprite(): Pixmap {
        val s = Constants.SPRITE_DEBRIS
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()
        val cx = s / 2f; val cy = s / 2f; val r = s * 0.35f

        // Main body (dark silver)
        pix.setColor(0.45f, 0.45f, 0.5f, 1f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), r.roundToInt())

        // 8 gear teeth
        pix.setColor(0.4f, 0.4f, 0.45f, 1f)
        for (i in 0 until 8) {
            val angle = i * kotlin.math.PI.toFloat() / 4f
            val tx = (cx + kotlin.math.cos(angle.toDouble()).toFloat() * r * 0.9f).roundToInt()
            val ty = (cy + kotlin.math.sin(angle.toDouble()).toFloat() * r * 0.9f).roundToInt()
            pix.fillCircle(tx, ty, (r * 0.2f).roundToInt())
        }

        // Center hole
        pix.setColor(0.2f, 0.2f, 0.25f, 1f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (r * 0.35f).roundToInt())

        // Highlight
        pix.setColor(0.65f, 0.65f, 0.7f, 0.3f)
        pix.fillCircle((cx + r * 0.15f).roundToInt(), (cy + r * 0.15f).roundToInt(), (r * 0.15f).roundToInt())

        return pix
    }

    /** Antenna: bent pole with emitter dish on top. */
    private fun createDebrisAntennaSprite(): Pixmap {
        val s = Constants.SPRITE_DEBRIS
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()
        val cx = s / 2f; val cy = s / 2f; val r = s * 0.35f
        val half = s * 0.4f

        // Main pole (dark gray)
        pix.setColor(0.5f, 0.5f, 0.55f, 1f)
        pix.fillRectangle((cx - 2).roundToInt(), (cy - half * 0.8f).roundToInt(), 4, (half * 1.6f).roundToInt())

        // Cross bar (broken satellite arm)
        pix.setColor(0.45f, 0.45f, 0.5f, 1f)
        pix.fillRectangle((cx - half * 0.7f).roundToInt(), (cy - 1).roundToInt(), (half * 1.4f).roundToInt(), 3)

        // Dish/reflector (parabolic arc at top)
        pix.setColor(0.6f, 0.6f, 0.65f, 1f)
        for (i in -3..3) {
            val dishX = (cx + i * 4).roundToInt()
            val dishY = (cy - half * 0.8f + kotlin.math.abs(i).toFloat() * 3f).roundToInt()
            pix.fillCircle(dishX, dishY, (r * 0.12f).roundToInt())
        }

        // Dish concave center
        pix.setColor(0.35f, 0.35f, 0.4f, 1f)
        pix.fillCircle((cx - r * 0.05f).roundToInt(), (cy - half * 0.75f).roundToInt(), (r * 0.1f).roundToInt())

        // Blinking light at emitter tip (red)
        pix.setColor(0.9f, 0.15f, 0.1f, 1f)
        pix.fillCircle((cx - r * 0.1f).roundToInt(), (cy - half * 0.9f).roundToInt(), 3)
        pix.setColor(1f, 0.3f, 0.2f, 0.5f)
        pix.fillCircle((cx - r * 0.1f).roundToInt(), (cy - half * 0.9f).roundToInt(), 5)

        // Bolts
        pix.setColor(0.3f, 0.3f, 0.35f, 1f)
        pix.fillCircle((cx - half * 0.6f).roundToInt(), (cy + half * 0.4f).roundToInt(), 2)
        pix.fillCircle((cx + half * 0.6f).roundToInt(), (cy + half * 0.4f).roundToInt(), 2)

        return pix
    }

    /** Panel: jagged metal fragment with circuit traces. */
    private fun createDebrisPanelSprite(): Pixmap {
        val s = Constants.SPRITE_DEBRIS
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()
        val cx = s / 2f; val cy = s / 2f; val r = s * 0.35f
        val hw = r * 0.9f; val hh = r * 0.7f

        // Jagged panel body (light gray, pentagonal-ish)
        pix.setColor(0.55f, 0.55f, 0.6f, 1f)
        val corners = listOf(
            (cx - hw).roundToInt() to (cy + hh * 0.5f).roundToInt(),
            (cx - hw * 0.8f).roundToInt() to (cy - hh).roundToInt(),
            (cx + hw * 0.3f).roundToInt() to (cy - hh * 0.8f).roundToInt(),
            (cx + hw).roundToInt() to (cy - hh * 0.3f).roundToInt(),
            (cx + hw * 0.7f).roundToInt() to (cy + hh * 0.8f).roundToInt()
        )
        // Fill polygon via triangles from center
        for (i in corners.indices) {
            val (x1, y1) = corners[i]
            val (x2, y2) = corners[(i + 1) % corners.size]
            fillTriangle(pix, cx.roundToInt(), cy.roundToInt(), x1, y1, x2, y2)
        }

        // Circuit traces (green/gold lines)
        pix.setColor(0.2f, 0.6f, 0.3f, 0.7f)
        pix.drawLine((cx - hw * 0.5f).roundToInt(), (cy + hh * 0.2f).roundToInt(),
                     (cx + hw * 0.2f).roundToInt(), (cy + hh * 0.2f).roundToInt())
        pix.drawLine((cx + hw * 0.2f).roundToInt(), (cy + hh * 0.2f).roundToInt(),
                     (cx + hw * 0.4f).roundToInt(), (cy - hh * 0.3f).roundToInt())
        pix.setColor(0.6f, 0.5f, 0.1f, 0.6f)
        pix.drawLine((cx - hw * 0.3f).roundToInt(), (cy - hh * 0.1f).roundToInt(),
                     (cx + hw * 0.5f).roundToInt(), (cy - hh * 0.1f).roundToInt())

        // Chip (darker rectangle)
        pix.setColor(0.2f, 0.2f, 0.22f, 1f)
        pix.fillRectangle((cx - 6).roundToInt(), (cy - 4).roundToInt(), 12, 8)
        // Chip pins
        pix.setColor(0.4f, 0.4f, 0.45f, 0.8f)
        for (i in -2..2) {
            pix.fillRectangle((cx + i * 3 - 1).roundToInt(), (cy - 7).roundToInt(), 2, 3)
            pix.fillRectangle((cx + i * 3 - 1).roundToInt(), (cy + 5).roundToInt(), 2, 3)
        }

        // Scorch marks
        pix.setColor(0.15f, 0.12f, 0.08f, 0.6f)
        pix.fillCircle((cx - hw * 0.5f).roundToInt(), (cy - hh * 0.3f).roundToInt(), (r * 0.2f).roundToInt())

        // Broken corner
        pix.setColor(0f, 0f, 0f, 1f)
        fillTriangle(pix,
            (cx + hw * 0.8f).roundToInt(), (cy + hh * 0.6f).roundToInt(),
            (cx + hw).roundToInt(), (cy + hh * 0.2f).roundToInt(),
            (cx + hw * 0.5f).roundToInt(), (cy + hh).roundToInt()
        )

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

        // Radial glow (warm white/blue instead of gold)
        for (i in 5 downTo 1) {
            val r = outerR * i / 5f
            val alpha = 0.3f * (1f - i.toFloat() / 6f)
            pix.setColor(0.6f, 0.7f, 1f, alpha)
            pix.drawCircle(cx.roundToInt(), cy.roundToInt(), r.roundToInt())
        }

        // Center dot (warm white)
        pix.setColor(0.8f, 0.9f, 1f, 0.8f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), (outerR * 0.15f).roundToInt())

        return pix
    }

    // ── Laser sprites ────────────────────────────────────────────

    /** Default yellow laser (backward compat). */
    private fun createLaserSprite(): Pixmap =
        createLaserSprite(Color(1f, 1f, 0.2f, 1f))

    /** Default yellow glow (backward compat). */
    private fun createLaserGlowSprite(): Pixmap =
        createLaserGlowSprite(Color(1f, 1f, 0.4f, 1f))

    /**
     * Create a laser core pixmap tinted to the given [color].
     * The center pixel is brightened toward white for a hot-core effect.
     */
    private fun createLaserSprite(color: Color): Pixmap {
        val w = Constants.SPRITE_LASER_WIDTH
        val h = Constants.SPRITE_LASER_HEIGHT
        val pix = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = w / 2f
        val cy = h / 2f

        // Colored rectangle
        pix.setColor(color.r, color.g, color.b, 1f)
        pix.fillRectangle(0, (cy - 1f).roundToInt(), w, 3)

        // Bright center (blend toward white)
        pix.setColor(
            (color.r * 0.6f + 0.4f).coerceIn(0f, 1f),
            (color.g * 0.6f + 0.4f).coerceIn(0f, 1f),
            (color.b * 0.6f + 0.4f).coerceIn(0f, 1f),
            1f
        )
        pix.fillRectangle((cx - 2f).roundToInt(), (cy - 1f).roundToInt(), 4, 3)

        return pix
    }

    /**
     * Create a laser glow pixmap tinted to the given [color].
     * Uses the color hue with lower alpha for the diffuse glow effect.
     */
    private fun createLaserGlowSprite(color: Color): Pixmap {
        val w = Constants.SPRITE_LASER_GLOW_WIDTH
        val h = Constants.SPRITE_LASER_GLOW_HEIGHT
        val pix = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = w / 2f
        val cy = h / 2f

        // Diffuse glow
        pix.setColor(color.r, color.g, color.b, 0.3f)
        pix.fillRectangle((cx - w * 0.35f).roundToInt(), 1, (w * 0.7f).roundToInt(), h - 2)

        // Inner glow (brighter)
        pix.setColor(
            (color.r * 0.6f + 0.4f).coerceIn(0f, 1f),
            (color.g * 0.6f + 0.4f).coerceIn(0f, 1f),
            (color.b * 0.6f + 0.4f).coerceIn(0f, 1f),
            0.5f
        )
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

    // ── Heart sprite (HUD lives indicator) ────────────────────────

    private fun createHeartSprite(): Pixmap {
        val s = Constants.SPRITE_HEART
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2
        val cy = s / 2

        // Heart drawn with circles + triangle in Pixmap coordinates
        // (0,0 = top-left). The heart has two top bumps and a V-body.
        pix.setColor(1f, 0.2f, 0.3f, 1f)

        val hw = (s * 0.42f).toInt()    // half-width of the heart body
        val hh = (s * 0.46f).toInt()    // half-height

        // Top bumps (circles)
        val bumpR = (s * 0.22f).toInt()  // radius ~11px for s=48
        val b1x = cx - hw / 2
        val b2x = cx + hw / 2
        val bumpY = cy - hh / 3

        pix.fillCircle(b1x, bumpY, bumpR)
        pix.fillCircle(b2x, bumpY, bumpR)

        // V-body (triangle from below bumps to bottom point)
        val vTopY = bumpY + bumpR / 2
        val tipY = cy + hh
        pix.fillTriangle(
            cx - hw, vTopY,   // left
            cx + hw, vTopY,   // right
            cx, tipY,          // bottom tip
        )

        // Fill any gap between circles and triangle
        pix.fillRectangle(cx - hw, bumpY, hw * 2, vTopY - bumpY)

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
