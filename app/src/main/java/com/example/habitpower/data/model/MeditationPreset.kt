package com.example.habitpower.data.model

data class MeditationPreset(
    val name: String,
    val description: String,
    val durationSeconds: Int,
    val guidanceText: String
)

object MeditationPresets {
    val all = listOf(
        MeditationPreset(
            name = "5-Min Breath Reset",
            description = "Box breathing to clear the mind quickly.",
            durationSeconds = 5 * 60,
            guidanceText = "Breathe in 4 counts · Hold 4 · Out 4 · Hold 4"
        ),
        MeditationPreset(
            name = "10-Min Body Scan",
            description = "Gentle attention moved slowly from head to toe.",
            durationSeconds = 10 * 60,
            guidanceText = "Move your attention through the body without judgment."
        ),
        MeditationPreset(
            name = "20-Min Deep Stillness",
            description = "Extended silence for those with a steady practice.",
            durationSeconds = 20 * 60,
            guidanceText = "Rest as awareness. Let thoughts pass like clouds."
        ),
        MeditationPreset(
            name = "Pranayama",
            description = "Alternate nostril breathing for calm and balance.",
            durationSeconds = 10 * 60,
            guidanceText = "Left in · Right out · Right in · Left out. Slow and steady."
        ),
        MeditationPreset(
            name = "Nidra Wind-Down",
            description = "Guided relaxation on the edge of sleep.",
            durationSeconds = 15 * 60,
            guidanceText = "Lie down. Stay awake on the threshold of sleep."
        )
    )
}
