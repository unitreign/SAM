package fyi.reign.sam.apk

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import com.android.apksig.ApkSigner
import fyi.reign.sam.keystore.KeystoreManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object APKPatcher {

    private val ICON_PATHS = listOf(
        "res/mipmap-mdpi-v4/ic_launcher.png",
        "res/mipmap-hdpi-v4/ic_launcher.png",
        "res/mipmap-xhdpi-v4/ic_launcher.png",
        "res/mipmap-xxhdpi-v4/ic_launcher.png",
        "res/mipmap-xxxhdpi-v4/ic_launcher.png",
        "res/mipmap-mdpi-v4/ic_launcher_round.png",
        "res/mipmap-hdpi-v4/ic_launcher_round.png",
        "res/mipmap-xhdpi-v4/ic_launcher_round.png",
        "res/mipmap-xxhdpi-v4/ic_launcher_round.png",
        "res/mipmap-xxxhdpi-v4/ic_launcher_round.png",
    )

    private val ICON_SIZES = mapOf(
        "mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192
    )

    fun patchAndInstall(
        context: Context,
        newPackage: String,
        label: String,
        iconBitmap: Bitmap?
    ) {
        val workDir = File(context.filesDir, "apk_work").also { it.mkdirs() }
        val outputDir = File(context.filesDir, "generated_apks").also { it.mkdirs() }
        val templateFile = File(workDir, "template_copy.apk")
        val unsignedFile = File(workDir, "unsigned.apk")
        val signedFile = File(outputDir, "$newPackage.apk")

        // Copy template from assets
        context.assets.open("template.apk").use { input ->
            FileOutputStream(templateFile).use { input.copyTo(it) }
        }

        // Build icon bitmaps per density
        val iconBytes: Map<String, ByteArray>? = iconBitmap?.let { src ->
            ICON_SIZES.mapValues { (_, size) ->
                val scaled = Bitmap.createScaledBitmap(src, size, size, true)
                ByteArrayOutputStream().also { out ->
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                }.toByteArray()
            }
        }

        // Patch entries into new ZIP (strip old V1 signature files; apksig adds new ones)
        ZipFile(templateFile).use { zip ->
            ZipOutputStream(FileOutputStream(unsignedFile)).use { out ->
                for (entry in zip.entries()) {
                    if (isOldSignatureEntry(entry.name)) continue

                    val data = when {
                        entry.name == "AndroidManifest.xml" -> {
                            val original = zip.getInputStream(entry).readBytes()
                            AXMLPatcher.patch(original, newPackage, label)
                        }
                        iconBytes != null && ICON_PATHS.contains(entry.name) -> {
                            val density = densityFromPath(entry.name)
                            iconBytes[density] ?: zip.getInputStream(entry).readBytes()
                        }
                        else -> zip.getInputStream(entry).readBytes()
                    }

                    val newEntry = ZipEntry(entry.name)
                    if (entry.method == ZipEntry.STORED) {
                        newEntry.method = ZipEntry.STORED
                        val crc = java.util.zip.CRC32().also { it.update(data) }.value
                        newEntry.crc = crc
                        newEntry.size = data.size.toLong()
                        newEntry.compressedSize = data.size.toLong()
                    } else {
                        newEntry.method = ZipEntry.DEFLATED
                    }
                    out.putNextEntry(newEntry)
                    out.write(data)
                    out.closeEntry()
                }
            }
        }

        // Sign with V1 + V2
        val signerConfig = ApkSigner.SignerConfig.Builder(
            "SAM",
            KeystoreManager.getPrivateKey(),
            listOf(KeystoreManager.getCertificate())
        ).build()

        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(unsignedFile)
            .setOutputApk(signedFile)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .build()
            .sign()

        // Trigger install
        val apkUri = FileProvider.getUriForFile(
            context,
            "fyi.reign.sam.fileprovider",
            signedFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)

        // Cleanup work files
        templateFile.delete()
        unsignedFile.delete()
    }

    private fun isOldSignatureEntry(name: String): Boolean {
        if (!name.startsWith("META-INF/")) return false
        val base = name.removePrefix("META-INF/")
        return base == "MANIFEST.MF" || base.endsWith(".SF") ||
                base.endsWith(".RSA") || base.endsWith(".DSA") || base.endsWith(".EC")
    }

    private fun densityFromPath(path: String): String {
        return when {
            "xxxhdpi" in path -> "xxxhdpi"
            "xxhdpi" in path -> "xxhdpi"
            "xhdpi" in path -> "xhdpi"
            "hdpi" in path -> "hdpi"
            else -> "mdpi"
        }
    }
}
