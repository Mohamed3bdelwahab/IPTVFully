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
                AppDatabase.MIGRATION_3_4 // fix schema integrity
            )
            .fallbackToDestructiveMigrationOnDowngrade() // only destructive on downgrade
            .build()
            INSTANCE = instance
            instance
        }
    }
}
