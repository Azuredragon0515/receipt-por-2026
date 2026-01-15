package com.example.checkinreceipts.sensors

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sqrt

class ShakeLogic(
    private val threshold: Float = 10.0f,
    private val requiredPeaks: Int = 2,
    private val windowMs: Long = 800L,
    private val cooldownMs: Long = 5000L,
    private val minAxisContribution: Float = 0.35f
) {
    private var lastEmit: Long = 0L
    private var lastSign = 0f
    private var peaks = 0
    private var windowStart = 0L
    private var maxMagInWindow = 0f
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private val alpha = 0.9f
    private var coolingAnnounced: Boolean = false
    fun coolingRemaining(now: Long): Long {
        val rem = cooldownMs - (now - lastEmit)
        return if (rem > 0) rem else 0
    }

    fun linear(ax: Float, ay: Float, az: Float): Triple<Float, Float, Float> {
        gx = alpha * gx + (1 - alpha) * ax
        gy = alpha * gy + (1 - alpha) * ay
        gz = alpha * gz + (1 - alpha) * az
        val lx = ax - gx
        val ly = ay - gy
        val lz = az - gz
        return Triple(lx, ly, lz)
    }

    fun magnitude(lx: Float, ly: Float, lz: Float): Float {
        return sqrt(lx * lx + ly * ly + lz * lz)
    }

    fun feed(ax: Float, ay: Float, az: Float, now: Long): ShakeEvent? {
        val (lx, ly, lz) = linear(ax, ay, az)
        val mag = magnitude(lx, ly, lz)

        val remaining = coolingRemaining(now)
        if (remaining > 0) {
            if (!coolingAnnounced) {
                coolingAnnounced = true
                return ShakeEvent.CoolingDown(remaining)
            }
            return null
        } else {
            coolingAnnounced = false
        }

        val dom = max(abs(lx), max(abs(ly), abs(lz)))
        val sum = abs(lx) + abs(ly) + abs(lz) + 1e-6f
        val contribution = dom / sum

        if (windowStart == 0L) windowStart = now
        if (now - windowStart > windowMs) {
            peaks = 0
            lastSign = 0f
            windowStart = now
            maxMagInWindow = 0f
        }

        if (mag > maxMagInWindow) maxMagInWindow = mag

        val signNow = sign(
            when (dom) {
                abs(lx) -> lx
                abs(ly) -> ly
                else -> lz
            }
        )

        if (mag >= threshold && contribution >= minAxisContribution) {
            if (lastSign != 0f && signNow != 0f && signNow != lastSign) {
                peaks++
            }
            lastSign = signNow
        }

        if (peaks >= requiredPeaks && maxMagInWindow >= threshold) {
            lastEmit = now
            peaks = 0
            lastSign = 0f
            windowStart = now
            maxMagInWindow = 0f
            coolingAnnounced = false
            return ShakeEvent.Triggered(now)
        }

        return ShakeEvent.Telemetry(
            nowMs = now,
            ax = lx, ay = ly, az = lz,
            magnitude = mag,
            source = "ACCEL-linear",
            sps = 0
        )
    }
}