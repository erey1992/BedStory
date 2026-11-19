package com.mamaoca.cuentos

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RewardsTest {
    private fun r(id: String, kind: RewardKind) = Reward(id, kind, id, if (kind == RewardKind.Sticker) null else "x.webp",
        if (kind == RewardKind.Sticker) "<svg/>" else null, false)
    private val catalog = RewardCatalog(
        (1..5).map { r("s$it", RewardKind.Sticker) } + r("b1", RewardKind.Background) + r("c1", RewardKind.Coloring) + r("p1", RewardKind.Puzzle))

    @Test fun stickersComeInPacksOfThreeAndNeverRepeat() {
        val got = RewardRules.draw(catalog, setOf("s1"), Random(1), RewardKind.Sticker)
        assertEquals(3, got.size)
        assertTrue(got.all { it.kind == RewardKind.Sticker && it.id != "s1" })
        assertEquals(got.size, got.map { it.id }.toSet().size)
    }

    @Test fun otherKindsComeOneAtATime() {
        assertEquals(listOf("p1"), RewardRules.draw(catalog, emptySet(), Random(2), RewardKind.Puzzle).map { it.id })
    }

    @Test fun aKindAlreadyCompleteFallsBackToAnotherAndAllOwnedGivesNothing() {
        val got = RewardRules.draw(catalog, setOf("p1"), Random(3), RewardKind.Puzzle)
        assertTrue(got.isNotEmpty() && got.none { it.id == "p1" })
        assertTrue(RewardRules.draw(catalog, catalog.all.map { it.id }.toSet(), Random(4)).isEmpty())
    }

    @Test fun moreStarsMeanMoreChanceOfASurprise() {
        val chances = (0..3).map { RewardRules.surpriseChance(it, firstTime = true) }
        assertEquals(chances.sorted(), chances)
        assertTrue(RewardRules.surpriseChance(3, firstTime = false) < chances[0])
    }

    @Test fun aStoryGivesItsOwnStickerAndPrefersItsOwnPrizes() {
        val story = RewardCatalog(catalog.all + Reward("st-a", RewardKind.Sticker, "A", "a.webp", null, false, "a") +
            Reward("c-a", RewardKind.Coloring, "A", "c.webp", null, false, "a") + Reward("p-a", RewardKind.Puzzle, "A", "p.webp", null, false, "a"))
        assertEquals(listOf("st-a"), RewardRules.storyStickers(story, emptySet(), "a").map { it.id })
        assertTrue(RewardRules.storyStickers(story, setOf("st-a"), "a").isEmpty())
        // un cuento puede dar varias: su personaje y un objeto o animal
        val two = RewardCatalog(story.all + Reward("st-obj-gallina", RewardKind.Sticker, "A", "g.webp", null, false, "a"))
        assertEquals(setOf("st-a", "st-obj-gallina"), RewardRules.storyStickers(two, emptySet(), "a").map { it.id }.toSet())
        assertEquals("p-a", RewardRules.storyPuzzle(story, emptySet(), "a")?.id)
        assertNull(RewardRules.storyPuzzle(story, setOf("p-a"), "a"))
        assertNull(RewardRules.storyPuzzle(story, emptySet(), "b"))
        assertTrue(RewardRules.drawFor(story, emptySet(), Random(5), "a").single().id in setOf("c-a", "p-a"))
        // con lo del cuento ya ganado, la sorpresa es cualquier otra cosa
        assertTrue(RewardRules.drawFor(story, setOf("c-a", "p-a"), Random(6), "a").none { it.story == "a" })
        // y la calcomanía de un cuento nunca sale al azar
        repeat(20) { assertTrue(RewardRules.draw(story, emptySet(), Random(it), RewardKind.Sticker).none { r -> r.id == "st-a" }) }
    }

    @Test fun puzzleTimesReadWell() {
        assertEquals("1 segundo", formatTime(400))
        assertEquals("45 segundos", formatTime(45_000))
        assertEquals("2:05 minutos", formatTime(125_000))
    }

    @Test fun everyPrizeBelongsToARealAchievement() {
        val ids = ACHIEVEMENTS.map { it.id }.toSet()
        assertTrue(ACHIEVEMENT_PRIZES.keys.all { it in ids })
    }
}
