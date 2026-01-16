package com.example.checkinreceipts.data.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val header: String? = null,
    val dateText: String? = null,
    val totalText: String? = null,
    val note: String? = null,
    val type: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)