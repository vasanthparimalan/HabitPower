package com.example.habitpower.data

import com.example.habitpower.data.model.Checklist
import com.example.habitpower.data.model.ChecklistItem
import com.example.habitpower.data.model.ChantDefinition
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.DailyHealthStat
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.Quote
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.RoutineExerciseCrossRef
import com.example.habitpower.data.model.RoutineType
import com.example.habitpower.data.model.TargetOperator
import com.example.habitpower.data.model.Task
import com.example.habitpower.data.model.TaskList
import com.example.habitpower.data.model.UserHabitAssignment
import com.example.habitpower.data.model.UserLifeAreaAssignment
import com.example.habitpower.data.model.UserProfile
import com.example.habitpower.data.model.UserStats
import com.example.habitpower.gamification.GamificationRepository
import com.example.habitpower.util.CrashLogger
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class HpexSection(val key: String, val displayName: String) {
    USERS("users", "Users & Profiles"),
    LIFE_AREAS("lifeAreas", "Life Areas"),
    HABITS("habits", "Habits & Tracking History"),
    HEALTH("health", "Health Stats (sleep, steps)"),
    GAMIFICATION("gamification", "Streaks & XP"),
    CHANTS("chants", "Chants"),
    ROUTINES("routines", "Routines & Exercises"),
    TASKS("tasks", "Tasks & Checklists"),
    QUOTES("quotes", "Custom Quotes")
}

data class HpexImportResult(
    val users: Int = 0,
    val lifeAreas: Int = 0,
    val habits: Int = 0,
    val entries: Int = 0,
    val healthStats: Int = 0,
    val chants: Int = 0,
    val routines: Int = 0,
    val tasks: Int = 0,
    val quotes: Int = 0
)

