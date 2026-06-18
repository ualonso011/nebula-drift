package com.nebuladrift.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nebuladrift.ui.theme.MedalBronze
import com.nebuladrift.ui.theme.MedalGold
import com.nebuladrift.ui.theme.MedalSilver
import com.nebuladrift.ui.theme.NebulaCyan
import com.nebuladrift.ui.theme.NebulaGold
import com.nebuladrift.ui.theme.SpaceBlue
import com.nebuladrift.ui.theme.SpaceSurface
import com.nebuladrift.ui.theme.SpaceSurfaceVariant
import com.nebuladrift.ui.theme.TextMuted
import com.nebuladrift.ui.theme.TextPrimary
import com.nebuladrift.util.GameDataBridge
import com.nebuladrift.util.LeaderboardEntry

@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val bridge = remember { GameDataBridge(context) }
    val entries = remember { bridge.getLeaderboardEntries() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF07091E), SpaceBlue, Color(0xFF050718)),
                ),
            ),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "< BACK",
                style = MaterialTheme.typography.labelLarge,
                color = NebulaCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBackClick() },
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "LEADERBOARD",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.weight(1f))
            // Placeholder for symmetry
            Spacer(Modifier.width(48.dp))
        }

        HorizontalDivider(color = SpaceSurfaceVariant, thickness = 1.dp)

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No entries yet.\nPlay a game to set your first record!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(entries) { index, entry ->
                    LeaderboardRow(index = index, entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(index: Int, entry: LeaderboardEntry) {
    val medalColor = when (index) {
        0 -> MedalGold
        1 -> MedalSilver
        2 -> MedalBronze
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpaceSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Position
        if (medalColor != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(medalColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = medalColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.width(12.dp))

        // Name
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )

        // Score
        Text(
            text = entry.score.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = NebulaGold,
            fontWeight = FontWeight.Bold,
        )
    }
}
