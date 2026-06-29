package com.example.habitpower.util

import android.content.Context
import android.util.Log
import java.io.File

object LocalCrashHandler {

    private const val TAG = "HabitPower/Crash"
    private const val CRASH_FILE = "last_crash.txt"

    var lastCrashLog: String? = null
        private set

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val content = buildCrashReport(thread, throwable)
                File(appContext.filesDir, CRASH_FILE).writeText(content)
                Log.e(TAG, content)
            } catch (_: Exception) {}

            // Forward to Crashlytics (best-effort — may not flush before process dies)
            CrashLogger.recordException(throwable)

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun checkPreviousCrash(context: Context) {
        val file = File(context.filesDir, CRASH_FILE)
        if (!file.exists()) return
        val content = runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return
        lastCrashLog = content
        // Push first 3000 chars to Crashlytics breadcrumb log on this session
        CrashLogger.log("PREV_CRASH:\n${content.take(3000)}")
        Log.e(TAG, "Previous crash detected — see Admin > Crash Log")
    }

    fun clearCrashLog(context: Context) {
        lastCrashLog = null
        runCatching { File(context.filesDir, CRASH_FILE).delete() }
    }

    private fun buildCrashReport(thread: Thread, throwable: Throwable): String = buildString {
        appendLine("=== HabitPower Crash Report ===")
        appendLine("Time   : ${java.time.Instant.now()}")
        appendLine("Thread : ${thread.name}")
        appendLine()
        appendLine("${throwable::class.qualifiedName}: ${throwable.message}")
        appendLine(throwable.stackTraceToString())
        var cause = throwable.cause
        while (cause != null) {
            appendLine("Caused by: ${cause::class.qualifiedName}: ${cause.message}")
            appendLine(cause.stackTraceToString())
            cause = cause.cause
        }
    }
}
