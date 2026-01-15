package com.example.checkinreceipts.sensors

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeLogicTest {

    @Test
    fun below_threshold_does_not_trigger() {
        val logic = ShakeLogic(threshold = 12f, cooldownMs = 1000L)
        val t0 = 1_000L
        val triggered = logic.onAccelerometer(3f, 5f, 1f, t0)
        assertFalse(triggered)
    }


    @Test
    fun above_threshold_outside_cooldown_triggers() {
        val logic = ShakeLogic(threshold = 12f, cooldownMs = 1000L)
        val t0 = 1_000L
        val triggered = logic.onAccelerometer(8f, 10f, 2f, t0)
        assertTrue(triggered)
    }


    @Test
    fun within_cooldown_does_not_trigger_again() {
        val logic = ShakeLogic(threshold = 12f, cooldownMs = 1000L)
        val t0 = 1_000L
        assertTrue(logic.onAccelerometer(8f, 10f, 2f, t0))

        val t1 = t0 + 500L
        assertFalse(logic.onAccelerometer(9f, 9f, 2f, t1))

        val t2 = t0 + 1000L
        assertTrue(logic.onAccelerometer(8f, 10f, 2f, t2))
    }


    @Test
    fun exactly_at_threshold_triggers() {
        val logic = ShakeLogic(threshold = 10f, cooldownMs = 500L)
        val t0 = 1_000L

        assertTrue(logic.onAccelerometer(6f, 8f, 0f, t0))
    }

    @Test
    fun just_below_threshold_does_not_trigger() {
        val logic = ShakeLogic(threshold = 10f, cooldownMs = 500L)
        val t0 = 1_000L

        assertFalse(logic.onAccelerometer(6f, 7f, 0f, t0))
    }

    @Test
    fun cooldown_boundary_behavior() {
        val logic = ShakeLogic(threshold = 12f, cooldownMs = 1000L)
        val t0 = 10_000L
        assertTrue(logic.onAccelerometer(8f, 10f, 2f, t0))

        val tAlmost = t0 + 999L
        assertFalse(logic.onAccelerometer(8f, 10f, 2f, tAlmost))

        val tEdge = t0 + 1000L
        assertTrue(logic.onAccelerometer(8f, 10f, 2f, tEdge))
    }


    @Test
    fun noisy_sequence_then_trigger() {
        val logic = ShakeLogic(threshold = 12f, cooldownMs = 800L)
        var t = 2_000L


        val noise = listOf(
            floatArrayOf(2f, 3f, 1f),
            floatArrayOf(1f, 4f, 2f),
            floatArrayOf(0.5f, 0.5f, 0.5f),
            floatArrayOf(5f, 5f, 1f)
        )
        for (n in noise) {
            assertFalse(logic.onAccelerometer(n[0], n[1], n[2], t))
            t += 50L
        }

        assertTrue(logic.onAccelerometer(9f, 9f, 3f, t))
    }
}
