package fyi.reign.sam.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    @Insert
    suspend fun insert(entry: ShortcutEntry): Long

    @Delete
    suspend fun delete(entry: ShortcutEntry)

    @Query("SELECT * FROM shortcuts ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ShortcutEntry>>

    @Query("SELECT * FROM shortcuts WHERE generatedPackageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): ShortcutEntry?

    @Query("UPDATE shortcuts SET isInstalled = :installed WHERE generatedPackageName = :packageName")
    suspend fun updateInstalled(packageName: String, installed: Boolean)
}
