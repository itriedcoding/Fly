package com.fly.customizer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun detectsNewerPatch() {
        assertTrue(UpdateChecker.isNewer("1.0.0", "v1.0.1"))
        assertTrue(UpdateChecker.isNewer("1.0.0", "1.1.0"))
        assertTrue(UpdateChecker.isNewer("1.9.9", "2.0.0"))
    }

    @Test
    fun detectsOlderAndEqualVersions() {
        assertFalse(UpdateChecker.isNewer("1.1.0", "v1.1.0"))
        assertFalse(UpdateChecker.isNewer("1.2.0", "v1.1.9"))
        assertFalse(UpdateChecker.isNewer("2.0.0", "1.99.99"))
    }

    @Test
    fun handlesDebugSuffixAndShortVersions() {
        assertFalse(UpdateChecker.isNewer("1.1.0-debug", "v1.1.0"))
        assertTrue(UpdateChecker.isNewer("1.1.0-debug", "v1.2.0"))
        assertTrue(UpdateChecker.isNewer("1.0", "1.0.1"))
        assertFalse(UpdateChecker.isNewer("1.0.1", "1.0"))
    }

    @Test
    fun parsesVersionParts() {
        assertEquals(listOf(1, 1, 0), UpdateChecker.versionParts("v1.1.0"))
        assertEquals(listOf(2, 4), UpdateChecker.versionParts(" 2.4 "))
        assertEquals(emptyList<Int>(), UpdateChecker.versionParts("nightly"))
    }

    @Test
    fun ignoresMalformedLatestVersion() {
        assertFalse(UpdateChecker.isNewer("1.0.0", "latest"))
        assertFalse(UpdateChecker.isNewer("1.0.0", ""))
    }
}
