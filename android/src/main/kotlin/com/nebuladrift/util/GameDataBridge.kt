package com.nebuladrift.util

import android.content.Context
import com.badlogic.gdx.utils.Json
import com.nebuladrift.util.LeaderboardEntry

/**
 * Bridges SharedPreferences between Android-native Compose menus and
 * libGDX's [com.badlogic.gdx.Preferences] (they share the same backing
 * file `"nebula-drift"`).
 *
 * Use this from Compose screens to read/write high scores, leaderboard
 * entries, and volume settings without needing a running libGDX instance.
 */
class GameDataBridge(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json()

    // ── High score ──────────────────────────────────────────────

    fun getHighScore(): Int = prefs.getInt(KEY_HIGH_SCORE, 0)

    fun setHighScore(score: Int) {
        prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
    }

    // ── Leaderboard ────────────────────────────────────────────

    fun getLeaderboardEntries(): List<LeaderboardEntry> {
        val raw = prefs.getString(KEY_LEADERBOARD, "[]") ?: "[]"
        return try {
            @Suppress("UNCHECKED_CAST")
            val arr = json.fromJson(Array<LeaderboardEntry>::class.java, raw)
            arr.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addLeaderboardEntry(entry: LeaderboardEntry) {
        val entries = getLeaderboardEntries().toMutableList()
        entries.add(entry)
        entries.sortWith(compareByDescending<LeaderboardEntry> { it.score }.thenBy { it.time })
        val top10 = entries.take(MAX_ENTRIES)
        prefs.edit().putString(KEY_LEADERBOARD, json.toJson(top10)).apply()
    }

    fun isHighScore(score: Int): Boolean {
        val entries = getLeaderboardEntries()
        if (entries.size < MAX_ENTRIES) return true
        return score > entries.last().score
    }

    // ── Volume settings ─────────────────────────────────────────

    fun getMusicVolume(): Float = prefs.getFloat(KEY_MUSIC_VOLUME, 0.5f)
    fun setMusicVolume(v: Float) = prefs.edit().putFloat(KEY_MUSIC_VOLUME, v).apply()

    fun getSfxVolume(): Float = prefs.getFloat(KEY_SFX_VOLUME, 0.7f)
    fun setSfxVolume(v: Float) = prefs.edit().putFloat(KEY_SFX_VOLUME, v).apply()

    // ── Locale ──────────────────────────────────────────────────

    fun getLocale(): String = prefs.getString(KEY_LOCALE, "en") ?: "en"
    fun setLocale(locale: String) = prefs.edit().putString(KEY_LOCALE, locale).apply()

    companion object {
        private const val PREFS_NAME = "nebula-drift"
        private const val KEY_HIGH_SCORE = "highScore"
        private const val KEY_LEADERBOARD = "leaderboard"
        private const val KEY_MUSIC_VOLUME = "musicVolume"
        private const val KEY_SFX_VOLUME = "sfxVolume"
        private const val KEY_LOCALE = "locale"
        private const val MAX_ENTRIES = 10
    }
}
