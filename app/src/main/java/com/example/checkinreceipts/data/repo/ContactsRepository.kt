package com.example.checkinreceipts.data.repo

import com.example.checkinreceipts.data.db.dao.ContactDao
import com.example.checkinreceipts.data.entity.ContactEntity
import com.example.checkinreceipts.data.remote.ContactsApi
import com.example.checkinreceipts.data.remote.dto.ContactDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ContactsRepository(
    private val dao: ContactDao,
    private val io: CoroutineDispatcher,
    private val api: ContactsApi? = null
) {
    fun observe(): Flow<List<ContactEntity>> = dao.observeAll()

    suspend fun refresh(): Result<Unit> = withContext(io) {
        runCatching {
            val a = api ?: return@runCatching Unit
            val remote = a.list()
            for (r in remote) {
                val ent = ContactEntity(
                    name = r.name,
                    phone = r.phone,
                    serverId = r.id,
                    pendingSync = false,
                    pendingDelete = false,
                    updatedAt = System.currentTimeMillis()
                )
                dao.upsert(ent)
            }
            Unit
        }
    }

    suspend fun addLocalThenSync(name: String, phone: String): Result<Unit> = withContext(io) {
        runCatching {
            val a = api
            if (a != null) {
                val created = runCatching { a.create(ContactDto(name = name, phone = phone)) }.getOrNull()
                if (created?.id != null) {
                    dao.upsert(
                        ContactEntity(
                            name = created.name,
                            phone = created.phone,
                            serverId = created.id,
                            pendingSync = false,
                            pendingDelete = false,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    return@runCatching Unit
                }
            }
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

    suspend fun updateLocalThenSync(id: Long, name: String, phone: String): Result<Unit> = withContext(io) {
        runCatching {
            val a = api
            if (a != null) {
                val created = runCatching { a.create(ContactDto(name = name, phone = phone)) }.getOrNull()
                if (created?.id != null) {
                    dao.upsert(
                        ContactEntity(
                            id = id,
                            name = created.name,
                            phone = created.phone,
                            serverId = created.id,
                            pendingSync = false,
                            pendingDelete = false,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    return@runCatching Unit
                }
            }
            dao.update(
                ContactEntity(
                    id = id,
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

    suspend fun markPendingDelete(localId: Long): Result<Unit> = withContext(io) {
        runCatching { dao.setPendingDelete(localId, true) }
    }

    suspend fun undoDelete(localId: Long): Result<Unit> = withContext(io) {
        runCatching { dao.setPendingDelete(localId, false) }
    }

    suspend fun commitDelete(localId: Long): Result<Unit> = withContext(io) {
        runCatching {
            val c = dao.getById(localId)
            val sid = c?.serverId
            val a = api
            if (a != null && sid != null) {
                val ok = runCatching { a.delete(sid) }.isSuccess
                if (ok) {
                    dao.deleteById(localId)
                    return@runCatching Unit
                }
            }
            dao.deleteById(localId)
            Unit
        }
    }

    suspend fun retryPending(): Result<Unit> = withContext(io) {
        runCatching {
            val a = api ?: return@runCatching Unit
            val pending = dao.getPending()
            for (c in pending) {
                if (c.pendingDelete) continue
                if (c.pendingSync) {
                    val res = runCatching { a.create(ContactDto(name = c.name, phone = c.phone)) }.getOrNull()
                    res?.id?.let { dao.setServerIdAndSynced(c.id, it) }
                }
            }
            Unit
        }
    }
}