package com.example.habitpower.data

import android.content.Context
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.data.model.ExerciseLibraryItem
import com.example.habitpower.util.ExerciseImageSupport
import org.json.JSONArray
import java.util.Locale

class ExerciseLibraryRepository(private val context: Context) {

    private val items: List<ExerciseLibraryItem> by lazy { loadFromAssets() }
    private val bundledExerciseAssets: Set<String> by lazy {
        runCatching {
            context.assets.list("exercises")
                ?.map { it.lowercase(Locale.US) }
                ?.toSet()
                ?: emptySet()
        }.getOrDefault(emptySet())
    }

    fun getAll(): List<ExerciseLibraryItem> = items

    fun getByCategory(cat: ExerciseCategory): List<ExerciseLibraryItem> =
        items.filter { it.category == cat }

    fun search(query: String): List<ExerciseLibraryItem> =
        items.filter { it.name.contains(query, ignoreCase = true) }

    fun searchInCategory(query: String, cat: ExerciseCategory?): List<ExerciseLibraryItem> =
        if (cat == null) search(query) else items.filter {
            it.category == cat && it.name.contains(query, ignoreCase = true)
        }

    private fun loadFromAssets(): List<ExerciseLibraryItem> {
        return try {
            val json = context.assets.open("exercise_library.json")
                .bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                val name = obj.getString("name")
                val wgerId = obj.optInt("wger_id", -1).takeIf { it > 0 }
                val imageUri = obj.optString("imageUri")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?.let(ExerciseImageSupport::normalizePersistedImageUri)
                    ?: wgerId?.let { id ->
                        val candidate = "$id.webp"
                        if (bundledExerciseAssets.contains(candidate.lowercase(Locale.US)))
                            ExerciseImageSupport.toBundledAssetUri(candidate)
                        else null
                    }
                    ?: ExerciseImageSupport.resolveBundledAssetUri(name, bundledExerciseAssets)
                ExerciseLibraryItem(
                    name = name,
                    category = runCatching {
                        ExerciseCategory.valueOf(obj.getString("category"))
                    }.getOrDefault(ExerciseCategory.STRENGTH),
                    primaryMuscle = obj.optString("primaryMuscle").takeIf { it.isNotBlank() },
                    instructions = obj.optString("instructions").takeIf { it.isNotBlank() },
                    imageUri = imageUri,
                    wgerExerciseId = wgerId
                )
            }.sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
