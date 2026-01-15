package com.example.checkinreceipts.device.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.checkinreceipts.sensors.ShakeEvent
import com.example.checkinreceipts.sensors.ShakeLogic
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ShakeDetector(private val context: Context) {
    fun events(
        threshold: Float = 10.0f,
        requiredPeaks: Int = 2,
        windowMs: Long = 800L,
        cooldownMs: Long = 5000L,
        minAxisContribution: Float = 0.35f
    ): Flow<ShakeEvent> = callbackFlow {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: run { close(IllegalStateException("No TYPE_ACCELEROMETER")); return@callbackFlow }

        val logic = ShakeLogic(
            threshold = threshold,
            requiredPeaks = requiredPeaks,
            windowMs = windowMs,
            cooldownMs = cooldownMs,
            minAxisContribution = minAxisContribution
        )

        var samples = 0
        var lastSecond = System.currentTimeMillis()
        var sps = 0

        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val now = System.currentTimeMillis()
                samples++
                if (now - lastSecond >= 1000L) {
                    sps = samples
                    samples = 0
                    lastSecond = now
                }

                val ax = e.values[0]
                val ay = e.values[1]
                val az = e.values[2]

                val ev = logic.feed(ax, ay, az, now)
                if (ev is ShakeEvent.Telemetry) {
                    trySend(ev.copy(sps = sps))
                } else if (ev != null) {
                    trySend(ev)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sm.unregisterListener(listener) }
    }
}