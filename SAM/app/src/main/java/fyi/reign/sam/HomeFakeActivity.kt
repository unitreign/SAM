package fyi.reign.sam

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class HomeFakeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "SAM Launcher active.\n\nOpen any app and create a shortcut — SAM will capture it and build an installable APK."
        tv.setPadding(64, 64, 64, 64)
        tv.textSize = 16f
        setContentView(tv)
    }
}
