package com.example.habitpower.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.util.ExerciseImageSupport

@Composable
fun ExerciseImage(
    imageUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    exerciseName: String? = null,
    category: ExerciseCategory? = null,
    detailLabel: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    iconSize: Dp = 40.dp
) {
    val model = remember(imageUri) { ExerciseImageSupport.toImageModel(imageUri) }

    if (model == null) {
        ExerciseImageFallback(
            modifier = modifier,
            exerciseName = exerciseName,
            category = category,
            detailLabel = detailLabel,
            iconSize = iconSize
        )
        return
    }

    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            ExerciseImageFallback(
                modifier = Modifier.fillMaxSize(),
                exerciseName = exerciseName,
                category = category,
                detailLabel = detailLabel,
                iconSize = iconSize
            )
        },
        error = {
            ExerciseImageFallback(
                modifier = Modifier.fillMaxSize(),
                exerciseName = exerciseName,
                category = category,
                detailLabel = detailLabel,
                iconSize = iconSize
            )
        },
        success = { SubcomposeAsyncImageContent() }
    )
}

@Composable
private fun ExerciseImageFallback(
    modifier: Modifier,
    exerciseName: String?,
    category: ExerciseCategory?,
    detailLabel: String?,
    iconSize: Dp
) {
    val backgroundColors = fallbackPalette(category, MaterialTheme.colorScheme)
    val backgroundBrush = Brush.linearGradient(colors = backgroundColors)

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(12.dp)
        ) {
            Icon(
                imageVector = fallbackIcon(category),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(iconSize),
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )

            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category?.displayName ?: "Exercise",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                Text(
                    text = exerciseName ?: "Image coming soon",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                detailLabel
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
            }
        }
    }
}

private fun fallbackPalette(
    category: ExerciseCategory?,
    scheme: androidx.compose.material3.ColorScheme
): List<Color> {
    return when (category) {
        ExerciseCategory.YOGA -> listOf(scheme.primary, scheme.tertiary)
        ExerciseCategory.STRETCHING -> listOf(scheme.secondary, scheme.tertiaryContainer)
        ExerciseCategory.CARDIO_OTHER -> listOf(scheme.tertiary, scheme.primaryContainer)
        else -> listOf(scheme.primaryContainer, scheme.secondaryContainer)
    }
}

private fun fallbackIcon(category: ExerciseCategory?): ImageVector {
    return when (category) {
        ExerciseCategory.YOGA -> Icons.Default.SelfImprovement
        ExerciseCategory.STRETCHING -> Icons.Default.AccessibilityNew
        ExerciseCategory.CARDIO_OTHER -> Icons.Default.DirectionsRun
        else -> Icons.Default.FitnessCenter
    }
}
