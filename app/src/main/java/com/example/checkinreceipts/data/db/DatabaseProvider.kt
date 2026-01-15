package com.example.checkinreceipts.data.db
import android.app.Application
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null
    fun get(app: Application): AppDatabase {
        val cached = instance
        if (cached != null) return cached
        synchronized(this) {
            val again = instance
            if (again != null) return again
            val db = Room.databaseBuilder(
                app.applicationContext,
                AppDatabase::class.java,
                "app.db"
            )
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .build()
            instance = db
            return db
        }
    }
}