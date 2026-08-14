package fyi.reign.sam

import android.app.Application
import fyi.reign.sam.keystore.KeystoreManager

class SAMApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KeystoreManager.ensureKey()
    }
}
