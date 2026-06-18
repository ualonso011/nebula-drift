package com.nebuladrift.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nebuladrift.ui.theme.NebulaCyan
import com.nebuladrift.ui.theme.NebulaGold
import com.nebuladrift.ui.theme.SpaceBlue
import com.nebuladrift.ui.theme.SpaceSurface
import com.nebuladrift.ui.theme.SpaceSurfaceVariant
import com.nebuladrift.ui.theme.TextMuted
import com.nebuladrift.ui.theme.TextPrimary
import com.nebuladrift.util.GameDataBridge

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val bridge = remember { GameDataBridge(context) }

    var musicVolume by remember { mutableFloatStateOf(bridge.getMusicVolume()) }
    var sfxVolume by remember { mutableFloatStateOf(bridge.getSfxVolume()) }

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
                text = "SETTINGS",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.weight(1f))
        }

        HorizontalDivider(color = SpaceSurfaceVariant, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Music volume
            SettingSection(
                title = "Music Volume",
                value = musicVolume,
                onValueChange = { v ->
                    musicVolume = v
                    bridge.setMusicVolume(v)
                },
            )

            // SFX volume
            SettingSection(
                title = "SFX Volume",
                value = sfxVolume,
                onValueChange = { v ->
                    sfxVolume = v
                    bridge.setSfxVolume(v)
                },
            )

            Spacer(Modifier.height(12.dp))

            // About
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceSurface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Nebula Drift v0.4.0",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = NebulaGold,
            )
        }

        Spacer(Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 19,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = NebulaCyan,
                activeTrackColor = NebulaCyan,
                inactiveTrackColor = SpaceSurfaceVariant,
            ),
        )
    }
}
