package com.example.habitpower.util

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseImageSupportTest {

    @Test
    fun slugifyExerciseName_normalizesPunctuation() {
        assertEquals("push_up", ExerciseImageSupport.slugifyExerciseName("Push-Up"))
        assertEquals("sun_salutation_a", ExerciseImageSupport.slugifyExerciseName("Sun Salutation A"))
        assertEquals("farmer_s_carry", ExerciseImageSupport.slugifyExerciseName("Farmer's Carry"))
    }

    @Test
    fun resolveBundledAssetUri_prefersExistingSupportedExtensions() {
        val uri = ExerciseImageSupport.resolveBundledAssetUri(
            exerciseName = "Push-Up",
            assetNames = setOf("push_up.png", "push_up.webp")
        )

        assertEquals("file:///android_asset/exercises/push_up.webp", uri)
    }

    @Test
    fun normalizePersistedImageUri_mapsRelativeAssetPaths() {
        assertEquals(
            "file:///android_asset/exercises/push_up.png",
            ExerciseImageSupport.normalizePersistedImageUri("push_up.png")
        )
        assertEquals(
            "file:///android_asset/exercises/push_up.png",
            ExerciseImageSupport.normalizePersistedImageUri("exercises/push_up.png")
        )
    }

    @Test
    fun toImageModel_returnsFileForAbsolutePath() {
        val model = ExerciseImageSupport.toImageModel("/data/user/0/example/files/push_up.webp")

        assertTrue(model is File)
        assertEquals(
            "/data/user/0/example/files/push_up.webp",
            (model as File).invariantSeparatorsPath
        )
    }
}
