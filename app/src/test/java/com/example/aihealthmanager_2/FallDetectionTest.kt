package com.example.aihealthmanager_2

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.sqrt

/** 跌倒检测：先检测自由落体，再在 1 秒内检测撞击 */
class FallDetectionTest {

    companion object {
        const val FALL_THRESHOLD = 3.0
        const val IMPACT_THRESHOLD = 25.0
        const val IMPACT_WINDOW_MS = 1000L
    }

    private fun magnitude(x: Float, y: Float, z: Float): Double {
        return sqrt((x * x + y * y + z * z).toDouble())
    }

    data class FallState(var isFalling: Boolean = false, var fallTimestamp: Long = 0L)

    private fun processSensorData(state: FallState, mag: Double, timestamp: Long): Boolean {
        if (!state.isFalling && mag < FALL_THRESHOLD) {
            state.isFalling = true
            state.fallTimestamp = timestamp
            return false
        }

        if (state.isFalling && mag > IMPACT_THRESHOLD && (timestamp - state.fallTimestamp) < IMPACT_WINDOW_MS) {
            state.isFalling = false
            return true
        }

        if (state.isFalling && (timestamp - state.fallTimestamp) > IMPACT_WINDOW_MS) {
            state.isFalling = false
        }

        return false
    }

    @Test
    fun test_freefall_then_impact_triggersAlert() {
        val state = FallState()

        val freefallMag = magnitude(0.5f, 0.3f, 0.2f) // ≈0.62, < 3.0
        val triggered1 = processSensorData(state, freefallMag, 1000L)
        assertFalse(triggered1)
        assertTrue(state.isFalling)

        val impactMag = magnitude(18.0f, 15.0f, 12.0f) // ≈26.4, > 25.0
        val triggered2 = processSensorData(state, impactMag, 1500L) // 500ms内
        assertTrue(triggered2)
    }

    @Test
    fun test_impactOnly_withoutFreefall_noAlert() {
        val state = FallState()

        val impactMag = magnitude(18.0f, 15.0f, 12.0f) // ≈26.4, > 25.0
        val triggered = processSensorData(state, impactMag, 1000L)
        assertFalse(triggered)
        assertFalse(state.isFalling)
    }

    @Test
    fun test_freefall_then_lateImpact_noAlert() {
        val state = FallState()

        val freefallMag = magnitude(0.5f, 0.3f, 0.2f) // < 3.0
        processSensorData(state, freefallMag, 1000L)
        assertTrue(state.isFalling)

        // 超过1秒窗口，先触发重置
        val normalMag = magnitude(0f, 9.8f, 0f) // 正常重力
        processSensorData(state, normalMag, 2500L)
        assertFalse(state.isFalling)

        // 此时再来撞击，不触发
        val impactMag = magnitude(18.0f, 15.0f, 12.0f)
        val triggered = processSensorData(state, impactMag, 2600L)
        assertFalse(triggered)
    }

    @Test
    fun test_normalWalking_noAlert() {
        val state = FallState()

        val walkingMag = magnitude(1.0f, 9.5f, 1.5f) // ≈9.7, 正常走路
        val triggered = processSensorData(state, walkingMag, 1000L)
        assertFalse(triggered)
        assertFalse(state.isFalling)
    }
}
