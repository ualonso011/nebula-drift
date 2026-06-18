package com.nebuladrift.ui.gameover

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nebuladrift.ui.theme.DangerRed
import com.nebuladrift.ui.theme.NebulaCyan
import com.nebuladrift.ui.theme.NebulaGold
import com.nebuladrift.ui.theme.NebulaPurple
import com.nebuladrift.ui.theme.SpaceBlue
import com.nebuladrift.ui.theme.TextPrimary
import com.nebuladrift.ui.theme.TextSecondary
import com.nebuladrift.util.GameDataBridge
import com.nebuladrift.util.LeaderboardEntry

data class GameOverData(
    val score: Int,
    val timeFormatted: String,
    val asteroidsDestroyed: Int,
    val enemiesDestroyed: Int,
    val astronautsRescued: Int,
    val astronautsKilled: Int,
)

@Composable
fun GameOverScreen(
    data: GameOverData,
    onRetry: () -> Unit,
    onMainMenu: () -> Unit,
    onLeaderboard: () -> Unit,
) {
    val context = LocalContext.current
    val bridge = remember { GameDataBridge(context) }

    // Save high score
    val highScore = remember {
        val prefsHigh = bridge.getHighScore()
        if (data.score > prefsHigh) {
            bridge.setHighScore(data.score)
            data.score
        } else prefsHigh
    }
    val isNewRecord = data.score == highScore && data.score > 0

    // Save to leaderboard if qualifies
    remember {
        if (bridge.isHighScore(data.score)) {
            bridge.addLeaderboardEntry(
                LeaderboardEntry(
                    name = "Pilot",
                    score = data.score,
                    time = parseSeconds(data.timeFormatted),
                    date = java.util.Date().toString(),
                ),
            )
        }
    }

    // Animate score
    val animatedScore by animateIntAsState(
        targetValue = data.score,
        animationSpec = tween(1500),
        label = "scoreAnim",
    )

    // Title glow
    val infiniteTransition = rememberInfiniteTransition(label = "goGlow")
    val titleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "goGlow",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF07091E), SpaceBlue, Color(0xFF050718)),
                ),
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        // GAME OVER title
        Text(
            text = "GAME OVER",
            style = MaterialTheme.typography.headlineLarge,
            color = DangerRed.copy(alpha = titleAlpha),
            letterSpacing = 6.sp,
            fontWeight = FontWeight.Black,
        )

        // New record badge
        if (isNewRecord) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "NEW RECORD!",
                style = MaterialTheme.typography.bodyLarge,
                color = NebulaGold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
        }

        Spacer(Modifier.height(32.dp))

        // Score
        Text(
            text = animatedScore.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = NebulaCyan,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.height(8.dp))

        // Stats
        val stats = listOf(
            "Time" to data.timeFormatted,
            "Asteroids" to data.asteroidsDestroyed.toString(),
            "Enemies" to data.enemiesDestroyed.toString(),
            "Rescued" to data.astronautsRescued.toString(),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            stats.forEach { (label, value) ->
                Text(
                    text = "$label  $value",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            if (data.astronautsKilled > 0) {
                Text(
                    text = "Lost  ${data.astronautsKilled}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DangerRed.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.weight(0.3f))

        // Retry button
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NebulaPurple,
                contentColor = Color.White,
            ),
        ) {
            Text(
                "RETRY",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Main Menu
        Button(
            onClick = onMainMenu,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A2040),
                contentColor = TextPrimary,
            ),
        ) {
            Text(
                "MAIN MENU",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Leaderboard
        Text(
            text = "LEADERBOARD",
            style = MaterialTheme.typography.bodyMedium,
            color = NebulaCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(8.dp)
                .clickable { onLeaderboard() },
        )

        Spacer(Modifier.height(16.dp))
    }
}

private fun parseSeconds(formatted: String): Float {
    val parts = formatted.split(":")
    if (parts.size == 2) {
        return parts[0].toFloatOrNull()?.let { it * 60 } ?: 0f +
                parts[1].toFloatOrNull() ?: 0f
    }
    return 0f
}
