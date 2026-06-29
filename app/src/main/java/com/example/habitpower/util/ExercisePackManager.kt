package com.example.habitpower.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.ExerciseCategory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ExercisePackManager {

    private const val PACK_VERSION = 1
    private const val MIME_TYPE = "application/octet-stream"

    data class PackItem(
        val name: String,
        val description: String,
        val category: ExerciseCategory,
        val instructions: String?,
        val tags: String,
        val wgerExerciseId: Int?
    )

    fun serialize(exercises: List<Exercise>): String {
        val root = JSONObject()
        root.put("version", PACK_VERSION)
        root.put("exported_by", "HabitPower")
        val arr = JSONArray()
        exercises.forEach { e ->
            val obj = JSONObject()
            obj.put("name", e.name)
            obj.put("description", e.description)
            obj.put("category", e.category.name)
            obj.put("instructions", e.instructions ?: JSONObject.NULL)
            obj.put("tags", e.tags)
            obj.put("wger_id", e.wgerExerciseId ?: JSONObject.NULL)
            arr.put(obj)
        }
        root.put("exercises", arr)
        return root.toString(2)
    }

    fun parse(json: String): List<PackItem> {
        return try {
            val root = JSONObject(json)
            val arr = root.getJSONArray("exercises")
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                PackItem(
                    name = obj.getString("name"),
                    description = obj.optString("description"),
                    category = runCatching {
                        ExerciseCategory.valueOf(obj.getString("category"))
                    }.getOrDefault(ExerciseCategory.STRENGTH),
                    instructions = obj.optString("instructions").takeIf { it.isNotBlank() && it != "null" },
                    tags = obj.optString("tags"),
                    wgerExerciseId = obj.optInt("wger_id", -1).takeIf { it > 0 }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun share(context: Context, exercises: List<Exercise>) {
        if (exercises.isEmpty()) return
        val json = serialize(exercises)
        val file = File(context.cacheDir, "exercises_pack.hpex")
        file.writeText(json)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "HabitPower Exercise Pack")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Exercise Pack"))
    }

    fun readFromUri(context: Context, uri: Uri): List<PackItem> {
        return try {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() } ?: return emptyList()
            parse(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
