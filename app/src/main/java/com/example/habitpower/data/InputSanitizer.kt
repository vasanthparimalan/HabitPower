package com.example.habitpower.data

object InputSanitizer {
    private val CONTROL_REGEX = Regex("[\\p{Cntrl}&&[^\\r\\n\\t]]")

    fun sanitize(input: String?, maxLength: Int = 256): String? {
        if (input == null) return null
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        // remove control characters except common whitespace
        val cleaned = CONTROL_REGEX.replace(trimmed, "")
        return if (cleaned.length <= maxLength) cleaned else cleaned.substring(0, maxLength)
    }
}
