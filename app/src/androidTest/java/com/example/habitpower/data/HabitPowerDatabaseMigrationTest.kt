package com.example.habitpower.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class HabitPowerDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HabitPowerDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate11To12_backfillsRecurrenceDefaults() {
        val dbName = "habit-power-migration-test"

        helper.createDatabase(dbName, 11).apply {
            execSQL(
                """
                INSERT INTO habit_definitions (
                    id, name, goalIdentityStatement, description, commitmentTime,
                    commitmentLocation, preReminderMinutes, type, unit, targetValue,
                    showInWidget, showInDailyCheckIn, displayOrder, isActive, operator, lifeAreaId
                ) VALUES (
                    1, 'Read', 'be consistent', 'Read 20 pages', '07:30',
                    'Desk', 15, 'BOOLEAN', NULL, NULL,
                    1, 1, 0, 1, 'GREATER_THAN_OR_EQUAL', NULL
                )
                """.trimIndent()
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            12,
            true,
            HabitPowerDatabase.MIGRATION_11_12
        )

        migratedDb.query(
            "SELECT recurrenceType, recurrenceInterval, recurrenceDaysOfWeekMask, recurrenceYearlyDates, recurrenceAnchorDate, recurrenceStartDate, recurrenceEndDate FROM habit_definitions WHERE id = 1"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("DAILY", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(0, cursor.getInt(2))
            assertEquals("", cursor.getString(3))
            assertEquals(true, cursor.isNull(4))
            assertEquals(true, cursor.isNull(5))
            assertEquals(true, cursor.isNull(6))
        }

        migratedDb.close()
    }

    @Test
    fun migrate11To12_preservesExistingHabitCoreFields() {
        val dbName = "habit-power-migration-preserve-test"

        helper.createDatabase(dbName, 11).apply {
            execSQL(
                """
                INSERT INTO habit_definitions (
                    id, name, goalIdentityStatement, description, commitmentTime,
                    commitmentLocation, preReminderMinutes, type, unit, targetValue,
                    showInWidget, showInDailyCheckIn, displayOrder, isActive, operator, lifeAreaId
                ) VALUES (
                    9, 'Workout', 'be strong', 'Lift weights', '18:00',
                    'Gym', 30, 'NUMBER', 'reps', 12.0,
                    1, 0, 4, 1, 'GREATER_THAN_OR_EQUAL', 2
                )
                """.trimIndent()
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            12,
            true,
            HabitPowerDatabase.MIGRATION_11_12
        )

        migratedDb.query(
            "SELECT name, goalIdentityStatement, description, commitmentTime, commitmentLocation, preReminderMinutes, type, unit, targetValue, showInDailyCheckIn, displayOrder, operator, lifeAreaId FROM habit_definitions WHERE id = 9"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Workout", cursor.getString(0))
            assertEquals("be strong", cursor.getString(1))
            assertEquals("Lift weights", cursor.getString(2))
            assertEquals("18:00", cursor.getString(3))
            assertEquals("Gym", cursor.getString(4))
            assertEquals(30, cursor.getInt(5))
            assertEquals("NUMBER", cursor.getString(6))
            assertEquals("reps", cursor.getString(7))
            assertEquals(12.0, cursor.getDouble(8))
            assertEquals(0, cursor.getInt(9))
            assertEquals(4, cursor.getInt(10))
            assertEquals("GREATER_THAN_OR_EQUAL", cursor.getString(11))
            assertNotNull(cursor.getString(11))
            assertEquals(2, cursor.getLong(12))
        }

        migratedDb.close()
    }
}
