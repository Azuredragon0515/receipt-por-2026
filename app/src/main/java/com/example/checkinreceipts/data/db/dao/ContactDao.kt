package com.example.checkinreceipts.data.db.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.checkinreceipts.data.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg items: ContactEntity): List<Long>

    @Update
    suspend fun update(vararg items: ContactEntity)

    @Query("UPDATE contacts SET serverId = :serverId, pendingSync = 0, updatedAt = :updatedAt WHERE id = :localId")
    suspend fun setServerIdAndSynced(localId: Long, serverId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET pendingSync = 0, updatedAt = :updatedAt WHERE id = :localId")
    suspend fun clearPendingSync(localId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET pendingDelete = :pending, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPendingDelete(id: Long, pending: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM contacts WHERE pendingDelete = 1 OR pendingSync = 1")
    suspend fun getPending(): List<ContactEntity>

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): ContactEntity?
}