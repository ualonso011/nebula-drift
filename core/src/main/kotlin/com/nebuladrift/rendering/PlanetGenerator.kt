package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.nebuladrift.util.Constants
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates procedural planet textures.
 *
 * Each function returns a [Pixmap] sized [Constants.PLANET_TEXTURE_SIZE].
 * Use [createTexture] to convert to a GPU [Texture].
 */
object PlanetGenerator {

    private val rng = Random(42) // fixed seed for reproducibility

    /** Convert a pixmap to a GPU texture and dispose the pixmap. */
    fun createTexture(pixmap: Pixmap): Texture {
        val tex = Texture(pixmap)
        pixmap.dispose()
        return tex
    }

    // ── Gas Giant ─────────────────────────────────────────────

    /**
     * Horizontal colour bands with soft gradients, like Jupiter/Saturn.
     * Palette: warm orange/brown/tan tones.
     */
    fun generateGasGiant(): Pixmap {
        val s = Constants.PLANET_TEXTURE_SIZE
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.42f

        // Clip to circle (clear outside)
        pix.setColor(0f, 0f, 0f, 0f)
        for (y in 0 until s) {
            for (x in 0 until s) {
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy > r * r) {
                    pix.drawPixel(x, y)
                }
            }
        }

        // Band colors as RGB triples (no nesting — flat list)
        val bandRgb = listOf(
            listOf(0.6f, 0.4f, 0.2f),   // brown
            listOf(0.8f, 0.6f, 0.3f),   // tan
            listOf(0.7f, 0.5f, 0.25f),  // light brown
            listOf(0.5f, 0.35f, 0.15f), // dark brown
            listOf(0.75f, 0.55f, 0.3f), // golden tan
            listOf(0.6f, 0.45f, 0.2f),  // medium brown
            listOf(0.7f, 0.5f, 0.25f),  // tan
            listOf(0.55f, 0.4f, 0.2f),  // brown
        )

        // Band positions (normalised Y offsets from center)
        val bandCenters = listOf(-0.6f, -0.4f, -0.2f, 0.0f, 0.2f, 0.4f, 0.6f, 0.8f)

        for (y in 0 until s) {
            val ny = (y - cy) / r  // normalised -1..1
            if (ny * ny > 1f) continue

            // Find nearest band
            var bestDist = Float.MAX_VALUE
            var bestIdx = 0
            for (i in bandCenters.indices) {
                val d = kotlin.math.abs(ny - bandCenters[i])
                if (d < bestDist) {
                    bestDist = d
                    bestIdx = i
                }
            }

            // Slight noise for texture
            val noise = (rng.nextFloat() - 0.5f) * 0.08f

            // Interpolate between band color and neighbor
            val rgb = bandRgb[bestIdx]
            val cr = rgb[0] + noise
            val cg = rgb[1] + noise
            val cb = rgb[2] + noise

            val alpha = (1f - ny * ny).coerceIn(0.3f, 1f) // edge fade

            for (x in 0 until s) {
                val dx = x - cx
                if (dx * dx + ny * ny * r * r > r * r) continue
                // Per-pixel noise
                val pxNoise = (rng.nextFloat() - 0.5f) * 0.03f
                pix.setColor(
                    (cr + pxNoise).coerceIn(0f, 1f),
                    (cg + pxNoise).coerceIn(0f, 1f),
                    (cb + pxNoise).coerceIn(0f, 1f),
                    alpha
                )
                pix.drawPixel(x, y)
            }
        }

        // Edge glow (soft atmospheric rim)
        for (i in 0 until 5) {
            val glowR = r + i * 2f
            val gAlpha = 0.04f * (1f - i / 5f)
            pix.setColor(0.8f, 0.6f, 0.3f, gAlpha)
            pix.drawCircle(cx.roundToInt(), cy.roundToInt(), glowR.roundToInt())
        }

