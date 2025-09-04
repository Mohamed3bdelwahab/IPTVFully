package com.example.newiptv.data.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "newiptv_database"
            )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4, // fix schema integrity
                AppDatabase.MIGRATION_4_5, // add movie entities
                AppDatabase.MIGRATION_5_6, // fix schema integrity v2
                AppDatabase.MIGRATION_6_7  // add video player enhancements
            )
            .fallbackToDestructiveMigration() // force fresh database on schema mismatch
            .build()
            INSTANCE = instance
            instance
        }
    }
}
