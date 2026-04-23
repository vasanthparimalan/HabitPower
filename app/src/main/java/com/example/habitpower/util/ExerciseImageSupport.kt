package com.example.habitpower.util

import java.io.File
import java.util.Locale

object ExerciseImageSupport {
    private val bundledExtensions = listOf("webp", "png", "jpg", "jpeg", "svg")
    private val supportedRelativeExtensions = bundledExtensions.toSet()
    private val windowsAbsolutePathPattern = Regex("^[A-Za-z]:[\\\\/].*")

    fun slugifyExerciseName(name: String): String {
        return name
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    fun bundledAssetCandidates(exerciseName: String): List<String> {
        val slug = slugifyExerciseName(exerciseName)
        if (slug.isBlank()) return emptyList()
        return bundledExtensions.map { "$slug.$it" }
    }

    fun resolveBundledAssetUri(exerciseName: String, assetNames: Set<String>): String? {
        val match = bundledAssetCandidates(exerciseName)
            .firstOrNull { candidate -> assetNames.contains(candidate.lowercase(Locale.US)) }
            ?: return null
        return toBundledAssetUri(match)
    }

    fun toBundledAssetUri(fileName: String): String {
        val normalizedFileName = fileName.removePrefix("/").removePrefix("exercises/")
        return "file:///android_asset/exercises/$normalizedFileName"
    }

    fun normalizePersistedImageUri(raw: String?): String? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            trimmed.startsWith("file:///android_asset/") -> trimmed
            trimmed.startsWith("asset:///") -> "file:///android_asset/${trimmed.removePrefix("asset:///")}"
            trimmed.startsWith("asset://") -> "file:///android_asset/${trimmed.removePrefix("asset://")}"
            trimmed.startsWith("android.resource://") -> trimmed
            trimmed.startsWith("content://") -> trimmed
            trimmed.startsWith("http://") -> trimmed
            trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("file://") -> trimmed
            looksLikeAbsoluteFilePath(trimmed) -> trimmed
            looksLikeBundledRelativePath(trimmed) -> {
                val path = trimmed.removePrefix("/").removePrefix("exercises/")
                toBundledAssetUri(path)
            }
            else -> trimmed
        }
    }

    fun toImageModel(raw: String?): Any? {
        val normalized = normalizePersistedImageUri(raw) ?: return null
        return when {
            looksLikeAbsoluteFilePath(normalized) -> File(normalized)
            else -> normalized
        }
    }

    private fun looksLikeBundledRelativePath(value: String): Boolean {
        if (value.contains("://")) return false
        if (looksLikeAbsoluteFilePath(value)) return false
        val extension = value.substringAfterLast('.', "").lowercase(Locale.US)
        return extension in supportedRelativeExtensions
    }

    private fun looksLikeAbsoluteFilePath(value: String): Boolean {
        return value.startsWith("/") || windowsAbsolutePathPattern.matches(value)
    }
}
