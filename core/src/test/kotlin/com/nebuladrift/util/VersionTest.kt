package com.nebuladrift.util

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for the [Constants.GAME_VERSION] constant.
 *
 * Covers:
 * - Constant exists and is non-empty
 * - Follows semantic versioning format (x.y.z)
 * - Matches the expected version string
 */
class VersionTest {

    @Test
    fun `GAME_VERSION constant exists and is non-empty`() {
        assertTrue(Constants.GAME_VERSION.isNotEmpty())
    }

    @Test
    fun `GAME_VERSION follows semantic versioning format`() {
        val versionRegex = Regex("^\\d+\\.\\d+\\.\\d+$")
        assertTrue(versionRegex.matches(Constants.GAME_VERSION))
    }

    @Test
    fun `GAME_VERSION is 0_4_0`() {
        assertTrue(Constants.GAME_VERSION == "0.4.0")
    }
}
