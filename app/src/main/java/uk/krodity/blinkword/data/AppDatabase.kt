package uk.krodity.blinkword.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Document::class,
        Chapter::class,
        ContentChunk::class,
        Collection::class,
        DocumentCollectionCrossRef::class,
        ReadingDay::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun chapterDao(): ChapterDao
    abstract fun contentChunkDao(): ContentChunkDao
    abstract fun collectionDao(): CollectionDao
    abstract fun readingDayDao(): ReadingDayDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "blinkword.db",
            )
                // v2 -> v3 drops documents.content in favor of ContentChunk rows (see that
                // class): a real migration would need to read the old content column to
                // split it up, but that's the exact column whose oversized rows crash with
                // SQLiteBlobTooBigException -- the bug this migration exists to fix. Nothing
                // sane to migrate from a row Room can't safely read, so destructive it is;
                // no installed base has a library worth preserving through this yet either.
                // v3 -> v4 adds the collections tables, v4 -> v5 adds
                // documents.author/format, v5 -> v6 adds reading_days, v6 -> v7 adds
                // documents.sourceUrl -- all stay destructive to match. Backup/restore
                // (see BackupRepository) is the supported way across a wipe.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
