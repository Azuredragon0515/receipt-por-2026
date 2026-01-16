package com.example.checkinreceipts.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Record(
    val id: Long = 0L,
    val header: String?,
    val dateText: String?,
    val totalText: String?,
    val note: String? = null,
    val type: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)