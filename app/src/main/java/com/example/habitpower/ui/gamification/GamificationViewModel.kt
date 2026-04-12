package com.example.habitpower.ui.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.gamification.GamificationEngine
import com.example.habitpower.gamification.GamificationRepository
import com.example.habitpower.gamification.MotivationContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GamificationUiState(
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val totalXp: Int = 0,
    val level: Int = 1,
    val levelName: String = "Seeker",
    val levelProgress: Float = 0f,
    val xpLabel: String = "0 / 100 XP",
    val totalDaysPerfect: Int = 0,
    val earnedBadges: List<GamificationEngine.Badge.Metadata> = emptyList()
)

data class CelebrationEvent(
    val quote: String = "",
    val xpGained: Int = 0,
    val didLevelUp: Boolean = false,
    val newLevelName: String = "",
    val levelUpMessage: String = "",
    val isDayPerfect: Boolean = false,
    val streakMilestoneMessage: String? = null,
    val newBadges: List<GamificationEngine.Badge.Metadata> = emptyList()
)

class GamificationViewModel(
    private val habitPowerRepository: HabitPowerRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamificationUiState())
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    /** Non-null when an in-app celebration should be shown. Caller clears it via [clearCelebration]. */
    private val _pendingCelebration = MutableStateFlow<CelebrationEvent?>(null)
    val pendingCelebration: StateFlow<CelebrationEvent?> = _pendingCelebration.asStateFlow()

    init {
        observeActiveUser()
    }

    private fun observeActiveUser() {
        viewModelScope.launch {
            habitPowerRepository.getResolvedActiveUser().collectLatest { user ->
                if (user == null) return@collectLatest
                gamificationRepository.observeStats(user.id).collectLatest { stats ->
                    _uiState.value = GamificationUiState(
                        streak = stats.currentStreak,
                        longestStreak = stats.longestStreak,
                        totalXp = stats.totalXp,
                        level = stats.level,
                        levelName = GamificationEngine.levelName(stats.level),
                        levelProgress = GamificationEngine.levelProgress(stats.totalXp),
                        xpLabel = GamificationEngine.xpLabel(stats.totalXp),
                        totalDaysPerfect = stats.totalDaysPerfect,
                        earnedBadges = GamificationEngine.Badge.ALL.filter { (stats.earnedBadgesMask and it.id) != 0L }
                    )
                }
            }
        }
    }

    /**
     * Called by DailyCheckInScreen after the user saves their check-in.
     * Computes and persists updated stats, then populates [pendingCelebration].
     */
    fun onCheckInSaved(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val user = habitPowerRepository.getResolvedActiveUser().first() ?: return@launch
            val result = gamificationRepository.onDayCheckedIn(user.id, date)

            val streakMsg = if (result.isDayPerfect) {
                val streak = result.stats.currentStreak
                when (streak) {
                    3, 7, 14, 21, 30, 50, 60, 90, 100 -> MotivationContent.streakMilestoneMessage(streak)
                    else -> if (streak > 0 && streak % 100 == 0) MotivationContent.streakMilestoneMessage(streak) else null
                }
            } else null

            val quote = if (result.isDayPerfect)
                MotivationContent.randomDayCelebration()
            else
                MotivationContent.randomHabitCheer()

            _pendingCelebration.value = CelebrationEvent(
                quote = quote,
                xpGained = result.xpGained,
                didLevelUp = result.didLevelUp,
                newLevelName = if (result.didLevelUp) GamificationEngine.levelName(result.stats.level) else "",
                levelUpMessage = if (result.didLevelUp) MotivationContent.levelUpMessage(result.stats.level) else "",
                isDayPerfect = result.isDayPerfect,
                streakMilestoneMessage = streakMsg,
                newBadges = result.newBadges
            )
        }
    }

    fun clearCelebration() {
        _pendingCelebration.value = null
    }
}
