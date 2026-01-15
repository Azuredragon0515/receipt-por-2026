package com.example.checkinreceipts.data.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String = "",
    val phone: String = "",
    val serverId: Long? = null,
    val pendingSync: Boolean = false,
    val pendingDelete: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)