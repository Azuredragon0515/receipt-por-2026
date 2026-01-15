package com.example.checkinreceipts.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GeoUtilsTest {

    @Test
    fun same_point_distance_is_zero() {
        val d = GeoUtils.haversineDistanceMeters(22.3000, 114.1700, 22.3000, 114.1700)
        assertEquals(0.0, d, 1e-6)
        assertTrue(GeoUtils.isInsideRadius(22.3000, 114.1700, 22.3000, 114.1700, 1.0))
    }

    @Test
    fun known_points_distance_is_reasonable() {
        val lat1 = 22.2819; val lon1 = 114.1588
        val lat2 = 22.2770; val lon2 = 114.1655
        val d = GeoUtils.haversineDistanceMeters(lat1, lon1, lat2, lon2)
        assertTrue(d in 800.0..1200.0)
    }

    @Test
    fun boundary_radius_check() {
        val lat1 = 22.3000; val lon1 = 114.1700
        val meters = 200.0
        val deltaLat = meters / 111_320.0
        val lat2 = lat1 + deltaLat
        val lon2 = lon1

        val d = GeoUtils.haversineDistanceMeters(lat1, lon1, lat2, lon2)
        assertTrue(abs(d - 200.0) < 1.0)

        assertTrue(GeoUtils.isInsideRadius(lat1, lon1, lat2, lon2, 200.0))
        assertFalse(GeoUtils.isInsideRadius(lat1, lon1, lat2, lon2, 199.0))
    }
}