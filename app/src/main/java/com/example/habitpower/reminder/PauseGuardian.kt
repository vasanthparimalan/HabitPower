package com.example.habitpower.reminder

import com.example.habitpower.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

object PauseGuardian {

    suspend fun isActive(prefs: UserPreferencesRepository): Boolean {
        val active = prefs.stepBackActive.first()
        if (!active) return false
        val returnDay = prefs.stepBackReturnEpochDay.first()
        if (returnDay != null && LocalDate.ofEpochDay(returnDay) < LocalDate.now()) {
            prefs.setStepBack(false, null)
            return false
        }
        return true
    }
}
