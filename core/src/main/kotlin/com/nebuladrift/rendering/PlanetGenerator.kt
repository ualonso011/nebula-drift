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
 * Generates procedural planet textures at [PLANET_TEXTURE_SIZE].
 *
 * Each generator returns a [Pixmap]; call [createTexture] to convert
 * to a GPU [Texture] and release the pixmap.
 *
 * Optimised for mobile — uses [Pixmap.drawLine] and [Pixmap.fillCircle]
 * instead of pixel-by-pixel [Pixmap.drawPixel] to avoid ANR on Android.
 */
object PlanetGenerator {

    /** Resolution for planet textures. 128 is fast on mobile, still crisp at game scale. */
    private const val TEX_SIZE = 128

    private val rng = Random(42)

    fun createTexture(pixmap: Pixmap): Texture {
        val tex = Texture(pixmap)
        pixmap.dispose()
        return tex
    }

    // ── Gas Giant ─────────────────────────────────────────────

    /**
     * Horizontal colour bands (Jupiter/Saturn style) using [drawLine]
     * for efficient per-row drawing.
     */
    fun generateGasGiant(): Pixmap {
        val s = TEX_SIZE
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.42f

        // Band colours: (r, g, b) per band row
        val bands = listOf(
            listOf(0.60f, 0.40f, 0.20f),
            listOf(0.80f, 0.60f, 0.30f),
            listOf(0.70f, 0.50f, 0.25f),
            listOf(0.50f, 0.35f, 0.15f),
            listOf(0.75f, 0.55f, 0.30f),
            listOf(0.60f, 0.45f, 0.20f),
            listOf(0.70f, 0.50f, 0.25f),
            listOf(0.55f, 0.40f, 0.20f),
        )
        val bandCenters = listOf(-0.6f, -0.4f, -0.2f, 0.0f, 0.2f, 0.4f, 0.6f, 0.8f)

        // Draw each row as a horizontal line (much faster than per-pixel)
        for (y in 0 until s) {
            val ny = (y - cy) / r
            if (ny * ny > 1f) continue

            // Find nearest band
            var bestDist = Float.MAX_VALUE
            var bestIdx = 0
            for (i in bandCenters.indices) {
                val d = kotlin.math.abs(ny - bandCenters[i])
                if (d < bestDist) { bestDist = d; bestIdx = i }
            }

            val rgb = bands[bestIdx]
            val noise = (rng.nextFloat() - 0.5f) * 0.1f
            val rr = (rgb[0] + noise).coerceIn(0f, 1f)
            val gg = (rgb[1] + noise).coerceIn(0f, 1f)
            val bb = (rgb[2] + noise).coerceIn(0f, 1f)

            // Edge fade alpha
            val alpha = (1f - ny * ny).coerceIn(0.3f, 1f)

            pix.setColor(rr, gg, bb, alpha)

            // Clip line to circle: find x extents for this y
            val hw = kotlin.math.sqrt((r * r - (y - cy) * (y - cy)).coerceAtLeast(0f))
            val x1 = (cx - hw).roundToInt().coerceIn(0, s - 1)
            val x2 = (cx + hw).roundToInt().coerceIn(0, s - 1)
            if (x2 >= x1) pix.drawLine(x1, y, x2, y)
        }

        // Soft edge glow
        for (i in 0 until 3) {
            val glowR = r + i * 2f
            val ga = 0.06f * (1f - i / 3f)
            pix.setColor(0.8f, 0.6f, 0.3f, ga)
            pix.drawCircle(cx.roundToInt(), cy.roundToInt(), glowR.roundToInt())
        }

        return pix
    }

    // ── Rocky ─────────────────────────────────────────────────

    /**
     * Irregular cratered surface using [Pixmap.fillCircle] for cheap
     * layered drawing. No per-pixel noise — fast on mobile.
     */
    fun generateRocky(): Pixmap {
        val s = TEX_SIZE
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.42f

        // Base body
        pix.setColor(0.5f, 0.45f, 0.4f, 1f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), r.roundToInt())

        // Bumps for irregular shape
        pix.setColor(0.55f, 0.5f, 0.45f, 1f)
        val bumpAngles = listOf(0.0, 0.8, 1.6, 2.4, 3.2, 4.0, 4.8, 5.6)
        for (angle in bumpAngles) {
            val bx = (cx + cos(angle) * r * 0.7f).roundToInt()
            val by = (cy + sin(angle) * r * 0.7f).roundToInt()
            pix.fillCircle(bx, by, (r * 0.3f).roundToInt())
        }

        // Craters: dark fill + light highlight edge
        data class CrDef(val dx: Float, val dy: Float, val rf: Float)
        val craters = listOf(
            CrDef(-0.3f, -0.3f, 0.20f),
            CrDef(+0.35f, +0.2f, 0.15f),
            CrDef(-0.1f, +0.4f, 0.12f),
            CrDef(+0.4f, -0.35f, 0.18f),
            CrDef(-0.4f, +0.1f, 0.10f),
        )
        for (c in craters) {
            val cr = (r * c.rf).roundToInt()
            val cx2 = (cx + c.dx * r).roundToInt()
            val cy2 = (cy + c.dy * r).roundToInt()
            // Dark indent
            pix.setColor(0.3f, 0.25f, 0.2f, 0.8f)
            pix.fillCircle(cx2, cy2, cr)
            // Highlight edge
            pix.setColor(0.6f, 0.55f, 0.5f, 0.35f)
            pix.fillCircle((cx2 + cr * 0.15f).roundToInt(), (cy2 - cr * 0.15f).roundToInt(),
                (cr * 0.6f).roundToInt())
        }

