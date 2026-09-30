package org.ardour.android.engine

object ArdourNative {
    init {
        System.loadLibrary("ardour_android")
    }

    @JvmStatic
    external fun bootstrapState(): String

    @JvmStatic
    external fun engineApiVersion(): Int

    @JvmStatic
    external fun audioProbeState(): String

    @JvmStatic
    external fun startAudioProbe(): Boolean

    @JvmStatic
    external fun stopAudioProbe()
}
