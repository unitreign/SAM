package fyi.reign.sam.data

import android.content.Context
import android.content.pm.PackageManager
import fyi.reign.sam.data.db.SAMDatabase
import fyi.reign.sam.data.db.ShortcutEntry
import kotlinx.coroutines.flow.Flow

class ShortcutRepository(context: Context) {
    private val dao = SAMDatabase.getInstance(context).shortcutDao()
    private val pm = context.packageManager

    val shortcuts: Flow<List<ShortcutEntry>> = dao.getAll()

    suspend fun insert(entry: ShortcutEntry): Long = dao.insert(entry)

    suspend fun delete(entry: ShortcutEntry) = dao.delete(entry)

    suspend fun getByPackageName(packageName: String): ShortcutEntry? =
        dao.getByPackageName(packageName)

    suspend fun refreshInstallStatus(entry: ShortcutEntry) {
        val installed = try {
            pm.getPackageInfo(entry.generatedPackageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        dao.updateInstalled(entry.generatedPackageName, installed)
    }

    fun isInstalled(packageName: String): Boolean = try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
