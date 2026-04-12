package com.example.habitpower.gamification

/**
 * Seeded motivational content — no network required.
 *
 * Content is indexed by category so callers can pick the most
 * contextually appropriate message.
 */
object MotivationContent {

    // ── Single-habit completion ───────────────────────────────────────────────
    val habitCompletionCheers: List<String> = listOf(
        "That's one more brick in the wall of your future self. 💪",
        "Every check-off is a vote for the person you're becoming.",
        "Small wins compound. You just leveled up — for real.",
        "Done! The discipline you build today shapes who you are tomorrow.",
        "That might have been hard. You did it anyway. That's the definition of growth.",
        "Progress over perfection — and you just made progress.",
        "One habit closer to the best version of you. Keep the streak alive!",
        "Champions are built one habit at a time. There you go.",
        "Your future self is cheering you on right now.",
        "Habits are identity. You just proved who you are.",
        "Not everyone does this. You do. That matters.",
        "Checked off. That's momentum — guard it.",
        "The hardest part is starting. You've already won that battle.",
        "That's what consistency looks like — exactly like this.",
        "Another rep for your future self. The gains are invisible until suddenly they're not."
    )

    // ── Full-day completion celebrations ─────────────────────────────────────
    val dayCompleteCelebrations: List<String> = listOf(
        "ALL IN. Every single habit today. You are exactly who you want to become. 🏆",
        "Perfect day! The person you're becoming is very proud of the person you are today.",
        "Day CRUSHED. No half-measures — you went all the way. That's rare. That's you. 🔥",
        "100%. Every habit. Every promise to yourself — kept. Sleep like a champion tonight.",
        "You did everything you set out to do today. That's identity-level consistency.",
        "Full day complete! The compound interest of habits is quietly making you extraordinary.",
        "Look at that — a perfect day. Your streak is more than numbers; it's a testament to who you are.",
        "Everything on your list, done. You're not just building habits — you're building a life.",
        "Total commitment. Today you proved it again: you are the real deal.",
        "ALL habits complete! Tomorrow you get to do it again — and you already know you can. 💪"
    )

    // ── Streak milestone messages ─────────────────────────────────────────────
    fun streakMilestoneMessage(streak: Int): String = when {
        streak == 3  -> "🔥 3-DAY STREAK!\nThe compound effect is starting. Most people quit here. Not you."
        streak == 7  -> "⚡ ONE FULL WEEK!\nSeven days of keeping promises to yourself. That's a warrior's mindset."
        streak == 14 -> "💪 FORTNIGHT FORCE!\nTwo weeks of daily discipline. You've built something real now."
        streak == 21 -> "🎯 3 WEEKS STRONG!\nResearch says habits take 21 days. Yours just became part of who you are."
        streak == 30 -> "🏆 30 DAYS!\nA full month of consistency. You're in elite territory now."
        streak == 50 -> "👊 FIFTY DAYS!\nHalf a century of showing up. Your identity is set in stone."
        streak == 60 -> "🏅 60-DAY MILESTONE!\nTwo months of daily commitment. Very few people ever get here."
        streak == 90 -> "⭐ 90 DAYS!\nA full quarter of life transformation. The world is starting to notice."
        streak == 100 -> "👑 100-DAY STREAK!\nYou are in the top fraction of a percent. CENTURY achieved. Legendary."
        streak % 100 == 0 -> "🔮 $streak DAYS!!\nYou have transcended ordinary discipline. Truly extraordinary."
        else -> "🔥 $streak-day streak! You're on fire — don't stop now."
    }

    // ── Level-up messages ─────────────────────────────────────────────────────
    fun levelUpMessage(newLevel: Int): String {
        val name = GamificationEngine.levelName(newLevel)
        return when (newLevel) {
            2  -> "⭐ LEVEL 2 — $name!\nYou've taken your first real steps. The journey has begun."
            3  -> "⭐ LEVEL 3 — $name!\nYou're building something solid. Keep showing up."
            4  -> "⭐ LEVEL 4 — $name!\nYour consistency is starting to look like a real achievement."
            5  -> "🌟 LEVEL 5 — $name! 🎉\nHalfway to the first major milestone. You are a WARRIOR now."
            6  -> "🌟 LEVEL 6 — $name!\nSix levels deep — you're in the serious zone now."
            7  -> "🌟 LEVEL 7 — $name!\nChampion status incoming — keep that pace."
            8  -> "💫 LEVEL 8 — $name!\nYou've outpaced most people who ever download habit apps."
            9  -> "💫 LEVEL 9 — $name!\nOne step from double digits. You're basically a habit legend."
            10 -> "🏆 LEVEL 10 — $name! 🎉\nDOUBLE DIGITS! You've joined an incredibly rare group. LEGEND status earned."
            15 -> "🔮 LEVEL 15 — $name!\nTop 1% territory. Your commitment is extraordinary."
            20 -> "👑 LEVEL 20 — ENLIGHTENED! 🎉🎉🎉\nYou have reached the pinnacle. You ARE your habits. Truly Enlightened."
            else -> "⭐ LEVEL $newLevel — $name!\nAnother level unlocked. The climb continues."
        }
    }

