package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.FitViewport
import ktx.app.KtxScreen
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.LaserOwner
import com.nebuladrift.entities.Ship
import com.nebuladrift.entities.SpaceDebris
import com.nebuladrift.entities.enemies.Enemy
import com.nebuladrift.input.GameInputProcessor
import com.nebuladrift.managers.AudioManager
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.managers.LeaderboardManager
import com.nebuladrift.rendering.CameraSetup
import com.nebuladrift.rendering.GameRenderer
import com.nebuladrift.rendering.HudRenderer
import com.nebuladrift.rendering.ParallaxBackground
import com.nebuladrift.rendering.ParticleManager
import com.nebuladrift.rendering.SpriteAtlas
import com.nebuladrift.rendering.UiSkin
import com.nebuladrift.systems.AstronautSpawnSystem
import com.nebuladrift.systems.CollisionSystem
import com.nebuladrift.systems.DebrisSpawnSystem
import com.nebuladrift.systems.DifficultyManager
import com.nebuladrift.systems.EnemySpawnSystem
import com.nebuladrift.systems.GameContext
import com.nebuladrift.systems.GameEvent
import com.nebuladrift.systems.MirrorSystem
import com.nebuladrift.systems.PhysicsSystem
import com.nebuladrift.systems.ScoreSystem
import com.nebuladrift.systems.SpawnSystem
import com.nebuladrift.util.Constants
import com.nebuladrift.util.LeaderboardEntry
import com.nebuladrift.NebulaDriftGame

/**
 * Main gameplay screen.
 *
 * Handles both gameplay and the game-over overlay (inline, no screen switch).
 * When the ship is destroyed the game transitions to a game-over HUD rendered
 * directly via Scene2D on the same screen.
 */
