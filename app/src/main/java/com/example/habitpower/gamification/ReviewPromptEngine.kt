package com.example.habitpower.gamification

/**
 * Generates human-readable insights and reflection prompts for each review cadence.
 * All functions are pure — no Android dependencies. Data is passed in; prompts come out.
 */
object ReviewPromptEngine {

    // ── Weekly ────────────────────────────────────────────────────────────────

    fun weeklyInsights(
        consistency: Int,
        topHabit: String?,
        totalActive: Int,
        _completedToday: Int
    ): List<String> = buildList {
        when {
            consistency >= 80 -> add("You showed up $consistency% of the time this week — that's compounding identity, not just habit.")
            consistency >= 60 -> add("$consistency% this week. Solid. You're in the building phase — keep the chain alive.")
            consistency >= 40 -> add("$consistency% this week. More than half your habits need attention. What's one you can protect unconditionally?")
            consistency > 0  -> add("$consistency% this week. Hard weeks aren't failures — they're data. What pattern created this gap?")
            else             -> add("No completions logged yet this week. One habit done today starts the rebuild.")
        }
        topHabit?.let {
            add("\"$it\" was your most consistent habit this week. It's the foundation — build from it.")
        }
        if (totalActive >= 7 && consistency < 50) {
            add("You have $totalActive active habits but under 50% completion. Fewer habits done daily beats many habits skipped.")
        }
    }

    fun weeklyReflectionPrompt(consistency: Int, topHabit: String?): String = when {
        consistency >= 80 -> "What made this week click? Write down one thing so you can recreate it."
        consistency >= 50 -> "Which one habit — if done every single day — would make next week feel like a win?"
        topHabit != null  -> "\"$topHabit\" held this week. What would happen if you protected it as non-negotiable for 30 days?"
        else              -> "If you could only keep one habit for the next 7 days, which one matters most to who you're becoming?"
    }

    // ── Monthly ───────────────────────────────────────────────────────────────

    fun monthlyInsights(
        consistency: Int,
        thrivingCount: Int,
        steadyCount: Int,
        _strugglingCount: Int,
        staleCount: Int,
        anchorHabitName: String?,
        topIdentity: String?,
        totalActive: Int
    ): List<String> = buildList {
        when {
            consistency >= 75 -> add("Strong 30 days — $consistency% overall. This is what identity-building looks like in practice.")
            consistency >= 50 -> add("$consistency% over 30 days. Consistency above 50% means you're showing up more than not — that matters.")
            else              -> add("$consistency% over 30 days. This month is a signal: something in the system needs to change.")
        }
        if (thrivingCount > 0) add("$thrivingCount habit${pl(thrivingCount)} thriving at 80%+. These are becoming automatic — who you are, not what you do.")
        if (steadyCount > 0)   add("$steadyCount habit${pl(steadyCount)} steady at 50–79%. Close to locked in — one small lift would tip them to thriving.")
        if (staleCount > 0)    add("$staleCount habit${pl(staleCount)} under 20% completion. Honest question: are these still yours, or did life outgrow them?")
        anchorHabitName?.let { add("\"$it\" is your anchor this month — completing it makes the rest of your day follow.") }
        topIdentity?.let { add("$it — you've built this identity through 30 days of repeated action.") }
        if (totalActive >= 9) add("$totalActive active habits is a heavy load. Research consistently shows fewer, deeper habits beat a wide shallow stack.")
    }

    fun monthlyReflectionPrompt(staleCount: Int, thrivingCount: Int, totalActive: Int): String = when {
        staleCount > thrivingCount   -> "Which 3 habits are non-negotiable to who you're becoming? Everything else is optional for now."
        thrivingCount >= 3           -> "You have $thrivingCount habits that are thriving. Which one feels completely automatic — ready to graduate?"
        totalActive >= 7             -> "If you had to cut your habit list in half this month, which ones survive? Start there."
        else                         -> "What's one habit you could make so small it becomes impossible to skip, even on the hardest day?"
    }

    // ── Quarterly / Season ────────────────────────────────────────────────────

    fun quarterlyInsights(
        overallPercent: Int,
        activeHabitCount: Int,
        anchorHabit: String?,
        mostConsistentHabit: String?,
        graduateCandidate: String?,
        retireCandidate: String?
    ): List<String> = buildList {
        add("This 90-day season: $overallPercent% overall across $activeHabitCount habit${pl(activeHabitCount)}.")
        anchorHabit?.let { add("\"$it\" anchored this season. It's the keystone — everything else follows when this is done.") }
        mostConsistentHabit?.let { add("\"$it\" was your most consistent habit this season. This is becoming part of your identity.") }
        graduateCandidate?.let { add("\"$it\" hit 85%+ — consider graduating it. It may no longer need daily tracking.") }
        retireCandidate?.let { add("\"$it\" was under 20% all season. It's asking you to either shrink it, pause it, or retire it honestly.") }
        if (overallPercent >= 70) add("A 70%+ season is rare and meaningful. You showed up when it counted.")
        else if (overallPercent < 40) add("Under 40% for a full season is a clear signal. The problem is rarely motivation — it's usually design.")
    }