    // ── Badge earned messages ─────────────────────────────────────────────────
    fun badgeEarnedTitle(badgeMask: Long): String = when (badgeMask) {
        GamificationEngine.Badge.FIRST_STEP   -> "🌱 Badge Earned: First Step"
        GamificationEngine.Badge.STREAK_3     -> "🔥 Badge Earned: On Fire"
        GamificationEngine.Badge.STREAK_7     -> "⚡ Badge Earned: Weekly Warrior"
        GamificationEngine.Badge.STREAK_14    -> "💪 Badge Earned: Fortnight Force"
        GamificationEngine.Badge.STREAK_30    -> "🏆 Badge Earned: Monthly Master"
        GamificationEngine.Badge.STREAK_100   -> "👑 Badge Earned: Century King"
        GamificationEngine.Badge.CENTURY      -> "💯 Badge Earned: Centurion"
        GamificationEngine.Badge.LEVEL_5      -> "⭐ Badge Earned: Rising Star"
        GamificationEngine.Badge.LEVEL_10     -> "🌟 Badge Earned: Decade Legend"
        GamificationEngine.Badge.LEVEL_MAX    -> "🔮 Badge Earned: Enlightened"
        GamificationEngine.Badge.PERFECT_WEEK -> "🎯 Badge Earned: Perfect Week"
        else -> "🏅 Badge Earned!"
    }

    // ── Morning brief templates ───────────────────────────────────────────────
    fun morningBriefTitle(name: String): String =
        "🌅 Good morning, ${name.ifBlank { "Champion" }}!"

    fun morningBriefBody(
        userName: String,
        yesterdayCompleted: Int,
        yesterdayTotal: Int,
        streak: Int,
        level: Int,
        weakestAreaName: String?,
        todayHabitCount: Int
    ): String = buildString {
        val pct = if (yesterdayTotal > 0) (yesterdayCompleted * 100 / yesterdayTotal) else 0
        if (userName.isNotBlank()) appendLine("$userName, here's your momentum snapshot:")
        appendLine("Yesterday: $yesterdayCompleted / $yesterdayTotal habits ($pct%)")
        if (streak > 0) appendLine("Current streak: ${GamificationEngine.streakLabel(streak)}")
        appendLine("Level: ${GamificationEngine.levelName(level)} (Lvl $level)")
        if (weakestAreaName != null) appendLine("Focus area today: $weakestAreaName ⬆")
        appendLine()
        append("Today has $todayHabitCount habit${if (todayHabitCount != 1) "s" else ""}. Let's close them all.")
    }.trim()

    // ── Evening nudge templates ───────────────────────────────────────────────
    fun eveningNudgeTitle(name: String): String =
        "🌇 Day's not over, ${name.ifBlank { "Champion" }}!"

    fun eveningNudgeBody(completed: Int, total: Int, pendingNames: List<String>): String = buildString {
        appendLine("Progress: $completed / $total habits ✓")
        if (pendingNames.isNotEmpty()) {
            val preview = pendingNames.take(3).joinToString(", ")
            val more = if (pendingNames.size > 3) " +${pendingNames.size - 3} more" else ""
            appendLine("Still to go: $preview$more")
        }
        append("Close strong — your future self is watching.")
    }.trim()

    // ── Day-complete notification ─────────────────────────────────────────────
    fun dayCrushedTitle(name: String): String =
        "🏆 Day Crushed, ${name.ifBlank { "Champion" }}!"

    fun dayCrushedBody(total: Int, streak: Int, xpGained: Int): String = buildString {
        appendLine("All $total habits complete! 🎉")
        if (streak > 0) appendLine("Streak: ${GamificationEngine.streakLabel(streak)}")
        append("+$xpGained XP earned. You're becoming the person you want to be. 💪")
    }.trim()

    // ── Midday nudge (fired only if 0 habits done by noon) ───────────────────
    fun midDayNudgeTitle(name: String): String =
        "⏰ Hey ${name.ifBlank { "there" }}, habits are waiting!"

    fun midDayNudgeBody(total: Int): String =
        "You have $total habit${if (total != 1) "s" else ""} to check off today. A small step now beats regret tonight."

    // ── Random pick helpers ───────────────────────────────────────────────────
    fun randomHabitCheer(): String =
        habitCompletionCheers.random()

    fun randomDayCelebration(): String =
        dayCompleteCelebrations.random()
}
