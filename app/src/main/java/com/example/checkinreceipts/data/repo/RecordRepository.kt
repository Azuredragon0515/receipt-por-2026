package com.example.checkinreceipts.data.repo
import android.content.Context
import com.example.checkinreceipts.data.db.DatabaseProvider
import com.example.checkinreceipts.data.entity.RecordEntity
import com.example.checkinreceipts.domain.model.Record
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class RecordRepository private constructor(context: Context) {
    private val db = DatabaseProvider.get(context as android.app.Application)
    private val recordDao = db.recordDao()

    fun observeAll(): Flow<List<Record>> =
        recordDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    suspend fun getLatestCheckIn(): Record? {
        return recordDao.getLatestByType("check-in")?.toDomain()
    }

    suspend fun add(record: Record): Long {
        return recordDao.insert(record.toEntity())
    }

    suspend fun addAll(vararg records: Record): List<Long> {
        return recordDao.insertAll(records.map { it.toEntity() })
    }

    suspend fun addAll(records: List<Record>): List<Long> {
        return recordDao.insertAll(records.map { it.toEntity() })
    }

    suspend fun insertCheckIn(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        note: String? = null,
        createdAt: Long = System.currentTimeMillis()
    ): Long {
        val record = Record(
            id = 0L,
            header = null,
            dateText = null,
            totalText = null,
            note = note,
            type = "check-in",
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            createdAt = createdAt
        )
        return add(record)
    }

    suspend fun quickAddNote(note: String): Long {
        val now = System.currentTimeMillis()
        val record = Record(
            id = 0L,
            header = null,
            dateText = null,
            totalText = null,
            note = note,
            type = null,
            latitude = null,
            longitude = null,
            accuracy = null,
            createdAt = now
        )
        return add(record)
    }

    fun quickAddNoteSync(note: String): Long = runBlocking {
        quickAddNote(note)
    }

    companion object {
        @Volatile private var INSTANCE: RecordRepository? = null

        fun getInstance(context: Context): RecordRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecordRepository(context).also { INSTANCE = it }
            }
    }
}

private fun com.example.checkinreceipts.data.entity.RecordEntity.toDomain(): com.example.checkinreceipts.domain.model.Record =
    com.example.checkinreceipts.domain.model.Record(
        id = id,
        header = header,
        dateText = dateText,
        totalText = totalText,
        note = note,
        type = type,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        createdAt = createdAt
    )

private fun com.example.checkinreceipts.domain.model.Record.toEntity(): RecordEntity =
    RecordEntity(
        id = if (id == 0L) 0L else id,
        header = header,
        dateText = dateText,
        totalText = totalText,
        note = note,
        type = type,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        createdAt = createdAt
    )