package com.example.checkinreceipts.data.export

import android.content.Context
import android.os.Environment
import com.example.checkinreceipts.data.repo.RecordRepository
import com.example.checkinreceipts.domain.model.Record
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class Exporter(
    private val context: Context,
    private val repo: RecordRepository
) {
    private val json = Json { prettyPrint = true }
    suspend fun exportAll(): File {
        val flow = repo.observeAll()
        val snapshot: List<Record> = flow.first()
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(dir, "records_${System.currentTimeMillis()}.json")
        file.writeText(json.encodeToString(snapshot))
        return file
    }
}
