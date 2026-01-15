package com.example.checkinreceipts.sensors

sealed class ShakeEvent {
    data class Triggered(val timestampMs: Long) : ShakeEvent()
    data class CoolingDown(val remainingMs: Long) : ShakeEvent()
    data class Telemetry(
        val nowMs: Long,
        val ax: Float,
        val ay: Float,
        val az: Float,
        val magnitude: Float,
        val source: String,
        val sps: Int
    ) : ShakeEvent()
}