        return pix
    }

    // ── Rocky ─────────────────────────────────────────────────

    /**
     * Irregular cratered surface, gray/brown palette.
     * Similar to asteroid generation but at larger scale.
     */
    fun generateRocky(): Pixmap {
        val s = Constants.PLANET_TEXTURE_SIZE
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.42f

        // Base body (irregular circle)
        pix.setColor(0.5f, 0.45f, 0.4f, 1f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), r.roundToInt())

        // Bumps to make it irregular
        pix.setColor(0.55f, 0.5f, 0.45f, 1f)
        val bumpAngles = listOf(0.0, 0.8, 1.6, 2.4, 3.2, 4.0, 4.8, 5.6)
        for (angle in bumpAngles) {
            val bx = (cx + cos(angle) * r * 0.7f).roundToInt()
            val by = (cy + sin(angle) * r * 0.7f).roundToInt()
            pix.fillCircle(bx, by, (r * 0.3f).roundToInt())
        }

        // Surface noise (per-pixel variation)
        for (y in 0 until s) {
            for (x in 0 until s) {
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy > r * r) continue
                val noise = (rng.nextFloat() - 0.5f) * 0.15f
                val px = pix.getPixel(x, y)
                val pr = ((px shr 24) and 0xFF) / 255f
                val pg = ((px shr 16) and 0xFF) / 255f
                val pb = ((px shr 8) and 0xFF) / 255f
                pix.setColor(
                    (pr + noise).coerceIn(0.2f, 0.8f),
                    (pg + noise).coerceIn(0.2f, 0.7f),
                    (pb + noise).coerceIn(0.2f, 0.6f),
                    1f
                )
                pix.drawPixel(x, y)
            }
        }

        // Craters (dark indents with highlights)
        // Each entry: (dxOffset, dyOffset, radiusFraction)
        data class CraterPos(val dx: Float, val dy: Float, val radiusFrac: Float)
        val craters = listOf(
            CraterPos(-0.3f, -0.3f, 0.2f),
            CraterPos(+0.35f, +0.2f, 0.15f),
            CraterPos(-0.1f, +0.4f, 0.12f),
            CraterPos(+0.4f, -0.35f, 0.18f),
            CraterPos(-0.4f, +0.1f, 0.1f),
        )
        for (crater in craters) {
            val cx2 = (cx + crater.dx * r).roundToInt()
            val cy2 = (cy + crater.dy * r).roundToInt()
            val cr = (r * crater.radiusFrac).roundToInt()

            // Dark indent
            pix.setColor(0.3f, 0.25f, 0.2f, 0.8f)
            pix.fillCircle(cx2, cy2, cr)

            // Highlight edge (bottom-right)
            pix.setColor(0.6f, 0.55f, 0.5f, 0.4f)
            pix.fillCircle((cx2 + cr * 0.2f).roundToInt(), (cy2 - cr * 0.2f).roundToInt(),
                (cr * 0.7f).roundToInt())
        }

        // Edge fade
        for (y in 0 until s) {
            for (x in 0 until s) {
                val dx = x - cx
                val dy = y - cy
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toFloat())
                if (dist > r * 0.8f && dist < r + 3f) {
                    val alpha = 1f - ((dist - r * 0.8f) / (r * 0.2f + 3f)).coerceIn(0f, 1f)
                    val px = pix.getPixel(x, y)
                    val pr = ((px shr 24) and 0xFF) / 255f
                    val pg = ((px shr 16) and 0xFF) / 255f
                    val pb = ((px shr 8) and 0xFF) / 255f
                    pix.setColor(pr, pg, pb, alpha)
                    pix.drawPixel(x, y)
                }
            }
        }

        return pix
    }

    // ── Ringed ────────────────────────────────────────────────

    /**
     * Rocky body with a translucent elliptical ring around it.
     * Ring is gold/tan with partial transparency.
     */
    fun generateRinged(): Pixmap {
        val s = Constants.PLANET_TEXTURE_SIZE
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val bodyR = s * 0.28f
        val ringOuterR = s * 0.44f
        val ringInnerR = s * 0.34f

        // ── Body (rocky-style surface) ──
        pix.setColor(0.6f, 0.55f, 0.45f, 1f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), bodyR.roundToInt())

        // Surface noise on body
        for (y in 0 until s) {
            for (x in 0 until s) {
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy > bodyR * bodyR) continue
                val noise = (rng.nextFloat() - 0.5f) * 0.12f
                val px = pix.getPixel(x, y)
                val pr = ((px shr 24) and 0xFF) / 255f
                val pg = ((px shr 16) and 0xFF) / 255f
                val pb = ((px shr 8) and 0xFF) / 255f
                pix.setColor(
                    (pr + noise).coerceIn(0.2f, 0.8f),
                    (pg + noise).coerceIn(0.2f, 0.7f),
                    (pb + noise).coerceIn(0.2f, 0.6f),
                    1f
                )
                pix.drawPixel(x, y)
            }
        }

        // ── Ring (elliptical, drawn behind & in front of body) ──
        drawRing(pix, cx, cy, ringInnerR, ringOuterR, s)

        return pix
    }

    /**
     * Draw an elliptical ring using horizontal scanlines.
     * The ring is gold/tan, semi-transparent, with banding.
     */
    private fun drawRing(
        pix: Pixmap,
        cx: Float, cy: Float,
        innerR: Float, outerR: Float,
        s: Int
    ) {
        // Draw ring as concentric ellipses (squashed vertically)
        val vScale = 0.35f // vertical squash factor
        val ringRgb = listOf(
            listOf(0.7f, 0.55f, 0.2f), // gold base
            listOf(0.8f, 0.65f, 0.3f), // light gold
            listOf(0.6f, 0.45f, 0.15f), // darker band
        )

        for (y in 0 until s) {
            for (x in 0 until s) {
                val dx = x - cx
                val dy = (y - cy) / vScale  // unstretch for ellipse check
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toFloat())

                if (dist in innerR..outerR) {
                    // Ring band pattern
                    val bandIdx = ((dist / outerR) * ringRgb.size).toInt()
                        .coerceIn(0, ringRgb.size - 1)
                    val rgb = ringRgb[bandIdx]
                    val cr = rgb[0]
                    val cg = rgb[1]
                    val cb = rgb[2]

                    // Radial gradient (darker at edges)
                    val edgeT = ((dist - innerR) / (outerR - innerR)).coerceIn(0f, 1f)
                    val alpha = (0.4f + 0.3f * sin(PI.toFloat() * edgeT)) * (1f - 0.3f * edgeT)

                    // Subtle noise
                    val noise = (rng.nextFloat() - 0.5f) * 0.05f

                    pix.setColor(
                        (cr + noise).coerceIn(0f, 1f),
                        (cg + noise).coerceIn(0f, 1f),
                        (cb + noise).coerceIn(0f, 1f),
                        alpha.coerceIn(0f, 1f)
                    )
                    pix.drawPixel(x, y)
                }
            }
        }

        // Outer edge glow on ring
        for (i in 0 until 4) {
            val glowR = outerR + i * 1.5f
            val gAlpha = 0.03f * (1f - i / 4f)
            pix.setColor(0.8f, 0.6f, 0.2f, gAlpha)
            for (angle in 0..360 step 5) {
                val rad = Math.toRadians(angle.toDouble())
                val gx = (cx + cos(rad) * glowR).roundToInt()
                val gy = (cy + sin(rad) * glowR * vScale).roundToInt()
                if (gx in 0 until s && gy in 0 until s) {
                    pix.drawPixel(gx, gy)
                }
            }
        }
    }
}
