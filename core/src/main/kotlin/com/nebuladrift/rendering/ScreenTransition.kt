package com.nebuladrift.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * Full-screen fade transition between game screens.
 *
 * Cycles through four phases:
 * 1. **FADE_OUT** — alpha ramps 0→1 (screen fades to black)
 * 2. **SWITCH** — one-frame signal to swap the active [Screen]
 * 3. **FADE_IN** — alpha ramps 1→0 (black fades to the new screen)
 * 4. **DONE** — transition complete, overlay is invisible
 *
 * Render via [render] with a [SpriteBatch] and a 1×1 white [Texture].
 *
 * Usage:
 * ```
 * val transition = FadeTransition()
 * // Each frame:
 * if (transition.update(delta)) {
 *     // SWITCH phase — swap screens here
 *     game.setScreen<NextScreen>()
 * }
 * // After normal screen rendering:
 * transition.render(batch, whiteTexture)
 * ```
 *
 * @property duration Duration of each fade half (total = 2 × duration).
 * @property color Tint colour for the overlay (default: black).
 */
class FadeTransition(
    private val duration: Float = 0.5f,
    private val color: Color = Color.BLACK
) {

    /** Transition lifecycle phases in order. */
    enum class Phase {
        FADE_OUT,
        SWITCH,
        FADE_IN,
        DONE
    }

    /** Current phase. */
    var phase: Phase = Phase.FADE_OUT
        private set

    /** Elapsed time within the current phase. */
    var timer: Float = 0f
        private set

    /** Current overlay alpha (0 = transparent, 1 = opaque). */
    val alpha: Float
        get() = when (phase) {
            Phase.FADE_OUT -> (timer / duration).coerceIn(0f, 1f)
            Phase.FADE_IN -> (1f - timer / duration).coerceIn(0f, 1f)
            Phase.SWITCH, Phase.DONE -> 0f
        }

    /** Whether the entire transition has completed. */
    val isComplete: Boolean get() = phase == Phase.DONE

    /**
     * Advance the transition by [delta] seconds.
     * @return **true** exactly once when the SWITCH phase completes,
     *         signalling the caller to perform the actual screen switch.
     */
    fun update(delta: Float): Boolean {
        timer += delta

        return when (phase) {
            Phase.FADE_OUT -> {
                if (timer >= duration) {
                    // Transition through SWITCH to FADE_IN in one call
                    phase = Phase.FADE_IN
                    timer = 0f
                    true // signal: switch screens now
                } else {
                    false
                }
            }
            Phase.FADE_IN -> {
                if (timer >= duration) {
                    phase = Phase.DONE
                }
                false
            }
            Phase.SWITCH, Phase.DONE -> false
        }
    }

    /**
     * Render a full-screen overlay at the current alpha.
     *
     * @param batch An active [SpriteBatch] (between begin/end).
     * @param whiteTexture A 1×1 white [Texture] used as the overlay rect.
     */
    fun render(batch: SpriteBatch, whiteTexture: Texture) {
        if (phase == Phase.DONE) return

        batch.color = Color(color.r, color.g, color.b, alpha)
        batch.draw(
            whiteTexture,
            0f, 0f,
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat()
        )
        batch.color = Color.WHITE
    }

    /** Reset to initial state for reuse. */
    fun reset() {
        phase = Phase.FADE_OUT
        timer = 0f
    }
}