        // Quick surface variation: a few light speckle circles
        pix.setColor(0.6f, 0.55f, 0.5f, 0.3f)
        for (i in 0 until 12) {
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val dist = rng.nextFloat() * r * 0.7f
            val sx = (cx + cos(angle) * dist).roundToInt()
            val sy = (cy + sin(angle) * dist).roundToInt()
            pix.fillCircle(sx, sy, (r * 0.04f).roundToInt().coerceAtLeast(1))
        }

        // Edge fade
        for (dy in 0 until s) {
            for (dx in 0 until s) {
                val dist = kotlin.math.sqrt(
                    ((dx - cx) * (dx - cx) + (dy - cy) * (dy - cy)).toFloat()
                )
                if (dist > r * 0.85f && dist < r + 2f) {
                    val px = pix.getPixel(dx, dy)
                    val pr = ((px shr 24) and 0xFF) / 255f
                    val pg = ((px shr 16) and 0xFF) / 255f
                    val pb = ((px shr 8) and 0xFF) / 255f
                    val alpha = 1f - ((dist - r * 0.85f) / (r * 0.15f + 2f)).coerceIn(0f, 1f)
                    pix.setColor(pr, pg, pb, alpha)
                    pix.drawPixel(dx, dy)
                }
            }
        }

        return pix
    }

    // ── Ringed ────────────────────────────────────────────────

    /**
     * Rocky body with a translucent elliptical ring.
     * Ring drawn efficiently with horizontal lines (not per-pixel).
     */
    fun generateRinged(): Pixmap {
        val s = TEX_SIZE
        val pix = Pixmap(s, s, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val cx = s / 2f
        val cy = s / 2f
        val bodyR = s * 0.28f

        // ── Body (simple rocky surface, no per-pixel noise) ──
        pix.setColor(0.6f, 0.55f, 0.45f, 1f)
        pix.fillCircle(cx.roundToInt(), cy.roundToInt(), bodyR.roundToInt())

        // Speckles
        pix.setColor(0.7f, 0.65f, 0.5f, 0.4f)
        for (i in 0 until 8) {
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val dist = rng.nextFloat() * bodyR * 0.6f
            val sx = (cx + cos(angle) * dist).roundToInt()
            val sy = (cy + sin(angle) * dist).roundToInt()
            pix.fillCircle(sx, sy, (bodyR * 0.05f).roundToInt().coerceAtLeast(1))
        }

        // ── Ring (elliptical, behind body) ──
        drawRing(pix, cx, cy, s * 0.34f, s * 0.44f, s)

        return pix
    }

    // ── Ring helper ───────────────────────────────────────────

    /** Draw a squashed elliptical ring using horizontal line spans. */
    private fun drawRing(pix: Pixmap, cx: Float, cy: Float, innerR: Float, outerR: Float, s: Int) {
        val vScale = 0.35f
        val bandColors = listOf(
            listOf(0.7f, 0.55f, 0.2f),
            listOf(0.8f, 0.65f, 0.3f),
            listOf(0.6f, 0.45f, 0.15f),
        )

        for (y in 0 until s) {
            // Un-stretch y-coord for ellipse check
            val ny = (y - cy) / vScale

            // For each row, find x range that falls within ring annulus
            val hwOuter = kotlin.math.sqrt((outerR * outerR - (ny - cy) * (ny - cy)).coerceAtLeast(0f))
            val hwInner = if (kotlin.math.abs(ny - cy) < innerR)
                kotlin.math.sqrt((innerR * innerR - (ny - cy) * (ny - cy)).coerceAtLeast(0f))
            else 0f

            if (hwOuter <= 0f || hwOuter <= hwInner + 0.5f) continue

            val x1 = (cx - hwOuter).roundToInt().coerceIn(0, s - 1)
            val x2 = (cx - hwInner).roundToInt().coerceIn(0, s - 1)
            val x3 = (cx + hwInner).roundToInt().coerceIn(0, s - 1)
            val x4 = (cx + hwOuter).roundToInt().coerceIn(0, s - 1)

            // Radial band & alpha
            val midDist = (hwOuter + hwInner) / 2f
            val bandIdx = ((midDist / outerR) * bandColors.size).toInt().coerceIn(0, bandColors.size - 1)
            val rgb = bandColors[bandIdx]
            val edgeT = ((midDist - innerR) / (outerR - innerR)).coerceIn(0f, 1f)
            val alpha = (0.4f + 0.3f * sin(PI.toFloat() * edgeT)) * (1f - 0.3f * edgeT)

            pix.setColor(rgb[0], rgb[1], rgb[2], alpha.coerceIn(0f, 1f))
            // Draw left span + right span
            if (x2 >= x1) pix.drawLine(x1, y, x2, y)
            if (x4 >= x3) pix.drawLine(x3, y, x4, y)
        }
    }
}
