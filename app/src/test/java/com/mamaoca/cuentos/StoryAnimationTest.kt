package com.mamaoca.cuentos

import org.junit.Assert.*
import org.junit.Test

class StoryAnimationTest {
    private val item = PlacedSticker("st-x", 0.5f, 0.5f, 0.3f, uid = 1)

    @Test fun entrancesStartHiddenOrOffscreenAndEndInPlace() {
        assertEquals(0f, poseOf(Effect.FadeIn, 0f, item, Pose()).alpha, 0.001f)
        assertTrue(item.x + poseOf(Effect.FlyInLeft, 0f, item, Pose()).dx < 0f)
        assertTrue(item.x + poseOf(Effect.FlyInRight, 0f, item, Pose()).dx > 1f)
        Effect.entries.filter { it.group == EffectGroup.Entrance }.forEach { fx ->
            val end = poseOf(fx, 1f, item, Pose())
            assertEquals(fx.name, 0f, end.dx, 0.001f); assertEquals(fx.name, 0f, end.dy, 0.001f)
            assertEquals(fx.name, 1f, restAfter(fx, item, Pose(alpha = 0f)).alpha, 0.001f)
        }
    }

    @Test fun movesChangeWhereTheStickerRestsAndExitsHideIt() {
        val walked = restAfter(Effect.WalkRight, item, Pose())
        assertTrue(walked.dx > 0.1f)
        // el siguiente movimiento parte de donde quedó
        assertTrue(restAfter(Effect.WalkRight, item, walked).dx > walked.dx)
        assertEquals(0f, restAfter(Effect.Jump, item, Pose()).dy, 0.001f) // saltar vuelve al suelo
        Effect.entries.filter { it.group == EffectGroup.Exit }.forEach { fx ->
            assertEquals(fx.name, 0f, restAfter(fx, item, Pose()).alpha, 0.001f)
        }
    }

    @Test fun everySoundStepHasALabel() {
        STORY_SOUNDS.keys.forEach { assertFalse(Step(1, sound = "sfx:$it").label.startsWith("?")) }
        assertEquals("🎤 Mi voz", Step(1, sound = "rec:voz.m4a").label)
    }
}
