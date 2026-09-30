package org.ardour.android.engine

object ArdourNative {
    init {
        System.loadLibrary("ardour_android")
    }

    @JvmStatic
    external fun bootstrapState(): String

    @JvmStatic
    external fun engineApiVersion(): Int
}
