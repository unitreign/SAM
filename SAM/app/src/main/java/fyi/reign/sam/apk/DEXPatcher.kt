package fyi.reign.sam.apk

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.Adler32

object DEXPatcher {

    private val CONTEXT = "fyi/reign/sam/shortcut/".toByteArray(Charsets.UTF_8)
    private val OLD_SUFFIX = "PLACEHOLDER".toByteArray(Charsets.UTF_8)

    fun patch(dex: ByteArray, newSuffix: String): ByteArray {
        require(newSuffix.length == OLD_SUFFIX.size) {
            "newSuffix must be exactly ${OLD_SUFFIX.size} chars, got ${newSuffix.length}"
        }
        val buf = dex.copyOf()
        val newSuffixBytes = newSuffix.toByteArray(Charsets.UTF_8)

        var i = 0
        while (i <= buf.size - CONTEXT.size - OLD_SUFFIX.size) {
            if (matchAt(buf, i, CONTEXT) && matchAt(buf, i + CONTEXT.size, OLD_SUFFIX)) {
                System.arraycopy(newSuffixBytes, 0, buf, i + CONTEXT.size, newSuffixBytes.size)
                i += CONTEXT.size + newSuffixBytes.size
            } else {
                i++
            }
        }

        recomputeChecksums(buf)
        return buf
    }

    private fun matchAt(data: ByteArray, offset: Int, pattern: ByteArray): Boolean {
        if (offset + pattern.size > data.size) return false
        for (k in pattern.indices) {
            if (data[offset + k] != pattern[k]) return false
        }
        return true
    }

    private fun recomputeChecksums(dex: ByteArray) {
        // SHA-1 of bytes [32..end], stored at [12..31]
        val sha1 = MessageDigest.getInstance("SHA-1").digest(dex.copyOfRange(32, dex.size))
        System.arraycopy(sha1, 0, dex, 12, 20)

        // Adler-32 of bytes [12..end], stored at [8..11] little-endian
        val adler = Adler32()
        adler.update(dex, 12, dex.size - 12)
        ByteBuffer.wrap(dex, 8, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(adler.value.toInt())
    }
}
