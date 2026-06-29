package com.example.habitpower.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashLogger {

    private val instance: FirebaseCrashlytics?
        get() = runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()

    fun log(message: String) {
        instance?.log(message)
    }

    fun setUserId(userId: Long) {
        instance?.setUserId(userId.toString())
    }

    fun setKey(key: String, value: String) {
        instance?.setCustomKey(key, value)
    }

    fun setKey(key: String, value: Int) {
        instance?.setCustomKey(key, value)
    }

    fun setKey(key: String, value: Boolean) {
        instance?.setCustomKey(key, value)
    }

    fun recordException(e: Throwable) {
        instance?.recordException(e)
    }
}
