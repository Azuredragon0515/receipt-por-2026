package com.example.checkinreceipts.data.repo

import com.example.checkinreceipts.data.db.dao.ContactDao
import com.example.checkinreceipts.data.entity.ContactEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ContactsRepository(
    private val dao: ContactDao,
    private val io: CoroutineDispatcher
) {
    fun observe(): Flow<List<ContactEntity>> = dao.observeAll()
    suspend fun refresh(): Result<Unit> = withContext(io) {
        runCatching { Unit }
    }

    suspend fun initialSync(): Result<Unit> = refresh()

    suspend fun addLocalThenSync(name: String, phone: String): Result<Unit> = withContext(io) {
        runCatching {
            dao.upsert(
                ContactEntity(
                    name = name,
                    phone = phone,
                    pendingSync = true,
                    pendingDelete = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
            Unit
        }
    }

    suspend fun deleteOptimistic(entity: ContactEntity): Result<Unit> = withContext(io) {
        runCatching {
            dao.setPendingDelete(entity.id, true)
            Unit
        }
    }

    suspend fun undoDelete(localId: Long): Result<Unit> = withContext(io) {
        runCatching {
            dao.setPendingDelete(localId, false)
            Unit
        }
    }

    suspend fun retryPending(): Result<Unit> = withContext(io) {
        runCatching {
            val pending = dao.getPending()
            for (c in pending) {
                if (c.pendingDelete) {
                    dao.deleteById(c.id)
                } else if (c.pendingSync) {
                    dao.clearPendingSync(c.id)
                }
            }
            Unit
        }
    }
}