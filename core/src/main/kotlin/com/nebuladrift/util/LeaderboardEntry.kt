package com.nebuladrift.util

/**
 * A single entry in the leaderboard.
 *
 * @property name  Player-chosen pilot name (from the predefined list)
 * @property score Final score for that play-through
 * @property time  Elapsed play time in seconds
 * @property date  Human-readable date string (e.g. "Wed Jun 17 12:34:56 UTC 2026")
 */
data class LeaderboardEntry(
    var name: String = "",
    var score: Int = 0,
    var time: Float = 0f,
    var date: String = ""
)
