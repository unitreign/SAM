package fyi.reign.sam.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortcuts")
data class ShortcutEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val sourcePackage: String,
    val intentUri: String,
    val generatedPackageName: String,
    val iconPath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isInstalled: Boolean = false
)
