package com.nebuladrift.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor

/**
 * Handles gameplay input for the ship.
 *
 * Touch/mouse:
 * - Left half of screen (x < screenWidth/2): tap = flap (instant upward impulse, like Flappy Bird)
 * - Right half of screen (x >= screenWidth/2): tap = fire laser
 *
 * Keyboard:
 * - A / Left / Space: tap = flap
 * - Space: also fires laser (alternative)
 *
 * Designed to be used inside an [com.badlogic.gdx.InputMultiplexer] alongside
 * Scene2D UI processors (added in Phase 3).
 *
 * @param onFlap Called when the player taps to flap (instant upward impulse)
 * @param onFire Called when the player fires a laser
 */
class GameInputProcessor(
    private val onFlap: () -> Unit,
    private val onFire: () -> Unit,
    /** Debug: force game over (key: O). No-op by default. */
    private val onDebugGameOver: () -> Unit = {},
) : InputProcessor {

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val screenWidth = Gdx.graphics.width
        val halfWidth = screenWidth / 2
        
        // Debug logging
        com.badlogic.gdx.Gdx.app.log("GameInput", "touchDown: screenX=$screenX, screenWidth=$screenWidth, halfWidth=$halfWidth")
        
        if (screenX < halfWidth) {
            com.badlogic.gdx.Gdx.app.log("GameInput", "LEFT side - flap")
            onFlap()
        } else {
            com.badlogic.gdx.Gdx.app.log("GameInput", "RIGHT side - fire")
            onFire()
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        // No action needed for Flappy Bird style input
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        // No continuous tracking needed for current gameplay
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false

    override fun scrolled(amountX: Float, amountY: Float): Boolean = false

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.A, Input.Keys.LEFT, Input.Keys.SPACE -> onFlap()
            Input.Keys.UP -> onFire()
            Input.Keys.O -> onDebugGameOver()
        }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        // No action needed for Flappy Bird style input
        return true
    }

    override fun keyTyped(character: Char): Boolean = false

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
}
