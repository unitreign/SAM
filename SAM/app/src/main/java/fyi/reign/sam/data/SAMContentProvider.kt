package fyi.reign.sam.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import fyi.reign.sam.data.db.SAMDatabase
import kotlinx.coroutines.runBlocking

class SAMContentProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "fyi.reign.sam.provider"
        private const val MATCH_SHORTCUT = 1
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "shortcut/*", MATCH_SHORTCUT)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (uriMatcher.match(uri) != MATCH_SHORTCUT) return null
        val packageName = uri.lastPathSegment ?: return null

        val dao = SAMDatabase.getInstance(context!!).shortcutDao()
        val entry = runBlocking { dao.getByPackageName(packageName) } ?: return MatrixCursor(arrayOf("intent_uri"))

        val cursor = MatrixCursor(arrayOf("intent_uri"))
        cursor.addRow(arrayOf(entry.intentUri))
        return cursor
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
