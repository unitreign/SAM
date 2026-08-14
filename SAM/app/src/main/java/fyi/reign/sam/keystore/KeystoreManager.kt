package fyi.reign.sam.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Calendar
import javax.security.auth.x500.X500Principal

object KeystoreManager {
    private const val ALIAS = "sam_signing_key"
    private const val PROVIDER = "AndroidKeyStore"

    fun ensureKey() {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (ks.containsAlias(ALIAS)) return

        val notBefore = Calendar.getInstance()
        val notAfter = Calendar.getInstance().also { it.add(Calendar.YEAR, 25) }

        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSubject(X500Principal("CN=SAM, O=SAM"))
            .setCertificateSerialNumber(BigInteger.ONE)
            .setCertificateNotBefore(notBefore.time)
            .setCertificateNotAfter(notAfter.time)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, PROVIDER)
            .apply { initialize(spec) }
            .generateKeyPair()
    }

    fun getPrivateKey(): PrivateKey {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return ks.getKey(ALIAS, null) as PrivateKey
    }

    fun getCertificate(): X509Certificate {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return ks.getCertificate(ALIAS) as X509Certificate
    }
}
