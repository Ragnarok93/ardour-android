package org.ardour.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.ardour.android.engine.ArdourNative
import org.ardour.android.ui.ArdourApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val nativeStatus = runCatching {
            "API ${ArdourNative.engineApiVersion()} • ${ArdourNative.bootstrapState()}"
        }.getOrElse { error ->
            "Native bridge unavailable • ${error.javaClass.simpleName}"
        }

        setContent {
            ArdourApp(nativeStatus = nativeStatus)
        }
    }
}
