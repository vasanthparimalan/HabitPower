package com.example.habitpower.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habitpower.data.model.ChantDefinition
import com.example.habitpower.data.model.ChantSession
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChantTest {

    @get:Rule
    val db = TestDatabaseRule()

    private val repo get() = buildTestRepository(db.database)

    @Test
    fun seedChantsIfNeeded_populatesBuiltIns() = runBlocking {
        val beforeSeed = db.database.chantDao().getAllChants().first()
        assertTrue("DB must be empty before seeding", beforeSeed.isEmpty())

        repo.seedChantsIfNeeded()

        val afterSeed = db.database.chantDao().getAllChants().first()
        assertTrue("Built-in chants must be seeded", afterSeed.isNotEmpty())
        assertTrue(afterSeed.any { it.isBuiltIn })
    }

    @Test
    fun seedChantsIfNeeded_idempotent() = runBlocking {
        repo.seedChantsIfNeeded()
        val countAfterFirst = db.database.chantDao().getAllChants().first().size

        repo.seedChantsIfNeeded()
        val countAfterSecond = db.database.chantDao().getAllChants().first().size

        assertTrue("Seeding twice must not add duplicates", countAfterFirst == countAfterSecond)
    }

    @Test
    fun insertCustomChant_appearsInAllChants() = runBlocking {
        repo.seedChantsIfNeeded()
        db.database.chantDao().insertChant(
            ChantDefinition(name = "My Mantra", text = "Peace and calm", isBuiltIn = false)
        )

        val all = db.database.chantDao().getAllChants().first()
        assertTrue(all.any { it.name == "My Mantra" && !it.isBuiltIn })
    }

    @Test
    fun deleteCustomChant_removed() = runBlocking {
        val id = db.database.chantDao().insertChant(
            ChantDefinition(name = "ToDelete", text = "Text", isBuiltIn = false)
        )
        val chant = db.database.chantDao().getAllChants().first().first { it.id == id }
        db.database.chantDao().deleteChant(chant)

        val remaining = db.database.chantDao().getAllChants().first()
        assertFalse(remaining.any { it.id == id })
    }

    @Test
    fun insertChantSession_retrievedByUser() = runBlocking {
        val userId = db.database.userDao().insertUser(UserProfile(name = "Chanter"))
        val chantId = db.database.chantDao().insertChant(
            ChantDefinition(name = "Om", text = "ॐ", isBuiltIn = false)
        )
        db.database.chantDao().insertSession(
            ChantSession(userId = userId, chantId = chantId, targetCount = 108, actualCount = 108, durationSeconds = 300)
        )

        val sessions = db.database.chantDao().getRecentSessions(userId).first()
        assertTrue(sessions.isNotEmpty())
        assertTrue(sessions.any { it.userId == userId && it.chantId == chantId })
    }
}
