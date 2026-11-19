package com.mamaoca.cuentos

import org.junit.Assert.*
import org.junit.Test

class AchievementsTest {
    @Test fun twentyFiveUniqueAchievements() {
        assertEquals(25, ACHIEVEMENTS.size)
        assertEquals(25, ACHIEVEMENTS.map { it.id }.toSet().size)
    }

    @Test fun streakCountsConsecutiveDaysOnly() {
        assertEquals(0, longestStreak(emptySet()))
        assertEquals(3, longestStreak(setOf(10L, 11L, 12L, 20L)))
        assertEquals(4, longestStreak(setOf(1L, 3L, 4L, 5L, 6L, 9L)))
    }

    @Test fun thresholdsUseTheRightCounters() {
        fun reached(stats: ReadingStats) = ACHIEVEMENTS.filter { it.reached(stats) }.map { it.id }.toSet()
        assertTrue(reached(ReadingStats()).isEmpty())
        val stats = ReadingStats(finished = (1..5).map { "c$it" }.toSet(), tagCounts = mapOf("animales" to 5, "dragones" to 2),
            longestStreak = 5, maxRereads = 3)
        assertEquals(setOf("primer-cuento", "lector-curioso", "amigo-animales", "racha-3", "racha-5", "otra-vez"), reached(stats))
    }
}