class GameScreen(
    private val game: NebulaDriftGame,
    private val i18n: I18nManager,
    private val atlas: SpriteAtlas,
    /**
     * Android callback: when the user clicks "Main Menu", the host
     * Activity finishes (back to Compose menus). Null on desktop —
     * falls back to the libGDX [MenuScreen].
     */
    private val onExitToMenu: (() -> Unit)? = null,
) : KtxScreen {

    // ── Rendering ─────────────────────────────────────────────
    private val cameraSetup = CameraSetup()
    private val batch = SpriteBatch()
    private val hudRenderer = HudRenderer(atlas)

    /** Entity renderer using the procedural sprite atlas. */
    private val gameRenderer = GameRenderer(batch, atlas, cameraSetup.camera)

    /** Particle system for explosions, trails, sparkles, and damage. */
    private val particleManager = ParticleManager(atlas).also { it.init() }

    /** Parallax scrolling background. */
    private val parallaxBackground = ParallaxBackground().also { it.init() }

    /** Runtime-generated placeholder background texture (fallback). */
    private val backgroundTexture: Texture by lazy { createBackgroundTexture() }

    // ── Game-over overlay ─────────────────────────────────────
    private val goCamera = OrthographicCamera()
    private val goViewport = FitViewport(16f, 9f, goCamera)
    private val goShapeRenderer = ShapeRenderer()
    /** SpriteBatch for game-over text (screen-pixel coordinates, like MenuScreen). */
    private val goTextCamera = OrthographicCamera()
    private val goBatch = SpriteBatch()
    /** Stage for Scene2D game-over UI. */
    private val goStage = Stage(FitViewport(800f, 450f))
    private var gameOverUITimer = 0f
    private val skin: Skin get() = UiSkin.instance
    private var isNewRecord = false
    private var showNameEntry = false
    private val predefinedNames = listOf("Pilot", "Ace", "Nova", "Stryker", "Vega", "Orion")
    private val nameEntryButtons = mutableListOf<TextButton>()

    // ── Entity state ──────────────────────────────────────────
    private var ship: Ship = freshShip()
    private val asteroids = mutableListOf<Asteroid>()
    private val lasers = mutableListOf<Laser>()
    private val enemies = mutableListOf<Enemy>()
    private val astronauts = mutableListOf<Astronaut>()
    private val debris = mutableListOf<SpaceDebris>()
    private val events = mutableListOf<GameEvent>()
    private var score = 0
    private var elapsedTime = 0f

    // ── Systems ───────────────────────────────────────────────
    private val physicsSystem = PhysicsSystem()
    private val spawnSystem = SpawnSystem()
    private val collisionSystem = CollisionSystem()
    private val scoreSystem = ScoreSystem()
    private val difficultyManager = DifficultyManager()
    private val mirrorSystem = MirrorSystem()
    private val enemySpawnSystem = EnemySpawnSystem()
    private val astronautSpawnSystem = AstronautSpawnSystem()
    private val debrisSpawnSystem = DebrisSpawnSystem()

    // ── Timers ────────────────────────────────────────────────
    private var laserCooldownTimer = 0f
    /** Set to true when the player fires a laser (consumed by MirrorSystem). */
    private var justFired = false

    // ── Input ─────────────────────────────────────────────────
    private val inputMultiplexer = InputMultiplexer()
    private lateinit var gameInputProcessor: GameInputProcessor

    // ── Flow ──────────────────────────────────────────────────
    /** Whether a game-over overlay is currently showing. */
    private var gameOverTriggered = false

    // ── Lifecycle ─────────────────────────────────────────────

    override fun show() {
        resetGame()

        // Notify HUD of current screen size
        hudRenderer.resize(Gdx.graphics.width, Gdx.graphics.height)

        gameInputProcessor = GameInputProcessor(
            onFlap = {
                ship.velocity.y = Constants.SHIP_FLAP_VELOCITY
            },
            onFire = {
                fireLaser()
                justFired = true
            },
            onDebugGameOver = {
                // Force game over for testing (key: O)
                if (!gameOverTriggered && !ship.isDestroyed) {
                    ship.destroy()
                }
            }
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
        goStage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        if (gameOverTriggered) {
            renderGameOver(delta)
            return
        }

        if (ship.isDestroyed) {
            triggerGameOver()
            renderGameOver(delta)
            return
        }

        // ── Clear ─────────────────────────────────────────────
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cameraSetup.viewport.apply()
        cameraSetup.update()

        // ── Cooldown ──────────────────────────────────────────
        laserCooldownTimer = (laserCooldownTimer - delta).coerceAtLeast(0f)

        // ── Update elapsed time ───────────────────────────────
        elapsedTime += delta

        // ── Update DifficultyManager ──────────────────────────
        difficultyManager.update(elapsedTime)

        // ── Record player action for MirrorSystem ─────────────
        mirrorSystem.recordPlayerAction(
            elapsedTime = elapsedTime,
            ship = ship,
            isShooting = justFired
        )
        justFired = false

        // ── Frame context ─────────────────────────────────────
        val context = GameContext(
            ship = ship,
            asteroids = asteroids,
            lasers = lasers,
            enemies = enemies,
            astronauts = astronauts,
            debris = debris,
            events = events,
            score = score,
            elapsedTime = elapsedTime,
            difficultyManager = difficultyManager,
            mirrorSystem = mirrorSystem
        )

        // ── Systems update (order matters) ────────────────────
        physicsSystem.update(delta, context)
        spawnSystem.update(delta, context)
        enemySpawnSystem.update(delta, context)
        astronautSpawnSystem.update(delta, context)
        debrisSpawnSystem.update(delta, context)
        collisionSystem.update(delta, context)
        scoreSystem.update(delta, context)
        score = context.score

        // ── Enemy laser cleanup ────────────────────────────────
        context.lasers.removeAll { laser ->
            laser.owner != LaserOwner.PLAYER && laser.isExpired
        }

        // ── Event dispatch (Particles + Audio) ─────────────────
        particleManager.onGameEvent(events)
        AudioManager.onGameEvent(events)

        // ── Update particles ──────────────────────────────────
        particleManager.update(delta)

        // ── Update parallax background ────────────────────────
        parallaxBackground.update(delta, difficultyManager.scrollSpeedMultiplier)

        // ── Render game world via GameRenderer ────────────────
        gameRenderer.parallaxBackground = parallaxBackground
        gameRenderer.backgroundTexture = backgroundTexture
        gameRenderer.render(context, elapsedTime)

        // ── Render particles over game world ──────────────────
        batch.begin()
        particleManager.render(batch)
        batch.end()

        // ── HUD overlay (screen-space) ────────────────────────
        hudRenderer.render(ship, score, scoreSystem.formattedTime, scoreSystem.astronautsRescued, i18n)

        // ── Clear frame-scoped events ─────────────────────────
        events.clear()
    }

    // ── Game-over ─────────────────────────────────────────────

    private fun triggerGameOver() {
        if (gameOverTriggered) return
        gameOverTriggered = true

        // Persist final stats
        GameSession.finalScore = score
        GameSession.finalTime = scoreSystem.elapsedTime
        GameSession.finalTimeFormatted = scoreSystem.formattedTime
        GameSession.asteroidsDestroyed = scoreSystem.asteroidsDestroyed
        GameSession.enemiesDestroyed = scoreSystem.enemiesDestroyed
        GameSession.astronautsRescued = scoreSystem.astronautsRescued
        GameSession.astronautsKilled = scoreSystem.astronautsKilled

        // High score check
        val prefs = Gdx.app.getPreferences("nebula-drift")
        val highScore = prefs.getInteger("highScore", 0)
        if (GameSession.finalScore > highScore) {
            prefs.putInteger("highScore", GameSession.finalScore)
            prefs.flush()
            isNewRecord = true
        }

        if (isNewRecord && LeaderboardManager.isHighScore(GameSession.finalScore)) {
            showNameEntry = true
        }

        // Audio
        AudioManager.stopMusic()
        AudioManager.playSound(Constants.SFX_GAME_OVER)
        if (isNewRecord) AudioManager.playSound(Constants.SFX_NEW_RECORD)

        // Build the game-over UI
        try {
            buildGameOverUI()
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Failed to build game-over UI", e)
        }

        // Switch input to game-over stage (no more gameplay input)
        goCamera.position.set(8f, 4.5f, 0f)
        Gdx.input.inputProcessor = goStage
    }

    private fun renderGameOver(delta: Float) {
        gameOverUITimer += delta

        // ── Clear ────────────────────────────────────────────────
        Gdx.gl.glClearColor(0f, 0f, 0.04f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // ── Background (world coords, like MenuScreen) ───────────
        goViewport.apply()
        goCamera.update()
        goShapeRenderer.projectionMatrix = goCamera.combined
        goShapeRenderer.begin(ShapeType.Filled)
        goShapeRenderer.color = Color(0f, 0f, 0.04f, 1f)
        goShapeRenderer.rect(0f, 0f, 16f, 9f)
        goShapeRenderer.end()

        // ── "GAME OVER" title in screen-pixel coords (like MenuScreen.renderTitleText) ──
        val screenW = Gdx.graphics.width.toFloat()
        val screenH = Gdx.graphics.height.toFloat()
        goTextCamera.setToOrtho(false, screenW, screenH)
        goBatch.projectionMatrix = goTextCamera.combined
        goBatch.begin()

        val headingFont = com.nebuladrift.rendering.FontManager.heading()
        headingFont.data.setScale(2f)
        headingFont.color = Color.RED
        val heading = i18n.get("game_over")
        val headingLayout = com.badlogic.gdx.graphics.g2d.GlyphLayout(headingFont, heading)
        val headingX = (screenW - headingLayout.width) / 2f
        headingFont.draw(goBatch, heading, headingX, screenH * 0.65f)
        headingFont.data.setScale(1f)
        headingFont.color = Color.WHITE

        goBatch.end()

        // ── Scene2D Stage ────────────────────────────────────────
        goStage.act(delta)
        goStage.draw()
    }

    private fun buildGameOverUI() {
        goStage.clear()
        nameEntryButtons.clear()

        val root = Table()
        root.setFillParent(true)
        root.defaults().center()

        // ── Heading ────────────────────────────────────────────
        val headingLabel = Label(i18n.get("game_over"), skin.get("heading-white", Label.LabelStyle::class.java))
        headingLabel.color = Color.RED
        root.add(headingLabel).colspan(2).padTop(30f).row()

        // New record banner
        if (isNewRecord) {
            val nrLabel = Label(i18n.get("new_record"), skin.get("small-gold", Label.LabelStyle::class.java))
            val nrBg = Table()
            nrBg.setBackground(skin.get("white", com.badlogic.gdx.scenes.scene2d.utils.Drawable::class.java))
            nrBg.color = Color(0.8f, 0.6f, 0f, 0.25f)
            nrBg.add(nrLabel).pad(6f, 20f, 6f, 20f)
            root.add(nrBg).colspan(2).padTop(10f).padBottom(8f).row()
        }

        // ── Stats ──────────────────────────────────────────────
        val stats = listOf(
            "${i18n.get("score")}: ${GameSession.finalScore}",
            "${i18n.get("time")}: ${GameSession.finalTimeFormatted}",
            "${i18n.get("asteroids_destroyed")}: ${GameSession.asteroidsDestroyed}",
            "${i18n.get("enemies_destroyed")}: ${GameSession.enemiesDestroyed}",
            "${i18n.get("astronauts_rescued")}: ${GameSession.astronautsRescued}"
        )

        val statsTable = Table()
        for (line in stats) {
            statsTable.add(Label(line, skin.get("body-white", Label.LabelStyle::class.java))).padBottom(2f).row()
        }

        if (GameSession.astronautsKilled > 0) {
            statsTable.add(Label(
                "${i18n.get("astronauts_killed")}: ${GameSession.astronautsKilled}",
                skin.get("body-gray", Label.LabelStyle::class.java)
            )).padBottom(2f).row()
        }

        root.add(statsTable).colspan(2).padTop(12f).padBottom(20f).row()

        // ── Name entry ──────────────────────────────────────────
        if (showNameEntry) {
            val nameLabel = Label(i18n.get("enter_name"), skin.get("small-gold", Label.LabelStyle::class.java))
            root.add(nameLabel).colspan(2).padBottom(8f).row()

            val nameGrid = Table()
            predefinedNames.forEachIndexed { index, name ->
                val btn = TextButton(name, skin.get("small-btn", TextButton.TextButtonStyle::class.java))
                nameEntryButtons.add(btn)
                val col = index % 3
                val row = index / 3
                if (col == 0 && row > 0) nameGrid.row()
                nameGrid.add(btn).width(120f).height(36f).pad(4f)

                btn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        LeaderboardManager.addEntry(
                            LeaderboardEntry(
                                name = name,
                                score = GameSession.finalScore,
                                time = GameSession.finalTime,
                                date = java.util.Date().toString()
                            )
                        )
                        showNameEntry = false
                        rebuildWithoutNameEntry()
                    }
                })
            }
            root.add(nameGrid).colspan(2).padBottom(16f).row()
        }

        // ── Action buttons ──────────────────────────────────────
        val retryBtn = TextButton(i18n.get("retry"), skin.get("default", TextButton.TextButtonStyle::class.java))
        val menuBtn = TextButton(i18n.get("main_menu"), skin.get("default", TextButton.TextButtonStyle::class.java))

        root.add(retryBtn).colspan(2).width(240f).height(48f).padBottom(8f).row()
        root.add(menuBtn).colspan(2).width(240f).height(48f).padBottom(12f).row()

        // Leaderboard button (small)
        val lbBtn = TextButton(i18n.get("leaderboard"), skin.get("small-btn", TextButton.TextButtonStyle::class.java))
        root.add(lbBtn).colspan(2).width(200f).height(36f).row()

        goStage.addActor(root)

        // ── Listeners ───────────────────────────────────────────
        retryBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                GameSession.reset()
                // Reset game-over state and restart game
                resetGame()
                isNewRecord = false
                showNameEntry = false
                // Re-init input
                Gdx.input.inputProcessor = inputMultiplexer
            }
        })
        menuBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                GameSession.reset()
                isNewRecord = false
                showNameEntry = false
                gameOverTriggered = false
                if (onExitToMenu != null) {
                    // Android: finish Activity, return to Compose menu
                    onExitToMenu?.invoke()
                } else {
                    // Desktop: transition to libGDX MenuScreen
                    game.startTransition { game.setScreen<MenuScreen>() }
                }
            }
        })
        lbBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.startTransition { game.setScreen<LeaderboardScreen>() }
            }
        })
    }

    private fun rebuildWithoutNameEntry() {
        showNameEntry = false
        buildGameOverUI()
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
            velocity = Vector2(Constants.LASER_SPEED, 0f),
            owner = LaserOwner.PLAYER
        )
        lasers.add(laser)
        events.add(GameEvent.LaserFired(laser))
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun freshShip() = Ship()

    private fun resetGame() {
        ship = freshShip()
        asteroids.clear()
        lasers.clear()
        enemies.clear()
        astronauts.clear()
        debris.clear()
        events.clear()
        laserCooldownTimer = 0f
        score = 0
        elapsedTime = 0f
        justFired = false
        gameOverTriggered = false
        scoreSystem.reset()
        mirrorSystem.reset()
        particleManager.clear()
    }

    // ── Placeholder textures ──────────────────────────────────

    private fun createBackgroundTexture(): Texture {
        val width = 256
        val height = 144
        val pixmap = Pixmap(width, height, Pixmap.Format.RGB888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - width / 2f
                val dy = y - height / 2f
                val dist = kotlin.math.sqrt(dx * dx + dy * dy).toFloat() /
                    kotlin.math.sqrt((width / 2f) * (width / 2f) + (height / 2f) * (height / 2f)).toFloat()
                val brightness = (0.02f + (1f - dist.coerceIn(0f, 1f)) * 0.06f).coerceIn(0f, 1f)
                val r = (brightness * 10f).toInt().coerceIn(0, 255)
                val g = (brightness * 15f).toInt().coerceIn(0, 255)
                val b = (brightness * 40f).toInt().coerceIn(10, 255)
                pixmap.setColor(r / 255f, g / 255f, b / 255f, 1f)
                pixmap.drawPixel(x, y)
            }
        }

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
        gameRenderer.dispose()
        parallaxBackground.dispose()
        batch.dispose()
        hudRenderer.dispose()
        goShapeRenderer.dispose()
        goBatch.dispose()
        goStage.dispose()
        backgroundTexture.dispose()
    }
}
