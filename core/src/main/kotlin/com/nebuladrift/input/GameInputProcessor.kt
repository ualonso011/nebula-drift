package com.nebuladrift.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor

/**
 * Handles gameplay input for the ship.
 *
 * Touch/mouse:
 * - Left half of screen (x < screenWidth/2): touch down = start thrust, touch up = stop thrust
 * - Right half of screen (x >= screenWidth/2): touch down = fire laser
 *
 * Keyboard:
 * - A / Left: hold = thrust, release = stop
 * - Space: press = fire laser
 *
 * Designed to be used inside an [com.badlogic.gdx.InputMultiplexer] alongside
 * Scene2D UI processors (added in Phase 3).
 *
 * @param onThrustStart Called when the player starts thrusting
 * @param onThrustStop Called when the player stops thrusting
 * @param onFire Called when the player fires a laser
 */
class GameInputProcessor(
    private val onThrustStart: () -> Unit,
    private val onThrustStop: () -> Unit,
    private val onFire: () -> Unit
) : InputProcessor {

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (screenX < Gdx.graphics.width / 2) {
            onThrustStart()
        } else {
            onFire()
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        // Stop thrust on left-half release
        if (screenX < Gdx.graphics.width / 2) {
            onThrustStop()
        }
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
            Input.Keys.A, Input.Keys.LEFT -> onThrustStart()
            Input.Keys.SPACE -> onFire()
        }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.A, Input.Keys.LEFT -> onThrustStop()
        }
        return true
    }

    override fun keyTyped(character: Char): Boolean = false

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
}
