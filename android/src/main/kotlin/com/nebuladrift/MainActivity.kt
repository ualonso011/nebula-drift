package com.nebuladrift

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nebuladrift.ui.gameover.GameOverData
import com.nebuladrift.ui.gameover.GameOverScreen
import com.nebuladrift.ui.leaderboard.LeaderboardScreen
import com.nebuladrift.ui.menu.MenuScreen
import com.nebuladrift.ui.settings.SettingsScreen
import com.nebuladrift.ui.theme.SpaceTheme

/**
 * Main Activity — Compose-based menus for Nebula Drift.
 *
 * Delegates gameplay to [GameActivity] via an Intent and receives
 * the game result (score, stats) through extras.
 */
class MainActivity : ComponentActivity() {

    private var activeGameOverData: GameOverData? = null

    private val gameLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val gr = GameActivity.pendingGameResult
            if (gr == null) {
                // Result signal arrived before the @Volatile write
                // propagated — rare but handle gracefully.
                return@registerForActivityResult
            }
            GameActivity.pendingGameResult = null // consume immediately
            activeGameOverData = GameOverData(
                score = gr.score,
                timeFormatted = gr.timeFormatted,
                asteroidsDestroyed = gr.asteroidsDestroyed,
                enemiesDestroyed = gr.enemiesDestroyed,
                astronautsRescued = gr.astronautsRescued,
                astronautsKilled = gr.astronautsKilled,
            )
            setContent {
                SpaceTheme {
                    NebulaDriftApp(
                        gameOverData = activeGameOverData,
                        onLaunchGame = { launchGame() },
                        onGameOverConsumed = { activeGameOverData = null },
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SpaceTheme {
                NebulaDriftApp(
                    gameOverData = null,
                    onLaunchGame = { launchGame() },
                    onGameOverConsumed = {},
                )
            }
        }
    }

    private fun launchGame() {
        gameLauncher.launch(
            Intent(this@MainActivity, GameActivity::class.java),
        )
    }
}

@Composable
fun NebulaDriftApp(
    gameOverData: GameOverData?,
    onLaunchGame: () -> Unit,
    onGameOverConsumed: () -> Unit,
) {
    var screen by remember { mutableStateOf(if (gameOverData != null) "gameover" else "menu") }

    // Reset game data on navigation away from game over
    if (screen != "gameover" && gameOverData != null) {
        onGameOverConsumed()
    }

    when (screen) {
        "menu" -> MenuScreen(
            onPlayClick = {
                onLaunchGame()
                screen = "menu" // stay on menu while game runs
            },
            onLeaderboardClick = { screen = "leaderboard" },
            onSettingsClick = { screen = "settings" },
        )
        "gameover" -> {
            if (gameOverData != null) {
                GameOverScreen(
                    data = gameOverData,
                    onRetry = { onLaunchGame() },
                    onMainMenu = {
                        onGameOverConsumed()
                        screen = "menu"
                    },
                    onLeaderboard = { screen = "leaderboard" },
                )
            } else {
                screen = "menu"
            }
        }
        "leaderboard" -> LeaderboardScreen(
            onBackClick = { screen = "menu" },
        )
        "settings" -> SettingsScreen(
            onBackClick = { screen = "menu" },
        )
    }
}