class HpexBackupManager(
    private val repository: HabitPowerRepository,
    private val gamificationRepository: GamificationRepository
) {

    // ── Export ────────────────────────────────────────────────────────────────

    suspend fun export(sections: Set<HpexSection>, appVersion: String): String {
        CrashLogger.log("hpex export started: sections=${sections.map { it.key }}")
        val root = JSONObject()
        root.put("hpexVersion", 1)
        root.put("exportedAt", LocalDate.now().toString())
        root.put("appVersion", appVersion)

        val sectionsArr = JSONArray()
        sections.forEach { sectionsArr.put(it.key) }
        root.put("sections", sectionsArr)

        val users = repository.getAllUsers().first()

        if (HpexSection.USERS in sections) {
            val arr = JSONArray()
            users.forEach { u ->
                arr.put(JSONObject().apply {
                    put("id", u.id)
                    put("name", u.name)
                    put("isActive", u.isActive)
                })
            }
            root.put("users", arr)
        }

        if (HpexSection.LIFE_AREAS in sections) {
            val lifeAreas = repository.getAllLifeAreas().first()
            val laArr = JSONArray()
            lifeAreas.forEach { la ->
                laArr.put(JSONObject().apply {
                    put("id", la.id)
                    put("name", la.name)
                    putJsonOpt("description", la.description)
                    put("displayOrder", la.displayOrder)
                    put("isActive", la.isActive)
                    putJsonOpt("emoji", la.emoji)
                })
            }
            root.put("lifeAreas", laArr)

            val assignments = repository.getAllLifeAreaAssignmentsForExport()
            val assArr = JSONArray()
            assignments.forEach { a ->
                assArr.put(JSONObject().apply {
                    put("userId", a.userId)
                    put("lifeAreaId", a.lifeAreaId)
                    put("displayOrder", a.displayOrder)
                    put("isActive", a.isActive)
                })
            }
            root.put("userLifeAreaAssignments", assArr)
        }

        if (HpexSection.HABITS in sections) {
            val habits = repository.getAllHabits().first()
            val hArr = JSONArray()
            habits.forEach { h ->
                hArr.put(JSONObject().apply {
                    put("id", h.id)
                    put("name", h.name)
                    put("goalIdentityStatement", h.goalIdentityStatement)
                    put("description", h.description)
                    putJsonOpt("commitmentTime", h.commitmentTime)
                    put("commitmentLocation", h.commitmentLocation)
                    putJsonOptInt("preReminderMinutes", h.preReminderMinutes)
                    put("recurrenceType", h.recurrenceType.name)
                    put("recurrenceInterval", h.recurrenceInterval)
                    put("recurrenceDaysOfWeekMask", h.recurrenceDaysOfWeekMask)
                    putJsonOptInt("recurrenceDayOfMonth", h.recurrenceDayOfMonth)
                    putJsonOptInt("recurrenceWeekOfMonth", h.recurrenceWeekOfMonth)
                    putJsonOptInt("recurrenceWeekday", h.recurrenceWeekday)
                    put("recurrenceYearlyDates", h.recurrenceYearlyDates)
                    putJsonOptDate("recurrenceAnchorDate", h.recurrenceAnchorDate)
                    putJsonOptDate("recurrenceStartDate", h.recurrenceStartDate)
                    putJsonOptDate("recurrenceEndDate", h.recurrenceEndDate)
                    put("type", h.type.name)
                    putJsonOpt("unit", h.unit)
                    putJsonOptDouble("targetValue", h.targetValue)
                    put("showInWidget", h.showInWidget)
                    put("showInDailyCheckIn", h.showInDailyCheckIn)
                    put("displayOrder", h.displayOrder)
                    put("isActive", h.isActive)
                    put("operator", h.operator.name)
                    putJsonOptLong("lifeAreaId", h.lifeAreaId)
                    putJsonOptLong("routineId", h.routineId)
                    put("lifecycleStatus", h.lifecycleStatus.name)
                })
            }
            root.put("habits", hArr)

            val assignments = repository.getAllHabitAssignmentsForExport()
            val assArr = JSONArray()
            assignments.forEach { a ->
                assArr.put(JSONObject().apply {
                    put("userId", a.userId)
                    put("habitId", a.habitId)
                    put("displayOrder", a.displayOrder)
                    put("isActive", a.isActive)
                })
            }
            root.put("userHabitAssignments", assArr)

            val entriesArr = JSONArray()
            users.forEach { u ->
                repository.getAllEntriesForUser(u.id).forEach { e ->
                    entriesArr.put(JSONObject().apply {
                        put("userId", e.userId)
                        put("habitId", e.habitId)
                        put("date", e.date.toString())
                        e.booleanValue?.let { put("booleanValue", it) }
                        e.numericValue?.let { put("numericValue", it) }
                        e.textValue?.let { put("textValue", it) }
                        e.quality?.let { put("quality", it) }
                    })
                }
            }
            root.put("habitEntries", entriesArr)
        }

        if (HpexSection.HEALTH in sections) {
            val stats = repository.getAllHealthStats().first()
            val arr = JSONArray()
            stats.sortedByDescending { it.date }.forEach { s ->
                arr.put(JSONObject().apply {
                    put("date", s.date.toString())
                    put("sleepHours", s.sleepHours)
                    put("stepsCount", s.stepsCount)
                    put("meditationCompleted", s.meditationCompleted)
                })
            }
            root.put("healthStats", arr)
        }

        if (HpexSection.GAMIFICATION in sections) {
            val arr = JSONArray()
            users.forEach { u ->
                val stats = gamificationRepository.getStats(u.id)
                arr.put(JSONObject().apply {
                    put("userId", u.id)
                    put("currentStreak", stats.currentStreak)
                    put("longestStreak", stats.longestStreak)
                    put("totalXp", stats.totalXp)
                    put("level", stats.level)
                    put("totalHabitsCompleted", stats.totalHabitsCompleted)
                    put("totalDaysPerfect", stats.totalDaysPerfect)
                    putJsonOptDate("lastPerfectDate", stats.lastPerfectDate)
                    putJsonOptDate("lastCheckInDate", stats.lastCheckInDate)
                    put("lastCheckInCompletedCount", stats.lastCheckInCompletedCount)
                    put("lastCheckInWasPerfect", stats.lastCheckInWasPerfect)
                    put("lastCheckInXpAwarded", stats.lastCheckInXpAwarded)
                    put("lastCheckInStreakBefore", stats.lastCheckInStreakBefore)
                    put("lastCheckInLongestBefore", stats.lastCheckInLongestBefore)
                    put("earnedBadgesMask", stats.earnedBadgesMask)
                    put("practiceDepth", stats.practiceDepth)
                })
            }
            root.put("userStats", arr)
        }

        if (HpexSection.CHANTS in sections) {
            val chants = repository.getAllChantsForExport()
            val arr = JSONArray()
            chants.forEach { c ->
                arr.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("text", c.text)
                    putJsonOpt("tradition", c.tradition)
                    put("defaultCount", c.defaultCount)
                    put("isBuiltIn", c.isBuiltIn)
                    putJsonOpt("audioUri", c.audioUri)
                })
            }
            root.put("chantDefinitions", arr)
        }

        if (HpexSection.ROUTINES in sections) {
            val routines = repository.getAllRoutines().first()
            val rArr = JSONArray()
            routines.forEach { r ->
                rArr.put(JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("description", r.description)
                    put("type", r.type.name)
                    put("restTimeSeconds", r.restTimeSeconds)
                    put("repeatCount", r.repeatCount)
                })
            }
            root.put("routines", rArr)

            val exercises = repository.getAllExercisesForExport()
            val eArr = JSONArray()
            exercises.forEach { e ->
                eArr.put(JSONObject().apply {
                    put("id", e.id)
                    put("name", e.name)
                    put("description", e.description)
                    putJsonOpt("imageUri", e.imageUri)
                    putJsonOpt("notes", e.notes)
                    putJsonOpt("instructions", e.instructions)
                    put("tags", e.tags)
                    put("category", e.category.name)
                    e.wgerExerciseId?.let { put("wgerExerciseId", it) }
                })
            }
            root.put("exercises", eArr)

            val crossRefs = repository.getAllRoutineCrossRefsForExport()
            val crArr = JSONArray()
            crossRefs.forEach { cr ->
                crArr.put(JSONObject().apply {
                    put("id", cr.id)
                    put("routineId", cr.routineId)
                    put("exerciseId", cr.exerciseId)
                    put("order", cr.order)
                    cr.sets?.let { put("sets", it) }
                    cr.reps?.let { put("reps", it) }
                    cr.durationSeconds?.let { put("durationSeconds", it) }
                })
            }
            root.put("routineExerciseCrossRefs", crArr)
        }

        if (HpexSection.TASKS in sections) {
            val taskLists = repository.getAllTaskListsForExport()
            val tlArr = JSONArray()
            taskLists.forEach { tl ->
                tlArr.put(JSONObject().apply {
                    put("id", tl.id)
                    put("userId", tl.userId)
                    put("name", tl.name)
                    put("createdAt", tl.createdAt)
                })
            }
            root.put("taskLists", tlArr)

            val tasks = repository.getAllTasksForExport()
            val tArr = JSONArray()
            tasks.forEach { t ->
                tArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("taskListId", t.taskListId)
                    put("name", t.name)
                    putJsonOpt("notes", t.notes)
                    t.dueDate?.let { put("dueDate", it) }
                    put("isDone", t.isDone)
                    t.completedAt?.let { put("completedAt", it) }
                })
            }
            root.put("tasks", tArr)

            val checklists = repository.getAllChecklistsForExport()
            val clArr = JSONArray()
            checklists.forEach { cl ->
                clArr.put(JSONObject().apply {
                    put("id", cl.id)
                    put("userId", cl.userId)
                    put("name", cl.name)
                    put("resetsDaily", cl.resetsDaily)
                })
            }
            root.put("checklists", clArr)

            val items = repository.getAllChecklistItemsForExport()
            val ciArr = JSONArray()
            items.forEach { ci ->
                ciArr.put(JSONObject().apply {
                    put("id", ci.id)
                    put("checklistId", ci.checklistId)
                    put("name", ci.name)
                    put("order", ci.order)
                    put("isChecked", ci.isChecked)
                    ci.lastCheckedAt?.let { put("lastCheckedAt", it) }
                })
            }
            root.put("checklistItems", ciArr)
        }

        if (HpexSection.QUOTES in sections) {
            val quotes = repository.getAllQuotesForExport()
            val arr = JSONArray()
            quotes.forEach { q ->
                arr.put(JSONObject().apply {
                    put("id", q.id)
                    put("text", q.text)
                    put("source", q.source)
                    put("sourceUrl", q.sourceUrl)
                })
            }
            root.put("quotes", arr)
        }

        return root.toString(2)
    }

    // ── Import ────────────────────────────────────────────────────────────────

    suspend fun import(json: String): HpexImportResult {
        CrashLogger.log("hpex import started: jsonLength=${json.length}")
        try {
        // Parse and validate BEFORE clearing anything — fail fast on bad file
        val root = JSONObject(json)
        if (root.optInt("hpexVersion", 0) < 1) {
            throw IllegalArgumentException("Not a valid HabitPower backup file (.hpex). Make sure you selected the correct file.")
        }

        // Clear all existing data then re-seed built-in chants immediately
        repository.clearForRestore()
        repository.seedChantsIfNeeded()

        var users = 0; var lifeAreas = 0; var habits = 0; var entries = 0
        var healthStats = 0; var chants = 0; var routines = 0; var tasks = 0; var quotes = 0
        var firstUserId: Long? = null

        // Users — insert first; all other tables reference userId
        root.optJSONArray("users")?.let { arr ->
            for (i in 0 until arr.length()) {
                val u = arr.getJSONObject(i)
                val userId = u.getLong("id")
                if (firstUserId == null) firstUserId = userId
                repository.insertUserDirect(UserProfile(
                    id = userId,
                    name = u.optString("name", "User ${i + 1}"),
                    isActive = u.optBoolean("isActive", false)
                ))
                users++
            }
        }

        // Life areas + assignments
        root.optJSONArray("lifeAreas")?.let { arr ->
            for (i in 0 until arr.length()) {
                val la = arr.getJSONObject(i)
                repository.insertLifeAreaDirect(LifeArea(
                    id = la.getLong("id"),
                    name = la.getString("name"),
                    description = la.optString("description").takeIf { it.isNotBlank() },
                    displayOrder = la.optInt("displayOrder", i),
                    isActive = la.optBoolean("isActive", true),
                    emoji = la.optString("emoji").takeIf { it.isNotBlank() }
                ))
                lifeAreas++
            }
        }
        root.optJSONArray("userLifeAreaAssignments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                repository.insertUserLifeAreaAssignmentDirect(UserLifeAreaAssignment(
                    userId = a.getLong("userId"),
                    lifeAreaId = a.getLong("lifeAreaId"),
                    displayOrder = a.optInt("displayOrder", 0),
                    isActive = a.optBoolean("isActive", true)
                ))
            }
        }

        // Habits + assignments
        root.optJSONArray("habits")?.let { arr ->
            for (i in 0 until arr.length()) {
                val h = arr.getJSONObject(i)
                val habitType = runCatching { HabitType.valueOf(h.getString("type")) }.getOrNull() ?: continue
                val recType = runCatching { HabitRecurrenceType.valueOf(h.optString("recurrenceType", "DAILY")) }.getOrDefault(HabitRecurrenceType.DAILY)
                val op = runCatching { TargetOperator.valueOf(h.optString("operator", "GREATER_THAN_OR_EQUAL")) }.getOrDefault(TargetOperator.GREATER_THAN_OR_EQUAL)
                val lifecycle = runCatching { HabitLifecycleStatus.valueOf(h.optString("lifecycleStatus", "ACTIVE")) }.getOrDefault(HabitLifecycleStatus.ACTIVE)
                repository.insertHabitDirect(HabitDefinition(
                    id = h.getLong("id"),
                    name = h.getString("name"),
                    goalIdentityStatement = h.optString("goalIdentityStatement", ""),
                    description = h.optString("description", ""),
                    commitmentTime = h.optString("commitmentTime").takeIf { it.isNotBlank() },
                    commitmentLocation = h.optString("commitmentLocation", ""),
                    preReminderMinutes = h.optNullableInt("preReminderMinutes"),
                    recurrenceType = recType,
                    recurrenceInterval = h.optInt("recurrenceInterval", 1),
                    recurrenceDaysOfWeekMask = h.optInt("recurrenceDaysOfWeekMask", 0),
                    recurrenceDayOfMonth = h.optNullableInt("recurrenceDayOfMonth"),
                    recurrenceWeekOfMonth = h.optNullableInt("recurrenceWeekOfMonth"),
                    recurrenceWeekday = h.optNullableInt("recurrenceWeekday"),
                    recurrenceYearlyDates = h.optString("recurrenceYearlyDates", ""),
                    recurrenceAnchorDate = h.optNullableDate("recurrenceAnchorDate"),
                    recurrenceStartDate = h.optNullableDate("recurrenceStartDate"),
                    recurrenceEndDate = h.optNullableDate("recurrenceEndDate"),
                    type = habitType,
                    unit = h.optString("unit").takeIf { it.isNotBlank() },
                    targetValue = h.optNullableDouble("targetValue"),
                    showInWidget = h.optBoolean("showInWidget", true),
                    showInDailyCheckIn = h.optBoolean("showInDailyCheckIn", true),
                    displayOrder = h.optInt("displayOrder", 0),
                    isActive = h.optBoolean("isActive", true),
                    operator = op,
                    lifeAreaId = h.optNullableLong("lifeAreaId"),
                    routineId = h.optNullableLong("routineId"),
                    lifecycleStatus = lifecycle
                ))
                habits++
            }
        }
        root.optJSONArray("userHabitAssignments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                repository.insertUserHabitAssignmentDirect(UserHabitAssignment(
                    userId = a.getLong("userId"),
                    habitId = a.getLong("habitId"),
                    displayOrder = a.optInt("displayOrder", 0),
                    isActive = a.optBoolean("isActive", true)
                ))
            }
        }

        // Habit entries (bulk for performance)
        val entriesToImport = mutableListOf<DailyHabitEntry>()
        root.optJSONArray("habitEntries")?.let { arr ->
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                val date = runCatching { LocalDate.parse(e.getString("date")) }.getOrNull() ?: continue
                entriesToImport.add(DailyHabitEntry(
                    userId = e.getLong("userId"),
                    habitId = e.getLong("habitId"),
                    date = date,
                    booleanValue = e.optNullableBoolean("booleanValue"),
                    numericValue = e.optNullableDouble("numericValue"),
                    textValue = if (e.has("textValue") && !e.isNull("textValue")) e.getString("textValue") else null,
                    quality = e.optNullableInt("quality")
                ))
                entries++
            }
        }
        if (entriesToImport.isNotEmpty()) repository.importBulkEntries(entriesToImport)

        // Health stats
        root.optJSONArray("healthStats")?.let { arr ->
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                val date = runCatching { LocalDate.parse(s.getString("date")) }.getOrNull() ?: continue
                repository.saveDailyStat(DailyHealthStat(
                    date = date,
                    sleepHours = s.optDouble("sleepHours", 0.0).toFloat(),
                    stepsCount = s.optInt("stepsCount", 0),
                    meditationCompleted = s.optBoolean("meditationCompleted", false)
                ))
                healthStats++
            }
        }

        // User stats / gamification
        root.optJSONArray("userStats")?.let { arr ->
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                gamificationRepository.upsertStats(UserStats(
                    userId = s.getLong("userId"),
                    currentStreak = s.optInt("currentStreak", 0),
                    longestStreak = s.optInt("longestStreak", 0),
                    totalXp = s.optInt("totalXp", 0),
                    level = s.optInt("level", 1),
                    totalHabitsCompleted = s.optInt("totalHabitsCompleted", 0),
                    totalDaysPerfect = s.optInt("totalDaysPerfect", 0),
                    lastPerfectDate = s.optNullableDate("lastPerfectDate"),
                    lastCheckInDate = s.optNullableDate("lastCheckInDate"),
                    lastCheckInCompletedCount = s.optInt("lastCheckInCompletedCount", 0),
                    lastCheckInWasPerfect = s.optBoolean("lastCheckInWasPerfect", false),
                    lastCheckInXpAwarded = s.optInt("lastCheckInXpAwarded", 0),
                    lastCheckInStreakBefore = s.optInt("lastCheckInStreakBefore", 0),
                    lastCheckInLongestBefore = s.optInt("lastCheckInLongestBefore", 0),
                    earnedBadgesMask = s.optLong("earnedBadgesMask", 0L),
                    practiceDepth = s.optDouble("practiceDepth", 0.0)
                ))
            }
        }

        // Chants — skip built-ins; they are seeded at startup
        root.optJSONArray("chantDefinitions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                if (c.optBoolean("isBuiltIn", false)) continue
                repository.insertChantDirect(ChantDefinition(
                    id = c.getLong("id"),
                    name = c.getString("name"),
                    text = c.getString("text"),
                    tradition = c.optString("tradition").takeIf { it.isNotBlank() },
                    defaultCount = c.optInt("defaultCount", 108),
                    isBuiltIn = false,
                    audioUri = c.optString("audioUri").takeIf { it.isNotBlank() }
                ))
                chants++
            }
        }

        // Routines → exercises → cross refs (insert in dependency order)
        root.optJSONArray("routines")?.let { arr ->
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                val rType = runCatching { RoutineType.valueOf(r.optString("type", "NORMAL")) }.getOrDefault(RoutineType.NORMAL)
                repository.insertRoutineDirect(Routine(
                    id = r.getLong("id"),
                    name = r.getString("name"),
                    description = r.optString("description", ""),
                    type = rType,
                    restTimeSeconds = r.optInt("restTimeSeconds", 0),
                    repeatCount = r.optInt("repeatCount", 1)
                ))
                routines++
            }
        }
        root.optJSONArray("exercises")?.let { arr ->
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                val cat = runCatching { ExerciseCategory.valueOf(e.optString("category", "STRENGTH")) }.getOrDefault(ExerciseCategory.STRENGTH)
                repository.insertExerciseDirect(Exercise(
                    id = e.getLong("id"),
                    name = e.getString("name"),
                    description = e.optString("description", ""),
                    imageUri = e.optString("imageUri").takeIf { it.isNotBlank() },
                    notes = e.optString("notes").takeIf { it.isNotBlank() },
                    instructions = e.optString("instructions").takeIf { it.isNotBlank() },
                    tags = e.optString("tags", ""),
                    category = cat,
                    wgerExerciseId = e.optNullableInt("wgerExerciseId")
                ))
            }
        }
        root.optJSONArray("routineExerciseCrossRefs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val cr = arr.getJSONObject(i)
                repository.insertRoutineCrossRefDirect(RoutineExerciseCrossRef(
                    id = cr.getLong("id"),
                    routineId = cr.getLong("routineId"),
                    exerciseId = cr.getLong("exerciseId"),
                    order = cr.optInt("order", i),
                    sets = cr.optNullableInt("sets"),
                    reps = cr.optNullableInt("reps"),
                    durationSeconds = cr.optNullableInt("durationSeconds")
                ))
            }
        }

        // Task lists → tasks → checklists → items
        root.optJSONArray("taskLists")?.let { arr ->
            for (i in 0 until arr.length()) {
                val tl = arr.getJSONObject(i)
                repository.insertTaskList(TaskList(
                    id = tl.getLong("id"),
                    userId = tl.getLong("userId"),
                    name = tl.getString("name"),
                    createdAt = tl.optLong("createdAt", System.currentTimeMillis())
                ))
                tasks++
            }
        }
        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                repository.insertTask(Task(
                    id = t.getLong("id"),
                    taskListId = t.getLong("taskListId"),
                    name = t.getString("name"),
                    notes = t.optString("notes").takeIf { it.isNotBlank() },
                    dueDate = if (t.has("dueDate") && !t.isNull("dueDate")) t.getLong("dueDate") else null,
                    isDone = t.optBoolean("isDone", false),
                    completedAt = if (t.has("completedAt") && !t.isNull("completedAt")) t.getLong("completedAt") else null
                ))
            }
        }
        root.optJSONArray("checklists")?.let { arr ->
            for (i in 0 until arr.length()) {
                val cl = arr.getJSONObject(i)
                repository.insertChecklist(Checklist(
                    id = cl.getLong("id"),
                    userId = cl.getLong("userId"),
                    name = cl.getString("name"),
                    resetsDaily = cl.optBoolean("resetsDaily", false)
                ))
                tasks++
            }
        }
        root.optJSONArray("checklistItems")?.let { arr ->
            for (i in 0 until arr.length()) {
                val ci = arr.getJSONObject(i)
                repository.insertChecklistItem(ChecklistItem(
                    id = ci.getLong("id"),
                    checklistId = ci.getLong("checklistId"),
                    name = ci.getString("name"),
                    order = ci.optInt("order", 0),
                    isChecked = ci.optBoolean("isChecked", false),
                    lastCheckedAt = if (ci.has("lastCheckedAt") && !ci.isNull("lastCheckedAt")) ci.getLong("lastCheckedAt") else null
                ))
            }
        }

        // Quotes
        root.optJSONArray("quotes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val q = arr.getJSONObject(i)
                repository.insertQuoteDirect(Quote(
                    id = q.getLong("id"),
                    text = q.getString("text"),
                    source = q.optString("source", ""),
                    sourceUrl = q.optString("sourceUrl", "")
                ))
                quotes++
            }
        }

        // Restore active user — this also triggers syncHabitReminders() + updateWidgetState()
        firstUserId?.let { repository.saveActiveUserId(it) }
        repository.triggerRefresh()
        val result = HpexImportResult(users, lifeAreas, habits, entries, healthStats, chants, routines, tasks, quotes)
        CrashLogger.log("hpex import complete: users=$users habits=$habits entries=$entries routines=$routines")
        return result
        } catch (e: Exception) {
            CrashLogger.recordException(e)
            throw e
        }
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private fun JSONObject.putJsonOpt(key: String, value: String?) {
        if (value != null) put(key, value) else put(key, JSONObject.NULL)
    }

    private fun JSONObject.putJsonOptInt(key: String, value: Int?) {
        if (value != null) put(key, value) else put(key, JSONObject.NULL)
    }

    private fun JSONObject.putJsonOptLong(key: String, value: Long?) {
        if (value != null) put(key, value) else put(key, JSONObject.NULL)
    }

    private fun JSONObject.putJsonOptDouble(key: String, value: Double?) {
        if (value != null) put(key, value) else put(key, JSONObject.NULL)
    }

    private fun JSONObject.putJsonOptDate(key: String, value: LocalDate?) {
        if (value != null) put(key, value.toString()) else put(key, JSONObject.NULL)
    }

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (has(key) && !isNull(key)) getInt(key) else null

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null

    private fun JSONObject.optNullableBoolean(key: String): Boolean? =
        if (has(key) && !isNull(key)) getBoolean(key) else null

    private fun JSONObject.optNullableDate(key: String): LocalDate? =
        optString(key).takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
