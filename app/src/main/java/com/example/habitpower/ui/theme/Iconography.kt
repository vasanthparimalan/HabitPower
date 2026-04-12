package com.example.habitpower.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icon intent map.
 * Keep icon meanings stable across screens so users build reliable habits of use.
 */
object AppIconography {
    val Dashboard: ImageVector = Icons.Default.DateRange
    val Routines: ImageVector = Icons.AutoMirrored.Filled.List
    val Focus: ImageVector = Icons.Default.PlayArrow
    val Analytics: ImageVector = Icons.Default.Insights
}
