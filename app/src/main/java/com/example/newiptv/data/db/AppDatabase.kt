package com.example.newiptv.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.newiptv.data.db.entities.*

@Database(
    entities = [
        CategoryEntity::class,
        ItemEntity::class,
        InfoEntity::class,
        EpisodeEntity::class,
        SeasonEntity::class   // ✅ Added SeasonEntity
    ],
    version = 4,              // ✅ bumped to v4 to fix schema integrity
    exportSchema = false      // ✅ no schema export (simpler for dev)
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun infoDao(): InfoDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun seasonDao(): SeasonDao   // ✅ added SeasonDao

    companion object {
        // 🔹 Migration v1 → v2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add SeasonEntity table if missing
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `seasons` (
                        `seasonId` TEXT NOT NULL,
                        `seriesId` TEXT NOT NULL,
                        `seasonNumber` INTEGER NOT NULL,
                        PRIMARY KEY(`seasonId`)
                    )
                    """.trimIndent()
                )
            }
        }

        // 🔹 Migration v2 → v3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Future schema updates (e.g. add new columns) go here
            }
        }

        // 🔹 Migration v3 → v4 (Fix schema integrity)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Recreate tables to ensure schema integrity
                database.execSQL("DROP TABLE IF EXISTS categories")
                database.execSQL("DROP TABLE IF EXISTS items")
                database.execSQL("DROP TABLE IF EXISTS info")
                database.execSQL("DROP TABLE IF EXISTS episodes")
                database.execSQL("DROP TABLE IF EXISTS seasons")
                
                // Recreate categories table
                database.execSQL("""
                    CREATE TABLE categories (
                        categoryId TEXT NOT NULL PRIMARY KEY,
                        categoryName TEXT NOT NULL,
                        parentId INTEGER NOT NULL,
                        type TEXT NOT NULL
                    )
                """.trimIndent())
                
                // Recreate items table
                database.execSQL("""
                    CREATE TABLE items (
                        itemId TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        cover TEXT,
                        plot TEXT,
                        cast TEXT,
                        director TEXT,
                        genre TEXT,
                        releaseDate TEXT,
                        lastModified TEXT,
                        rating TEXT,
                        rating5Based REAL,
                        backdropPath TEXT,
                        youtubeTrailer TEXT,
                        episodeRunTime TEXT,
                        categoryId TEXT NOT NULL,
                        type TEXT NOT NULL
                    )
                """.trimIndent())
                
                // Recreate info table
                database.execSQL("""
                    CREATE TABLE info (
                        itemId TEXT NOT NULL PRIMARY KEY,
                        name TEXT,
                        cover TEXT,
                        plot TEXT,
                        cast TEXT,
                        director TEXT,
                        genre TEXT,
                        releaseDate TEXT,
                        lastModified TEXT,
                        rating TEXT,
                        rating5Based REAL,
                        backdropPath TEXT,
                        youtubeTrailer TEXT,
                        episodeRunTime TEXT,
                        categoryId TEXT NOT NULL,
                        type TEXT NOT NULL
                    )
                """.trimIndent())
                
                // Recreate episodes table
                database.execSQL("""
                    CREATE TABLE episodes (
                        id TEXT NOT NULL PRIMARY KEY,
                        itemId TEXT NOT NULL,
                        season INTEGER NOT NULL,
                        episodeNum INTEGER NOT NULL,
                        title TEXT,
                        containerExtension TEXT,
                        movieImage TEXT,
                        plot TEXT,
                        rating TEXT,
                        releaseDate TEXT,
                        directSource TEXT
                    )
                """.trimIndent())
                
                // Recreate seasons table
                database.execSQL("""
                    CREATE TABLE seasons (
                        seasonId TEXT NOT NULL PRIMARY KEY,
                        seriesId TEXT NOT NULL,
                        seasonNumber INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
