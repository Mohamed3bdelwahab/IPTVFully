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
        SeasonEntity::class,
        // 🔹 Movie Entities
        MovieCategoryEntity::class,
        MovieItemEntity::class,
        MovieInfoEntity::class,
        MovieStreamDataEntity::class,
        // 🔹 Video Player Enhancement Entities
        PlaybackPositionEntity::class,
        PlaylistItemEntity::class
    ],
    version = 7,              // ✅ bumped to v7 for video player enhancements
    exportSchema = false      // ✅ no schema export (simpler for dev)
)
abstract class AppDatabase : RoomDatabase() {

    // 🔹 Series DAOs
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun infoDao(): InfoDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun seasonDao(): SeasonDao

    // 🔹 Movie DAOs
    abstract fun movieCategoryDao(): MovieCategoryDao
    abstract fun movieItemDao(): MovieItemDao
    abstract fun movieInfoDao(): MovieInfoDao
    abstract fun movieStreamDataDao(): MovieStreamDataDao

    // 🔹 Video Player Enhancement DAOs
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun playlistItemDao(): PlaylistItemDao

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

        // 🔹 Migration v4 → v5 (Add Movie Entities)
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create movie_categories table
                database.execSQL("""
                    CREATE TABLE movie_categories (
                        categoryId TEXT NOT NULL PRIMARY KEY,
                        categoryName TEXT NOT NULL,
                        parentId INTEGER NOT NULL,
                        type TEXT NOT NULL
                    )
                """.trimIndent())

                // Create movie_items table
                database.execSQL("""
                    CREATE TABLE movie_items (
                        itemId TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        streamType TEXT NOT NULL,
                        streamId TEXT NOT NULL,
                        streamIcon TEXT,
                        rating TEXT,
                        rating5Based REAL,
                        added TEXT,
                        isAdult TEXT NOT NULL,
                        categoryId TEXT NOT NULL,
                        containerExtension TEXT,
                        customSid TEXT,
                        directSource TEXT,
                        type TEXT NOT NULL
                    )
                """.trimIndent())

                // Create movie_info table
                database.execSQL("""
                    CREATE TABLE movie_info (
                        itemId TEXT NOT NULL PRIMARY KEY,
                        name TEXT,
                        originalName TEXT,
                        coverBig TEXT,
                        movieImage TEXT,
                        releaseDate TEXT,
                        episodeRunTime INTEGER NOT NULL,
                        youtubeTrailer TEXT,
                        director TEXT,
                        actors TEXT,
                        cast TEXT,
                        description TEXT,
                        plot TEXT,
                        age TEXT,
                        ratingMpaa TEXT,
                        ratingKinopoisk REAL,
                        ratingCountKinopoisk INTEGER NOT NULL,
                        country TEXT,
                        genre TEXT,
                        backdropPath TEXT,
                        tmdbId TEXT,
                        durationSecs INTEGER NOT NULL,
                        duration TEXT,
                        bitrate INTEGER NOT NULL,
                        backdrop TEXT,
                        rating TEXT,
                        categoryId TEXT NOT NULL,
                        type TEXT NOT NULL
                    )
                """.trimIndent())

                // Create movie_stream_data table
                database.execSQL("""
                    CREATE TABLE movie_stream_data (
                        itemId TEXT NOT NULL PRIMARY KEY,
                        name TEXT,
                        added TEXT,
                        categoryId TEXT NOT NULL,
                        containerExtension TEXT,
                        customSid TEXT,
                        directSource TEXT,
                        type TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        // 🔹 Migration v5 → v6 (Simple version bump)
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Simple version bump - let fallbackToDestructiveMigration handle schema issues
            }
        }

        // 🔹 Migration v6 → v7 (Add Video Player Enhancement Tables)
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create playback_positions table
                database.execSQL("""
                    CREATE TABLE playback_positions (
                        contentId TEXT NOT NULL PRIMARY KEY,
                        contentType TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        seriesId TEXT,
                        seasonNumber INTEGER,
                        episodeNumber INTEGER
                    )
                """.trimIndent())

                // Create playlist_items table
                database.execSQL("""
                    CREATE TABLE playlist_items (
                        itemId TEXT NOT NULL PRIMARY KEY,
                        playlistId TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        contentType TEXT NOT NULL,
                        contentId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        thumbnail TEXT
                    )
                """.trimIndent())
            }
        }
    }
}
