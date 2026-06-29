package com.example.habitpower.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule that spins up a fresh in-memory HabitPowerDatabase before each test
 * and closes it afterwards. No disk I/O, no network, works fully offline.
 *
 * Usage:
 *   @get:Rule val db = TestDatabaseRule()
 *   val dao = db.database.habitTrackingDao()
 */
class TestDatabaseRule : TestRule {

    lateinit var database: HabitPowerDatabase
        private set

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                database = Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    HabitPowerDatabase::class.java
                )
                    .allowMainThreadQueries()
                    .build()
                try {
                    base.evaluate()
                } finally {
                    database.close()
                }
            }
        }
}

/** Convenience: collect first emission from a Flow, blocking. */
fun <T> kotlinx.coroutines.flow.Flow<T>.firstBlocking(): T = runBlocking { first() }
