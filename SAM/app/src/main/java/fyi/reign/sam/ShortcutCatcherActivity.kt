package fyi.reign.sam

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import fyi.reign.sam.apk.APKPatcher
import fyi.reign.sam.data.db.SAMDatabase
import fyi.reign.sam.data.db.ShortcutEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom

class ShortcutCatcherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launcherApps = getSystemService(LauncherApps::class.java)

        @Suppress("DEPRECATION")
        val request: LauncherApps.PinItemRequest? =
            intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST)

        if (request == null || request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            finish(); return
        }

        val shortcutInfo = request.shortcutInfo ?: run { finish(); return }

        request.accept()

        val label = (shortcutInfo.shortLabel ?: shortcutInfo.longLabel ?: "Shortcut").toString()
        val sourcePackage = shortcutInfo.`package` ?: ""
        val shortcutIntent = shortcutInfo.intent ?: shortcutInfo.intents?.firstOrNull()
        val intentUri = shortcutIntent?.toUri(Intent.URI_INTENT_SCHEME) ?: ""

        val suffix = run {
            val bytes = ByteArray(6).also { SecureRandom().nextBytes(it) }
            bytes.joinToString("") { "%02x".format(it) }.take(11)
        }
        val generatedPackage = "fyi.reign.sam.shortcut.$suffix"

        val iconBitmap: Bitmap? = try {
            val drawable = launcherApps.getShortcutIconDrawable(
                shortcutInfo, resources.displayMetrics.densityDpi
            )
            drawable?.let { drawableToBitmap(it) }
        } catch (e: Exception) { null }

        val iconPath: String? = iconBitmap?.let { bmp ->
            val iconFile = File(filesDir, "icons/$generatedPackage.png")
            iconFile.parentFile?.mkdirs()
            FileOutputStream(iconFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            iconFile.absolutePath
        }

        val context = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val entry = ShortcutEntry(
                label = label,
                sourcePackage = sourcePackage,
                intentUri = intentUri,
                generatedPackageName = generatedPackage,
                iconPath = iconPath,
                createdAt = System.currentTimeMillis(),
                isInstalled = false
            )
            SAMDatabase.getInstance(context).shortcutDao().insert(entry)
            APKPatcher.patchAndInstall(context, generatedPackage, label, iconBitmap)
        }

        finish()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val w = drawable.intrinsicWidth.coerceAtLeast(1)
        val h = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return bitmap
    }
}
