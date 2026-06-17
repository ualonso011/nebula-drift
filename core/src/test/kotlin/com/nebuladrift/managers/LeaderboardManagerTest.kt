package com.nebuladrift.managers

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.nebuladrift.util.LeaderboardEntry
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [LeaderboardManager] singleton.
 *
 * Covers:
 * - Entry insertion and retrieval
 * - Sorting by score descending (ties broken by time ascending)
 * - Maximum 10 entries limit
 * - [isHighScore] logic when board is full / not full
 * - JSON round-trip preservation
 * - Empty leaderboard and clear behaviour
 */
class LeaderboardManagerTest {

    companion object {
        @BeforeAll @JvmStatic
        fun initGdx() {
            val config = HeadlessApplicationConfiguration()
            HeadlessApplication(object : ApplicationAdapter() {}, config)
        }
    }

    @BeforeEach
    fun setUp() {
        LeaderboardManager.clear()
    }

    @Test
    fun `addEntry inserts entry correctly`() {
        val entry = LeaderboardEntry("Pilot", 1000, 60f, "2026-06-17")
        LeaderboardManager.addEntry(entry)

        val entries = LeaderboardManager.getEntries()
        assert(entries.size == 1)
        assert(entries[0].name == "Pilot")
        assert(entries[0].score == 1000)
    }

    @Test
    fun `entries are sorted by score descending`() {
        LeaderboardManager.addEntry(LeaderboardEntry("Pilot1", 500, 60f, "2026-06-17"))
        LeaderboardManager.addEntry(LeaderboardEntry("Pilot2", 1000, 60f, "2026-06-17"))
        LeaderboardManager.addEntry(LeaderboardEntry("Pilot3", 750, 60f, "2026-06-17"))

        val entries = LeaderboardManager.getEntries()
        assert(entries[0].score == 1000)
        assert(entries[1].score == 750)
        assert(entries[2].score == 500)
    }

    @Test
    fun `ties are broken by time ascending`() {
        LeaderboardManager.addEntry(LeaderboardEntry("Pilot1", 1000, 90f, "2026-06-17"))
        LeaderboardManager.addEntry(LeaderboardEntry("Pilot2", 1000, 60f, "2026-06-17"))

        val entries = LeaderboardManager.getEntries()
        assert(entries[0].name == "Pilot2") // faster time
        assert(entries[1].name == "Pilot1")
    }

    @Test
    fun `max 10 entries are kept`() {
        for (i in 1..15) {
            LeaderboardManager.addEntry(LeaderboardEntry("Pilot$i", i * 100, 60f, "2026-06-17"))
        }

        val entries = LeaderboardManager.getEntries()
        assert(entries.size == 10)
        assert(entries[0].score == 1500) // highest
        assert(entries[9].score == 600) // 10th highest
    }

    @Test
    fun `isHighScore returns true when less than 10 entries`() {
        for (i in 1..5) {
            LeaderboardManager.addEntry(LeaderboardEntry("Pilot$i", 1000, 60f, "2026-06-17"))
        }

        assert(LeaderboardManager.isHighScore(500)) // any score qualifies
    }

    @Test
    fun `isHighScore returns true when score beats lowest`() {
        for (i in 1..10) {
            LeaderboardManager.addEntry(LeaderboardEntry("Pilot$i", 1000, 60f, "2026-06-17"))
        }

        assert(LeaderboardManager.isHighScore(1500)) // beats lowest (1000)
        assert(!LeaderboardManager.isHighScore(500)) // doesn't beat lowest
    }

    @Test
    fun `JSON round-trip preserves data`() {
        val original = LeaderboardEntry("Nova", 2500, 120.5f, "2026-06-17")
        LeaderboardManager.addEntry(original)

        val loaded = LeaderboardManager.getEntries()[0]
        assert(loaded.name == original.name)
        assert(loaded.score == original.score)
        assert(loaded.time == original.time)
        assert(loaded.date == original.date)
    }

    @Test
    fun `empty leaderboard returns empty list`() {
        val entries = LeaderboardManager.getEntries()
        assert(entries.isEmpty())
    }

    @Test
    fun `clear removes all entries`() {
        LeaderboardManager.addEntry(LeaderboardEntry("Pilot", 1000, 60f, "2026-06-17"))
        LeaderboardManager.clear()

        val entries = LeaderboardManager.getEntries()
        assert(entries.isEmpty())
    }
}
