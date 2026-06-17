package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.FitViewport
import com.nebuladrift.util.Constants

/**
 * Manages the game camera and viewport.
 *
 * Uses a [FitViewport] with the game's world dimensions so the
 * play area is always fully visible regardless of screen aspect
 * ratio (letterboxed on non-16:9 displays).
 */
class CameraSetup {
    val camera = OrthographicCamera()
    val viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera)

    init {
        camera.position.set(
            Constants.WORLD_WIDTH / 2f,
            Constants.WORLD_HEIGHT / 2f,
            0f
        )
    }

    /** Call in screen resize to update viewport and recenter camera. */
    fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    /** Call each frame to synchronise camera matrices. */
    fun update() {
        camera.update()
    }
}
