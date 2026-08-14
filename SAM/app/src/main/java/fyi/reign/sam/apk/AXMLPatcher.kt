package fyi.reign.sam.apk

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * In-place patcher for Android Binary XML (AXML) string pools.
 *
 * Replacements must be the same byte length as the originals. The template
 * APK is built with deliberate placeholder strings to guarantee this:
 *  - "fyi.reign.sam.shortcut.PLACEHOLDER" (34 chars) → new package (34 chars, 11-char suffix)
 *  - "fyi.reign.sam.shortcut.PLACEHOLDER.LaunchActivity" (49 chars) → same length
 *  - "SAMTemplate" (11 chars) → label truncated/space-padded to exactly 11 chars
 */
object AXMLPatcher {

    private const val AXML_MAGIC = 0x00080003
    private const val CHUNK_STRING_POOL = 0x001C0001
    private const val FLAG_UTF8 = 0x100

    fun patch(axml: ByteArray, newPackage: String, rawLabel: String): ByteArray {
        val buf = axml.copyOf()
        val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)

        require(bb.int == AXML_MAGIC) { "Not an AXML file" }
        bb.int // fileSize

        val chunkStart = bb.position()
        require(bb.int == CHUNK_STRING_POOL) { "Expected StringPool chunk" }
        bb.int // chunkSize
        val stringCount = bb.int
        bb.int // styleCount
        val flags = bb.int
        val stringsStart = bb.int
        bb.int // stylesStart

        val isUtf8 = (flags and FLAG_UTF8) != 0
        val stringsDataStart = chunkStart + stringsStart

        val offsets = IntArray(stringCount) { bb.int }

        val labelPadded = rawLabel.take(11).padEnd(11, ' ')
        val oldPkg = "fyi.reign.sam.shortcut.PLACEHOLDER"
        val oldActivity = "$oldPkg.LaunchActivity"
        val newActivity = "$newPackage.LaunchActivity"

        for (i in 0 until stringCount) {
            val strByteOffset = stringsDataStart + offsets[i]
            val str = if (isUtf8) readUtf8(buf, strByteOffset) else readUtf16(buf, strByteOffset)
            when (str) {
                oldPkg -> replaceContent(buf, strByteOffset, oldPkg, newPackage, isUtf8)
                oldActivity -> replaceContent(buf, strByteOffset, oldActivity, newActivity, isUtf8)
                "SAMTemplate" -> replaceContent(buf, strByteOffset, "SAMTemplate", labelPadded, isUtf8)
            }
        }
        return buf
    }

    private fun readUtf8(data: ByteArray, offset: Int): String {
        var pos = offset
        // skip utf16 char count (1 or 2 bytes)
        if (data[pos].toInt() and 0x80 != 0) pos += 2 else pos++
        // read utf8 byte count (1 or 2 bytes)
        val byteLen: Int
        if (data[pos].toInt() and 0x80 != 0) {
            byteLen = ((data[pos].toInt() and 0x7F) shl 8) or (data[pos + 1].toInt() and 0xFF)
            pos += 2
        } else {
            byteLen = data[pos].toInt() and 0xFF
            pos++
        }
        return String(data, pos, byteLen, Charsets.UTF_8)
    }

    private fun readUtf16(data: ByteArray, offset: Int): String {
        val bb = ByteBuffer.wrap(data, offset, data.size - offset).order(ByteOrder.LITTLE_ENDIAN)
        var charLen = bb.short.toInt() and 0xFFFF
        if (charLen and 0x8000 != 0) {
            charLen = ((charLen and 0x7FFF) shl 16) or (bb.short.toInt() and 0xFFFF)
        }
        val bytes = ByteArray(charLen * 2).also { bb.get(it) }
        return String(bytes, Charsets.UTF_16LE)
    }

    private fun replaceContent(
        data: ByteArray, offset: Int,
        original: String, replacement: String,
        isUtf8: Boolean
    ) {
        check(original.length == replacement.length) {
            "Replacement '${replacement}' must be same length as '${original}' (${original.length})"
        }
        var pos = offset
        if (isUtf8) {
            // skip utf16 char count header
            if (data[pos].toInt() and 0x80 != 0) pos += 2 else pos++
            // skip utf8 byte count header
            if (data[pos].toInt() and 0x80 != 0) pos += 2 else pos++
            val bytes = replacement.toByteArray(Charsets.UTF_8)
            System.arraycopy(bytes, 0, data, pos, bytes.size)
        } else {
            // skip utf16 char count (2 or 4 bytes)
            val bb = ByteBuffer.wrap(data, pos, 4).order(ByteOrder.LITTLE_ENDIAN)
            val first = bb.short.toInt() and 0xFFFF
            pos += if (first and 0x8000 != 0) 4 else 2
            val bytes = replacement.toByteArray(Charsets.UTF_16LE)
            System.arraycopy(bytes, 0, data, pos, bytes.size)
        }
    }
}
