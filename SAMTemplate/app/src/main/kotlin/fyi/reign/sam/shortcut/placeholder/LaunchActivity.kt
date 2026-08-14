package fyi.reign.sam.shortcut.PLACEHOLDER

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.widget.TextView

class LaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val samPackage = "fyi.reign.sam"

        val samInstalled = try {
            packageManager.getPackageInfo(samPackage, 0)
            true
        } catch (e: Exception) {
            false
        }

        if (!samInstalled) {
            val tv = TextView(this)
            tv.text = "Shortcut APK Maker is not installed. Please reinstall it."
            tv.setPadding(48, 48, 48, 48)
            setContentView(tv)
            return
        }

        val uri = Uri.parse("content://$samPackage.provider/shortcut/${packageName}")
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val intentUri = cursor.getString(0)
                val intent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Nothing to do — just finish
        } finally {
            cursor?.close()
        }

        finish()
    }
}
