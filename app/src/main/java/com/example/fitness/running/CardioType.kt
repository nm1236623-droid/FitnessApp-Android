package com.example.fitness.running

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Cardio activity types shown in the Cardio screen.
 */
enum class CardioType(
    val displayName: String,
    val icon: ImageVector,
) {
    SPIN_BIKE("飛輪/健身車", Icons.Filled.PedalBike),
    ELLIPTICAL("橢圓機", Icons.Filled.FitnessCenter),
    ROWING_MACHINE("划船機", Icons.Filled.Rowing),
    STAIR_CLIMBER("登階機/爬樓梯機", Icons.Filled.Stairs),
    CYCLING("騎單車", Icons.Filled.DirectionsBike),
    WALK_OR_JOG("快走/慢跑", Icons.Filled.DirectionsRun),
}
