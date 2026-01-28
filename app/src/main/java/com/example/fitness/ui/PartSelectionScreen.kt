package com.example.fitness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row // Fixed: Added missing import
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width // Fixed: Added missing import
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AirlineSeatReclineExtra
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fitness.ui.theme.TechColors
import com.example.fitness.ui.theme.glassEffect

private data class TrainingPartUi(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label,
)

@Composable
fun PartSelectionScreen(
    onSelect: (String) -> Unit,
    onCancel: () -> Unit = {},
) {
    val parts = listOf(
        TrainingPartUi("chest", "胸", Icons.Default.Favorite, contentDescription = "胸部"),
        TrainingPartUi("back", "背", Icons.Default.AirlineSeatReclineExtra, contentDescription = "背部"),
        TrainingPartUi("legs", "腿", Icons.Default.DirectionsRun, contentDescription = "腿部"),
        TrainingPartUi("arms", "手臂", Icons.Default.FitnessCenter, contentDescription = "手臂"),
        TrainingPartUi("shoulders", "肩", Icons.Default.AccessibilityNew, contentDescription = "肩膀"),
        TrainingPartUi("abs", "腹", Icons.Default.SportsMartialArts, contentDescription = "腹部"),
    )

    // Global Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(TechColors.DarkBlue, TechColors.DeepPurple)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Neon Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(cornerRadius = 20.dp)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TechColors.NeonBlue
                        )
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "部位選擇",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TechColors.NeonBlue
                        )
                        Text(
                            text = "選擇今天要訓練的目標肌群",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Adaptive Grid
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val columns = when {
                    maxWidth < 420.dp -> 2
                    maxWidth < 720.dp -> 3
                    else -> 4
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(parts, key = { it.key }) { part ->
                        PartTile(
                            part = part,
                            onClick = { onSelect(part.key) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartTile(
    part: TrainingPartUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Glass Effect Tile
    Box(
        modifier = modifier
            .height(120.dp)
            .glassEffect(cornerRadius = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Neon Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        TechColors.NeonBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = part.icon,
                    contentDescription = part.contentDescription,
                    tint = TechColors.NeonBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = part.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}