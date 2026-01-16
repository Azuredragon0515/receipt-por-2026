package com.example.checkinreceipts.data.db.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.checkinreceipts.data.entity.RecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE type = :type ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByType(type: String): RecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RecordEntity>): List<Long>

    @Update
    suspend fun update(vararg items: RecordEntity)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: Long)
}