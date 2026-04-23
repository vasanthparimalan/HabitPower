package com.example.habitpower.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.habitpower.data.dao.DailyHealthStatDao
import com.example.habitpower.data.dao.ExerciseDao
import com.example.habitpower.data.dao.HabitTrackingDao
import com.example.habitpower.data.dao.LifeAreaDao
import com.example.habitpower.data.dao.PomodoroSessionDao
import com.example.habitpower.data.dao.QuoteDao
import com.example.habitpower.data.dao.RoutineDao
import com.example.habitpower.data.dao.RoutineNotificationSettingsDao
import com.example.habitpower.data.dao.UserDao
import com.example.habitpower.data.dao.UserStatsDao
import com.example.habitpower.data.dao.WorkoutSessionDao
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.DailyHealthStat
import com.example.habitpower.data.model.PomodoroSession
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.Quote
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.RoutineExerciseCrossRef
import com.example.habitpower.data.model.RoutineNotificationSettings
import com.example.habitpower.data.model.UserHabitAssignment
import com.example.habitpower.data.model.UserLifeAreaAssignment
import com.example.habitpower.data.model.UserProfile
import com.example.habitpower.data.model.UserStats
import com.example.habitpower.data.model.WorkoutSession

/**
 * Room database for the HabitPower app.
 *
 * Keeps entities and migrations in one place. `exportSchema = true`
 * helps generate Room schema files for review and migration testing.
 */
