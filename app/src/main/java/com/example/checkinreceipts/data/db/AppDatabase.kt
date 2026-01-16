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
    version = 3,
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
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE records ADD COLUMN type TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN latitude REAL")
                database.execSQL("ALTER TABLE records ADD COLUMN longitude REAL")
                database.execSQL("ALTER TABLE records ADD COLUMN accuracy REAL")
            }
        }
    }
}