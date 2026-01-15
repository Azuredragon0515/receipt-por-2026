package com.example.checkinreceipts.device.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class SimpleLocation(val lat: Double, val lng: Double, val accuracy: Float?)

class LocationClient(private val context: Context) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
        .setMinUpdateIntervalMillis(2000L)
        .build()
    fun hasPermission(): Boolean {
        val f = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val c = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return f || c
    }

    fun locationUpdates(): Flow<SimpleLocation> = callbackFlow {
        if (!hasPermission()) {
            close(IllegalStateException("Location permission not granted"))
            return@callbackFlow
        }
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val l = result.lastLocation ?: return
                trySend(SimpleLocation(l.latitude, l.longitude, l.accuracy))
            }
        }
        fused.requestLocationUpdates(request, callback, context.mainLooper)
        awaitClose { fused.removeLocationUpdates(callback) }
    }
}