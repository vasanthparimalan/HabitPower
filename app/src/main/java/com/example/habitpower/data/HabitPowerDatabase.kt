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
import com.example.habitpower.data.dao.QuoteDao
import com.example.habitpower.data.dao.RoutineDao
import com.example.habitpower.data.dao.UserDao
import com.example.habitpower.data.dao.WorkoutSessionDao
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.DailyHealthStat
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.Quote
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.RoutineExerciseCrossRef
import com.example.habitpower.data.model.UserHabitAssignment
import com.example.habitpower.data.model.UserProfile
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
        DailyHabitEntry::class,
        Quote::class
    ],
    version = 11,
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
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