    fun quarterlyReflectionPrompt(graduateCandidate: String?, retireCandidate: String?): String = when {
        graduateCandidate != null && retireCandidate != null ->
            "Graduate \"$graduateCandidate\", retire \"$retireCandidate\" — then ask: what does the person I'm becoming do daily next season?"
        graduateCandidate != null ->
            "\"$graduateCandidate\" is ready to be internalized. What new habit would the identity you're building add next?"
        retireCandidate != null ->
            "If you retired \"$retireCandidate\" honestly — without guilt — what would that free up for a habit that truly fits?"
        else ->
            "If you could only carry 3 habits into next season, which would they be — and why those 3?"
    }

    // ── Yearly ────────────────────────────────────────────────────────────────

    fun yearlyInsights(
        consistency: Int,
        totalCompletions: Int,
        thrivingCount: Int,
        graduatedCount: Int,
        activeHabitCount: Int
    ): List<String> = buildList {
        add("Full year: $consistency% consistency — $totalCompletions total habit completions.")
        if (thrivingCount > 0) add("$thrivingCount habit${pl(thrivingCount)} thriving. These are no longer goals — they are who you are.")
        if (graduatedCount > 0) add("$graduatedCount habit${pl(graduatedCount)} graduated this year — internalized beyond the need for tracking.")
        when {
            consistency >= 70 -> add("Maintaining 70%+ across a full year is rare. You built real compound interest in yourself.")
            consistency >= 50 -> add("More than half your days were lived in alignment with your intentions. That is a life well-directed.")
            else              -> add("This year was honest data. The gap between intention and action is a design problem — and design can be fixed.")
        }
        if (activeHabitCount >= 10 && consistency < 60) {
            add("$activeHabitCount habits tracked this year. The most consistent people usually hold 3–5 deeply, not 10+ loosely.")
        }
    }

    fun yearlyReflectionPrompt(consistency: Int, mostConsistentHabit: String?): String = when {
        consistency >= 70 -> "What did this year's most consistent version of you do differently? How do you make that person the default?"
        mostConsistentHabit != null ->
            "\"$mostConsistentHabit\" was your most consistent habit this year. What does that habit say about who you actually are?"
        else ->
            "One year from now, what would make you say this was the year everything shifted? What habit is the seed for that?"
    }

    // ── Growth Path (5-year / 10-year) ────────────────────────────────────────

    fun fiveYearInsight(
        projections: List<GrowthProjection>,
        identityStatements: List<String>
    ): List<String> = buildList {
        if (projections.isNotEmpty()) {
            val top = projections.maxByOrNull { it.fiveYearSessions }!!
            add("At your current pace, 5 years of \"${top.habitName}\" = ~${top.fiveYearSessions} sessions. That's not a habit — it's a craft.")
        }
        identityStatements.firstOrNull()?.let {
            add("The identity you're building: \"$it\" — sustained for 5 years becomes an unchangeable part of who you are.")
        }
        add("5 years of 1% daily improvement = 37x the person you are today. The math is on your side. Show up.")
    }

    fun fiveYearReflectionPrompt(hasConsistentHabits: Boolean): String =
        if (hasConsistentHabits)
            "The habits you're building today are the person you'll be in 2031. What does that person's daily life look like?"
        else
            "Who do you want to be in 5 years? Write down 3 habits that person does every single day — then start one of them today."

    fun tenYearReflectionPrompt(): String =
        "In 10 years, what would you most regret not having built consistently? What habit is the seed for that change — and what's stopping you from starting it today?"

    // ── Decisions ─────────────────────────────────────────────────────────────

    fun decisionsInsight(
        retireCandidates: List<String>,
        graduateCandidates: List<String>,
        overCommitted: Boolean
    ): List<String> = buildList {
        if (graduateCandidates.isNotEmpty()) {
            add("Graduation candidates: ${graduateCandidates.joinToString()}. These habits have become part of you.")
        }
        if (retireCandidates.isNotEmpty()) {
            add("Retirement candidates: ${retireCandidates.joinToString()}. Not failure — honest evolution.")
        }
        if (overCommitted) {
            add("You're tracking more habits than you can consistently do. Cutting is not giving up — it's strategy.")
        }
        if (graduateCandidates.isEmpty() && retireCandidates.isEmpty() && !overCommitted) {
            add("Your habit system looks well-calibrated. Keep showing up — compounding takes time.")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun pl(count: Int) = if (count == 1) "" else "s"
}

data class GrowthProjection(
    val habitName: String,
    val completionPercent: Int,
    val scheduledDaysPerYear: Int,
    val fiveYearSessions: Int,
    val tenYearSessions: Int
)
