package com.nebuladrift.managers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.nebuladrift.util.LeaderboardEntry

/**
 * Singleton that persists the top 10 leaderboard entries to
 * libGDX [Preferences] using JSON serialisation.
 *
 * Stored under the `"leaderboard"` key inside the `"nebula-drift"` prefs file.
 *
 * Usage:
 * ```kotlin
 * LeaderboardManager.addEntry(LeaderboardEntry("Ace", 1500, 120.5f, "2026-06-17"))
 * val entries = LeaderboardManager.getEntries()
 * if (LeaderboardManager.isHighScore(1000)) { ... }
 * ```
 */
object LeaderboardManager {

    private const val PREFS_NAME = "nebula-drift"
    private const val PREFS_KEY = "leaderboard"
    private const val MAX_ENTRIES = 10

    /**
     * Return the current leaderboard, sorted by score descending
     * (ties broken by time ascending).  Never null — returns an
     * empty list on any deserialisation error.
     */
    fun getEntries(): List<LeaderboardEntry> {
        val prefs = Gdx.app.getPreferences(PREFS_NAME)
        val json = prefs.getString(PREFS_KEY, "[]")
        return try {
            val jsonParser = Json()
            jsonParser.fromJson(Array<LeaderboardEntry>::class.java, json).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Insert [entry] into the leaderboard, sort by score descending
     * (time ascending for ties), and keep only the top [MAX_ENTRIES].
     * Persists immediately.
     */
    fun addEntry(entry: LeaderboardEntry) {
        val entries = getEntries().toMutableList()
        entries.add(entry)
        entries.sortWith(compareByDescending<LeaderboardEntry> { it.score }.thenBy { it.time })
        val top10 = entries.take(MAX_ENTRIES)
        saveEntries(top10)
    }

    /**
     * Persist the given entries as a JSON array under [PREFS_KEY].
     */
    private fun saveEntries(entries: List<LeaderboardEntry>) {
        val prefs = Gdx.app.getPreferences(PREFS_NAME)
        val jsonParser = Json()
        val json = jsonParser.toJson(entries)
        prefs.putString(PREFS_KEY, json)
        prefs.flush()
    }

    /**
     * Return `true` if [score] would make it onto the leaderboard.
     *
     * The leaderboard is open (always a high score) when there are
     * fewer than [MAX_ENTRIES] entries. Otherwise the score must
     * be strictly greater than the current lowest score.
     */
    fun isHighScore(score: Int): Boolean {
        val entries = getEntries()
        if (entries.size < MAX_ENTRIES) return true
        return score > entries.last().score
    }

    /**
     * Clear all entries (useful for testing or a full reset).
     */
    fun clear() {
        val prefs = Gdx.app.getPreferences(PREFS_NAME)
        prefs.putString(PREFS_KEY, "[]")
        prefs.flush()
    }
}
