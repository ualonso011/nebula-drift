package com.nebuladrift.ui.menu

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nebuladrift.ui.theme.NebulaCyan
import com.nebuladrift.ui.theme.NebulaGold
import com.nebuladrift.ui.theme.NebulaPurple
import com.nebuladrift.ui.theme.SpaceBlue
import com.nebuladrift.ui.theme.TextMuted
import com.nebuladrift.util.GameDataBridge
import kotlin.random.Random

@Composable
fun MenuScreen(
    onPlayClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val context = LocalContext.current
    val bridge = remember { GameDataBridge(context) }
    val highScore = remember { bridge.getHighScore() }

    // Title glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "titleGlow")
    val titleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "titleGlow",
    )

    // Play button pulse
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "playPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07091E),
                        SpaceBlue,
                        Color(0xFF050718),
                    ),
                ),
            ),
    ) {
        // Starfield background
        val stars = remember {
            List(80) {
                Star(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    radius = 0.5f + Random.nextFloat() * 1.5f,
                    alpha = 0.3f + Random.nextFloat() * 0.7f,
                )
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha),
                    radius = star.radius * density,
                    center = Offset(star.x * size.width, star.y * size.height),
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(0.3f))

            // Title
            Text(
                text = "NEBULA",
                style = MaterialTheme.typography.displayLarge,
                color = NebulaCyan.copy(alpha = titleAlpha),
                textAlign = TextAlign.Center,
                letterSpacing = 8.sp,
            )
            Text(
                text = "DRIFT",
                style = MaterialTheme.typography.displayLarge,
                color = NebulaPurple.copy(alpha = titleAlpha * 0.9f),
                textAlign = TextAlign.Center,
                letterSpacing = 8.sp,
            )

            Spacer(Modifier.height(48.dp))

            // Play button
            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NebulaCyan,
                    contentColor = Color(0xFF003544),
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp * pulse,
                ),
            ) {
                Text(
                    text = "PLAY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                )
            }

            Spacer(Modifier.height(32.dp))

            // Secondary buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(
                    onClick = onLeaderboardClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary,
                    ),
                    border = ButtonDefaults.outlinedButtonBorder
                        ?.copy(width = 1.dp),
                ) {
                    Text(
                        text = "LEADERBOARD",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                OutlinedButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary,
                    ),
                    border = ButtonDefaults.outlinedButtonBorder
                        ?.copy(width = 1.dp),
                ) {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // High score
            if (highScore > 0) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "BEST: $highScore",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NebulaGold,
                )
            }

            Spacer(Modifier.weight(0.5f))

            // Version
            Text(
                text = "v0.4.0",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
)