@Database(
    entities = [
        Exercise::class,
        Routine::class,
        RoutineExerciseCrossRef::class,
        WorkoutSession::class,
        DailyHealthStat::class,
        UserProfile::class,
        HabitDefinition::class,
        LifeArea::class,
        UserHabitAssignment::class,
        UserLifeAreaAssignment::class,
        DailyHabitEntry::class,
        Quote::class,
        UserStats::class,
        RoutineNotificationSettings::class,
        PomodoroSession::class
    ],
    version = 25,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HabitPowerDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun dailyHealthStatDao(): DailyHealthStatDao
    abstract fun userDao(): UserDao
    abstract fun habitTrackingDao(): HabitTrackingDao
    abstract fun lifeAreaDao(): LifeAreaDao
    abstract fun quoteDao(): QuoteDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun routineNotificationSettingsDao(): RoutineNotificationSettingsDao
    abstract fun pomodoroSessionDao(): PomodoroSessionDao

    companion object {
        @Volatile
        private var INSTANCE: HabitPowerDatabase? = null

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN operator TEXT NOT NULL DEFAULT 'GREATER_THAN_OR_EQUAL'")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `quotes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `text` TEXT NOT NULL)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new table for life areas
                db.execSQL("CREATE TABLE IF NOT EXISTS `life_areas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `displayOrder` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)")
                // Add nullable foreign key column to habits
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN lifeAreaId INTEGER")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN goalIdentityStatement TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN commitmentTime TEXT")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN commitmentLocation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN preReminderMinutes INTEGER")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT 'DAILY'")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN recurrenceDaysOfWeekMask INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN recurrenceDayOfMonth INTEGER")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN recurrenceWeekOfMonth INTEGER")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN recurrenceWeekday INTEGER")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN recurrenceYearlyDates TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN recurrenceAnchorDate INTEGER")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN recurrenceStartDate INTEGER")
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN recurrenceEndDate INTEGER")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `user_stats` (
                        `userId` INTEGER NOT NULL PRIMARY KEY,
                        `currentStreak` INTEGER NOT NULL DEFAULT 0,
                        `longestStreak` INTEGER NOT NULL DEFAULT 0,
                        `totalXp` INTEGER NOT NULL DEFAULT 0,
                        `level` INTEGER NOT NULL DEFAULT 1,
                        `totalHabitsCompleted` INTEGER NOT NULL DEFAULT 0,
                        `totalDaysPerfect` INTEGER NOT NULL DEFAULT 0,
                        `lastPerfectDate` INTEGER,
                        `earnedBadgesMask` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `user_life_area_assignments` (
                        `userId` INTEGER NOT NULL,
                        `lifeAreaId` INTEGER NOT NULL,
                        `displayOrder` INTEGER NOT NULL DEFAULT 0,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`userId`, `lifeAreaId`),
                        FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`lifeAreaId`) REFERENCES `life_areas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_life_area_assignments_lifeAreaId` ON `user_life_area_assignments` (`lifeAreaId`)")

                // Backfill from existing habit assignments so current analytics keep behavior after upgrade.
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO user_life_area_assignments(userId, lifeAreaId, displayOrder, isActive)
                    SELECT DISTINCT ua.userId, hd.lifeAreaId, 0, 1
                    FROM user_habit_assignments ua
                    INNER JOIN habit_definitions hd ON hd.id = ua.habitId
                    WHERE ua.isActive = 1
                        AND hd.isActive = 1
                        AND hd.lifeAreaId IS NOT NULL
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_stats ADD COLUMN lastCheckInDate INTEGER")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN lastCheckInCompletedCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN lastCheckInWasPerfect INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN lastCheckInXpAwarded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN lastCheckInStreakBefore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN lastCheckInLongestBefore INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to exercises table
                db.execSQL("ALTER TABLE exercises ADD COLUMN instructions TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN tags TEXT NOT NULL DEFAULT ''")

                // Add new columns to routines table
                db.execSQL("ALTER TABLE routines ADD COLUMN type TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE routines ADD COLUMN restTimeSeconds INTEGER NOT NULL DEFAULT 0")

                // Add routine reference to habits table
                db.execSQL("ALTER TABLE habit_definitions ADD COLUMN routineId INTEGER")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create routine notification settings table
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `routine_notification_settings` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `enableExerciseStartSound` INTEGER NOT NULL DEFAULT 1,
                        `enableExerciseEndSound` INTEGER NOT NULL DEFAULT 1,
                        `exerciseStartSoundUri` TEXT,
                        `exerciseEndSoundUri` TEXT
                    )"""
                )
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ROUTINE habits were incorrectly created with showInDailyCheckIn = 0.
                // They should appear in daily check-in so users can confirm completion manually.
                db.execSQL("UPDATE habit_definitions SET showInDailyCheckIn = 1 WHERE type = 'ROUTINE'")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add lifecycle status column. Default ACTIVE keeps all existing habits unchanged.
                // isActive remains the canonical "include in daily tracking" boolean and stays in
                // sync whenever lifecycleStatus changes (ACTIVE → isActive=1, others → isActive=0).
                db.execSQL(
                    "ALTER TABLE habit_definitions ADD COLUMN lifecycleStatus TEXT NOT NULL DEFAULT 'ACTIVE'"
                )
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE life_areas ADD COLUMN emoji TEXT")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `pomodoro_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        `linkedHabitId` INTEGER
                    )"""
                )
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN category TEXT NOT NULL DEFAULT 'STRENGTH'")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routines ADD COLUMN repeatCount INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN wgerExerciseId INTEGER")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quotes ADD COLUMN source TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotes ADD COLUMN sourceUrl TEXT NOT NULL DEFAULT ''")
                // Backfill attribution on existing default Atomic Habits quotes
                db.execSQL(
                    "UPDATE quotes SET source = 'Atomic Habits — James Clear', " +
                    "sourceUrl = 'https://www.audible.com/search?keywords=Atomic+Habits+James+Clear' " +
                    "WHERE text LIKE '%(Atomic Habits)%'"
                )
                db.execSQL(
                    "UPDATE quotes SET source = 'The Power of Habit — Charles Duhigg', " +
                    "sourceUrl = 'https://www.audible.com/search?keywords=The+Power+of+Habit+Charles+Duhigg' " +
                    "WHERE text LIKE '%(The Power of Habit)%'"
                )
                db.execSQL(
                    "UPDATE quotes SET source = 'Tiny Habits — BJ Fogg', " +
                    "sourceUrl = 'https://www.audible.com/search?keywords=Tiny+Habits+BJ+Fogg' " +
                    "WHERE text LIKE '%(Tiny Habits)%'"
                )
                db.execSQL(
                    "UPDATE quotes SET source = 'Behavior Design — BJ Fogg', " +
                    "sourceUrl = 'https://www.audible.com/search?keywords=Tiny+Habits+BJ+Fogg' " +
                    "WHERE text LIKE '%(Behavior Design)%'"
                )
            }
        }

        /**
         * Obtain the singleton database instance.
         *
         * Migrations are applied explicitly; avoid destructive migration to
         * preserve user data across upgrades.
         */
        fun getDatabase(context: Context): HabitPowerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    HabitPowerDatabase::class.java,
                    "habit_power_database"
                )
                    .addMigrations(
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23,
                        MIGRATION_23_24,
                        MIGRATION_24_25
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
