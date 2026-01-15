package com.example.checkinreceipts.data.db
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.checkinreceipts.data.db.dao.ContactDao
import com.example.checkinreceipts.data.db.dao.RecordDao
import com.example.checkinreceipts.data.entity.ContactEntity
import com.example.checkinreceipts.data.entity.RecordEntity

@Database(
    entities = [
        RecordEntity::class,
        ContactEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun recordDao(): RecordDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }
    }
}