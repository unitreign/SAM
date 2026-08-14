package fyi.reign.sam.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ShortcutEntry::class], version = 1, exportSchema = false)
abstract class SAMDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile private var instance: SAMDatabase? = null

        fun getInstance(context: Context): SAMDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, SAMDatabase::class.java, "sam.db")
                    .build()
                    .also { instance = it }
            }
    }
}
